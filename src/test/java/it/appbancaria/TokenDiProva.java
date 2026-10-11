package it.appbancaria;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Token dei clienti demo, per i test che devono autenticarsi.
 *
 * <p>Il token si ottiene da un login vero, con le stesse credenziali che userebbe un
 * client: i test che hanno bisogno di un'autenticazione non ripetono il corpo della
 * richiesta ne il punto in cui il token si trova nella risposta.
 *
 * <p>Le credenziali sono quelle del seed {@code V2__seed_demo.sql}, dati dimostrativi.
 */
public final class TokenDiProva {

    public static final String EMAIL_MARIO = "mario.rossi@demo.it";
    public static final String EMAIL_GIULIA = "giulia.bianchi@demo.it";
    public static final String PASSWORD_DEMO = "Demo1234!";

    private TokenDiProva() {
    }

    /** Token di Mario Rossi. */
    public static String diMario(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        return token(mockMvc, objectMapper, EMAIL_MARIO, PASSWORD_DEMO);
    }

    /** Token di Giulia Bianchi. */
    public static String diGiulia(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        return token(mockMvc, objectMapper, EMAIL_GIULIA, PASSWORD_DEMO);
    }

    /** Esegue un login vero e restituisce il token. */
    public static String token(MockMvc mockMvc, ObjectMapper objectMapper,
                               String email, String password) throws Exception {
        String corpo = objectMapper.writeValueAsString(Map.of("email", email, "password", password));

        String risposta = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return objectMapper.readTree(risposta).get("token").asText();
    }
}
