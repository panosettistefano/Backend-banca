package it.appbancaria.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.appbancaria.TestcontainersConfiguration;
import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Login e protezione delle rotte, con il contesto completo e il MySQL di Testcontainers.
 *
 * <p>Le credenziali usate sono quelle del seed demo: il login corretto verifica anche che
 * l'hash BCrypt scritto in {@code V2__seed_demo.sql} corrisponda davvero alla password
 * dichiarata, cosa che nessun test dei repository poteva controllare.
 *
 * <p>Le richieste passano dalla catena di sicurezza vera (MockMvc la applica): quello che
 * si verifica qui e il comportamento HTTP, non le classi interne.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AutenticazioneControllerTest {

    private static final String EMAIL_DEMO = "mario.rossi@demo.it";
    private static final String PASSWORD_DEMO = "Demo1234!";
    private static final String MESSAGGIO_CREDENZIALI = "Credenziali non valide";
    private static final String MESSAGGIO_TOKEN = "Token mancante o non valido";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Il login demo restituisce il token e i dati del cliente, mai la password")
    void loginConCredenzialiCorrette() throws Exception {
        String risposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin(EMAIL_DEMO, PASSWORD_DEMO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.scadeIl").isNotEmpty())
                .andExpect(jsonPath("$.utente.id").isNumber())
                .andExpect(jsonPath("$.utente.nome").value("Mario"))
                .andExpect(jsonPath("$.utente.cognome").value("Rossi"))
                .andExpect(jsonPath("$.utente.email").value(EMAIL_DEMO))
                .andExpect(jsonPath("$.utente.ruolo").value("CLIENTE"))
                .andReturn().getResponse().getContentAsString();

        // Ne la password ne il suo hash devono uscire dal server.
        assertThat(risposta)
                .doesNotContain(PASSWORD_DEMO)
                .doesNotContain("$2a$")
                .doesNotContain("passwordHash");
    }

    @Test
    @DisplayName("La password errata produce 401 con il messaggio generico")
    void passwordErrata() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin(EMAIL_DEMO, "PasswordSbagliata1!")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errore").value(MESSAGGIO_CREDENZIALI))
                .andExpect(jsonPath("$.token").doesNotExist());
    }

    @Test
    @DisplayName("Un'email sconosciuta produce lo stesso 401 della password errata")
    void emailSconosciuta() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("nessuno@demo.it", PASSWORD_DEMO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errore").value(MESSAGGIO_CREDENZIALI));
    }

    @Test
    @DisplayName("Un corpo senza email o con email malformata produce 400")
    void corpoNonValido() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("", "")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin("non-una-email", PASSWORD_DEMO)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Una rotta protetta senza token risponde 401 JSON, non 403 ne HTML")
    void rottaProtettaSenzaToken() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errore").value(MESSAGGIO_TOKEN));
    }

    @Test
    @DisplayName("Con un token valido la sicurezza lascia passare (404: la rotta arriva allo STEP 4)")
    void conTokenValidoLaSicurezzaLasciaPassare() throws Exception {
        mockMvc.perform(get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenValido()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un token manomesso viene rifiutato con 401")
    void tokenManomesso() throws Exception {
        String token = tokenValido();
        // Cambia l'ultimo carattere: la firma non corrisponde piu al contenuto.
        String manomesso = token.substring(0, token.length() - 1)
                + (token.endsWith("A") ? "B" : "A");

        mockMvc.perform(get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + manomesso))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errore").value(MESSAGGIO_TOKEN));
    }

    @Test
    @DisplayName("Un token illeggibile viene rifiutato con 401")
    void tokenIlleggibile() throws Exception {
        mockMvc.perform(get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer non-un-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Il health check resta aperto senza token")
    void healthCheckAperto() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("La dispatch di errore del contenitore verso /error non viene bloccata")
    void dispatchDiErroreNonBloccata() throws Exception {
        // Quando una risposta esce in errore (400, 404) il contenitore riesegue la catena
        // su /error: se la sicurezza la fermasse, il 401 coprirebbe lo status vero. La
        // richiesta si costruisce con il tipo di dispatch che usa Tomcat, cosa che MockMvc
        // di suo non fa: senza questo test la regressione si vedrebbe solo a mano.
        mockMvc.perform(get("/error").with(richiesta -> {
                    richiesta.setDispatcherType(DispatcherType.ERROR);
                    return richiesta;
                }))
                .andExpect(status().is5xxServerError());
    }

    /** Corpo JSON del login, scritto come lo manderebbe un client esterno. */
    private String corpoLogin(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(Map.of("email", email, "password", password));
    }

    /** Esegue un login vero con le credenziali demo e restituisce il token. */
    private String tokenValido() throws Exception {
        String risposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoLogin(EMAIL_DEMO, PASSWORD_DEMO)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(risposta).get("token").asText();
    }
}
