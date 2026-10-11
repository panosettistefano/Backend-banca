package it.appbancaria.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.appbancaria.TestcontainersConfiguration;
import it.appbancaria.TokenDiProva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La forma uniforme degli errori, caso per caso.
 *
 * <p>Ogni errore dell'API, qualunque sia la causa, esce nella stessa forma: {@code istante},
 * {@code stato}, {@code errore}, {@code percorso} e, per la validazione, {@code dettagli}.
 * Qui si verificano i casi che prima producevano la pagina di errore predefinita di Spring
 * (corpo illeggibile, parametro sbagliato, rotta inesistente, metodo non ammesso) insieme
 * al caso che non passa dai controller (token mancante, risposto dal filtro di sicurezza).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class GestoreErroriTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void autentica() throws Exception {
        token = TokenDiProva.diMario(mockMvc, objectMapper);
    }

    @Test
    @DisplayName("I campi non validi producono 400 con l'elenco dei dettagli")
    void validazioneDelCorpo() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.stato").value(400))
                .andExpect(jsonPath("$.errore").value("Dati non validi"))
                .andExpect(jsonPath("$.percorso").value("/api/transfers"))
                .andExpect(jsonPath("$.istante").isNotEmpty())
                .andExpect(jsonPath("$.dettagli").isNotEmpty())
                .andExpect(jsonPath("$.dettagli[0]").isNotEmpty());
    }

    @Test
    @DisplayName("Un corpo illeggibile produce 400 con un messaggio comprensibile")
    void corpoIlleggibile() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{non-e-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errore").value("Corpo della richiesta non leggibile"))
                .andExpect(jsonPath("$.stato").value(400));
    }

    @Test
    @DisplayName("Una data non ISO produce 400 e il messaggio dice quale parametro")
    void parametroNonValido() throws Exception {
        mockMvc.perform(get("/api/accounts/3/movements?da=non-una-data")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errore").value("Parametro non valido: da"))
                .andExpect(jsonPath("$.percorso").value("/api/accounts/3/movements"));
    }

    @Test
    @DisplayName("Uno stato di trasferimento inesistente produce 400")
    void statoNonValido() throws Exception {
        mockMvc.perform(get("/api/transfers?stato=IN_ESISTENTE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errore").value("Parametro non valido: stato"));
    }

    @Test
    @DisplayName("Una rotta che non esiste risponde 404 con il corpo uniforme")
    void rottaInesistente() throws Exception {
        mockMvc.perform(get("/api/inesistente")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.stato").value(404))
                .andExpect(jsonPath("$.errore").value("Risorsa non trovata"))
                .andExpect(jsonPath("$.percorso").value("/api/inesistente"));
    }

    @Test
    @DisplayName("Un metodo non previsto sulla rotta produce 405")
    void metodoNonAmmesso() throws Exception {
        mockMvc.perform(get("/api/transfers/1/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.stato").value(405))
                .andExpect(jsonPath("$.errore").value("Metodo non ammesso"));
    }

    @Test
    @DisplayName("Il 401 del filtro di sicurezza ha la stessa forma degli altri errori")
    void tokenMancante() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.stato").value(401))
                .andExpect(jsonPath("$.errore").value("Token mancante o non valido"))
                .andExpect(jsonPath("$.percorso").value("/api/accounts"))
                .andExpect(jsonPath("$.istante").isNotEmpty())
                .andExpect(jsonPath("$.dettagli").isEmpty());
    }

    @Test
    @DisplayName("Gli errori di dominio mantengono la forma uniforme")
    void erroreDiDominio() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"contoOrigineId\":3,\"contoDestinazioneId\":3,\"importo\":10.00}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.stato").value(400))
                .andExpect(jsonPath("$.errore").value("I due conti devono essere diversi"))
                .andExpect(jsonPath("$.percorso").value("/api/transfers"))
                .andExpect(jsonPath("$.dettagli").isEmpty());
    }
}
