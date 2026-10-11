package it.appbancaria.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.appbancaria.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * La documentazione OpenAPI che alimenta Swagger UI.
 *
 * <p>Non verifica l'aspetto della pagina, ma il documento che la genera: che sia servito
 * senza token, che descriva tutti gli endpoint, che dichiari lo schema di sicurezza del
 * token e che ogni endpoint documenti anche le risposte di errore. Se una risposta di
 * errore venisse tolta dalle annotazioni, il test lo segnala.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApiDocumentazioneTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Il documento OpenAPI e servito senza token e descrive tutti gli endpoint")
    void documentoOpenApi() throws Exception {
        JsonNode documento = leggiDocumento();

        assertThat(documento.get("info").get("title").asText()).contains("AppBancaria");
        assertThat(documento.get("info").get("version").asText()).isNotBlank();

        assertThat(nomi(documento.get("paths"))).contains(
                "/api/auth/login",
                "/api/accounts",
                "/api/accounts/{id}/movements",
                "/api/transfers",
                "/api/transfers/{id}/confirm");
    }

    @Test
    @DisplayName("Il token e dichiarato come schema di sicurezza e richiesto da tutte le chiamate")
    void schemaDiSicurezza() throws Exception {
        JsonNode documento = leggiDocumento();

        JsonNode schema = documento.get("components").get("securitySchemes").get("bearerAuth");
        assertThat(schema).isNotNull();
        assertThat(schema.get("type").asText()).isEqualTo("http");
        assertThat(schema.get("scheme").asText()).isEqualTo("bearer");
        assertThat(schema.get("bearerFormat").asText()).isEqualTo("JWT");

        // Richiesto a livello di documento, quindi da ogni endpoint...
        assertThat(documento.get("security")).isNotEmpty();

        // ...tranne il login, che lo esclude perche e la porta da cui si entra.
        JsonNode login = documento.get("paths").get("/api/auth/login").get("post");
        assertThat(login.get("security")).isEmpty();
    }

    @Test
    @DisplayName("Ogni endpoint documenta le risposte di successo e quelle di errore")
    void risposteDocumentate() throws Exception {
        JsonNode percorsi = leggiDocumento().get("paths");

        assertThat(codiciDiRisposta(percorsi.get("/api/auth/login").get("post")))
                .contains("200", "400", "401");
        assertThat(codiciDiRisposta(percorsi.get("/api/accounts").get("get")))
                .contains("200", "401");
        assertThat(codiciDiRisposta(percorsi.get("/api/accounts/{id}/movements").get("get")))
                .contains("200", "400", "401", "404");
        assertThat(codiciDiRisposta(percorsi.get("/api/transfers").get("post")))
                .contains("201", "400", "401", "404", "409");
        assertThat(codiciDiRisposta(percorsi.get("/api/transfers").get("get")))
                .contains("200", "400", "401");
        assertThat(codiciDiRisposta(percorsi.get("/api/transfers/{id}/confirm").get("post")))
                .contains("200", "401", "404", "409");
    }

    @Test
    @DisplayName("Swagger UI e raggiungibile senza token")
    void swaggerUiAperta() throws Exception {
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    // ------------------------------------------------------------------
    // Aiuti
    // ------------------------------------------------------------------

    private JsonNode leggiDocumento() throws Exception {
        String corpo = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo);
    }

    private List<String> nomi(JsonNode oggetto) {
        List<String> nomi = new ArrayList<>();
        oggetto.fieldNames().forEachRemaining(nomi::add);
        return nomi;
    }

    private List<String> codiciDiRisposta(JsonNode operazione) {
        return nomi(operazione.get("responses"));
    }
}
