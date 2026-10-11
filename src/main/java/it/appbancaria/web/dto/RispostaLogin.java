package it.appbancaria.web.dto;

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
public record RispostaLogin(String token, String tipo, Instant scadeIl, UtenteDto utente) {
}
