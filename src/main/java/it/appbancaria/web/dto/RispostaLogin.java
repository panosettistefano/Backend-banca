package it.appbancaria.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

/**
 * Risposta del login: il token da usare nelle richieste successive e i dati del cliente.
 *
 * <p>Ne password ne hash compaiono: la risposta contiene solo cio che serve all'interfaccia
 * per mostrare nome e ruolo.
 *
 * @param token   token JWT compatto
 * @param tipo    schema di autorizzazione per lo header: {@code Bearer}
 * @param scadeIl istante di scadenza del token (ISO-8601, in UTC)
 * @param utente  dati essenziali del cliente autenticato
 */
@Schema(description = "Esito del login: token e dati del cliente")
public record RispostaLogin(
        @Schema(description = "Token JWT, da inviare come Authorization: Bearer <token>",
                example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJtYXJpby5yb3NzaUBkZW1vLml0In0.firma")
        String token,

        @Schema(description = "Schema di autorizzazione da usare nello header", example = "Bearer")
        String tipo,

        @Schema(description = "Istante di scadenza del token", example = "2026-10-11T14:30:00Z")
        Instant scadeIl,

        @Schema(description = "Dati essenziali del cliente autenticato")
        UtenteDto utente) {
}
