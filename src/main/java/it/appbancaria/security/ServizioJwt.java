package it.appbancaria.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * Firma e verifica dei token JWT (HS256, libreria jjwt 0.12.7).
 *
 * <p>Il token e autosufficiente: contiene l'email ({@code sub}) e il ruolo, e la firma
 * garantisce che nessuno possa modificarli senza conoscere la chiave. Il server non tiene
 * un elenco dei token emessi: e la firma a renderli autentici e la scadenza a renderli
 * temporanei. E il motivo per cui l'API puo restare senza sessioni.
 *
 * <p>Attenzione all'API della libreria: dalla 0.12 jjwt si usa
 * {@code Jwts.parser().verifyWith(chiave)} con {@code parseSignedClaims}; i metodi della
 * 0.11 ({@code parserBuilder}, {@code setSigningKey}, {@code parseClaimsJws}) non esistono piu.
 */
@Service
public class ServizioJwt {

    private final ConfigurazioneJwt configurazione;
    private final SecretKey chiave;

    public ServizioJwt(ConfigurazioneJwt configurazione) {
        this.configurazione = configurazione;
        // La chiave si costruisce una volta sola, all'avvio: se e troppo corta per HS256
        // l'applicazione non parte (WeakKeyException) invece di firmare token deboli.
        this.chiave = Keys.hmacShaKeyFor(configurazione.chiave().getBytes(StandardCharsets.UTF_8));
    }

    /** Emette un token per il cliente indicato, con la scadenza calcolata da adesso. */
    public TokenGenerato generaToken(UtenteAutenticato utente) {
        Instant adesso = Instant.now();
        Instant scadenza = adesso.plus(configurazione.scadenza());

        String token = Jwts.builder()
                .subject(utente.email())
                .claim("ruolo", utente.ruolo().name())
                .issuedAt(Date.from(adesso))
                .expiration(Date.from(scadenza))
                .signWith(chiave)
                .compact();

        return new TokenGenerato(token, scadenza);
    }

    /**
     * Verifica firma e scadenza e restituisce l'email contenuta nel token.
     *
     * @throws io.jsonwebtoken.JwtException token malformato, firmato con un'altra chiave
     *         o scaduto ({@code ExpiredJwtException}, {@code SignatureException}, ...)
     */
    public String emailDaToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(chiave)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }
}
