package it.appbancaria.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Legge il token dallo header {@code Authorization} e, se valido, autentica la richiesta.
 *
 * <p>Gira una volta sola per richiesta ({@link OncePerRequestFilter}). Se lo header manca,
 * la richiesta prosegue anonima: sara la catena di sicurezza a decidere se la rotta
 * richiede l'autenticazione. Se il token c'e ma non e valido (scaduto, manomesso, firmato
 * con un'altra chiave) il contesto resta vuoto e la richiesta prosegue come anonima: il 401
 * lo produce il {@link PuntoIngressoNonAutenticato}, cosi il corpo dell'errore e uno solo.
 */
@Component
public class FiltroAutenticazioneJwt extends OncePerRequestFilter {

    /** Prefisso dello header, schema di autorizzazione {@code Bearer}. */
    private static final String PREFISSO_BEARER = "Bearer ";

    private final ServizioJwt servizioJwt;
    private final ServizioUtenti servizioUtenti;

    public FiltroAutenticazioneJwt(ServizioJwt servizioJwt, ServizioUtenti servizioUtenti) {
        this.servizioJwt = servizioJwt;
        this.servizioUtenti = servizioUtenti;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest richiesta, HttpServletResponse risposta,
                                    FilterChain catena) throws ServletException, IOException {
        String header = richiesta.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(PREFISSO_BEARER)) {
            String token = header.substring(PREFISSO_BEARER.length());
            try {
                String email = servizioJwt.emailDaToken(token);
                UserDetails utente = servizioUtenti.loadUserByUsername(email);

                UsernamePasswordAuthenticationToken autenticazione =
                        new UsernamePasswordAuthenticationToken(utente, null, utente.getAuthorities());
                autenticazione.setDetails(new WebAuthenticationDetailsSource().buildDetails(richiesta));
                SecurityContextHolder.getContext().setAuthentication(autenticazione);
            } catch (JwtException | UsernameNotFoundException e) {
                // Token non valido, oppure cliente cancellato dopo l'emissione: si resta anonimi.
                SecurityContextHolder.clearContext();
            }
        }

        catena.doFilter(richiesta, risposta);
    }
}
