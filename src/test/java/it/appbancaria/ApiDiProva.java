package it.appbancaria;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Chiamate GET autenticate, per i test che leggono lo stato dall'API.
 *
 * <p>Evita di ripetere in ogni test la costruzione della richiesta con lo header
 * {@code Authorization} e la lettura del corpo JSON.
 */
public final class ApiDiProva {

    private ApiDiProva() {
    }

    /** GET autenticato che deve rispondere 200; restituisce il JSON letto. */
    public static JsonNode json(MockMvc mockMvc, ObjectMapper objectMapper,
                                String token, String percorso) throws Exception {
        String corpo = mockMvc.perform(get(percorso)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(corpo);
    }

    /** Come sopra, per le risposte che sono liste JSON. */
    public static List<JsonNode> lista(MockMvc mockMvc, ObjectMapper objectMapper,
                                       String token, String percorso) throws Exception {
        List<JsonNode> righe = new ArrayList<>();
        json(mockMvc, objectMapper, token, percorso).forEach(righe::add);
        return righe;
    }
}
