package it.appbancaria.security;

import java.time.Instant;

/**
 * Esito dell'emissione di un token: il testo da consegnare al client e l'istante di scadenza.
 *
 * <p>I due valori nascono insieme, cosi la scadenza comunicata nella risposta di login e
 * esattamente quella scritta dentro il token.
 *
 * @param token   token JWT compatto, da inviare come {@code Authorization: Bearer <token>}
 * @param scadeIl istante in cui il token smette di essere valido
 */
public record TokenGenerato(String token, Instant scadeIl) {
}
