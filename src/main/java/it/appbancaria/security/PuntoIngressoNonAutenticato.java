package it.appbancaria.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.appbancaria.web.dto.ErroreRisposta;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Risposta alle richieste non autenticate: {@code 401} con corpo JSON.
 *
 * <p>Senza un entry point esplicito Spring Security risponderebbe {@code 403} (il suo
 * predefinito e {@code Http403ForbiddenEntryPoint}): il 403 direbbe "so chi sei ma non
 * puoi", che e un'altra cosa dal "non so chi sei". Con questo componente ogni rotta
 * protetta raggiunta senza token valido riceve 401 e un corpo JSON, non una pagina HTML.
 *
 * <p>Il corpo ha la stessa forma degli errori del controller ({@link ErroreRisposta}):
 * la gestione uniforme di tutti gli errori arriva allo STEP 6.
 */
@Component
public class PuntoIngressoNonAutenticato implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public PuntoIngressoNonAutenticato(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest richiesta, HttpServletResponse risposta,
                         AuthenticationException eccezione) throws IOException {
        risposta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        risposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        risposta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(risposta.getOutputStream(),
                ErroreRisposta.di(HttpStatus.UNAUTHORIZED, "Token mancante o non valido",
                        richiesta.getRequestURI()));
    }
}
