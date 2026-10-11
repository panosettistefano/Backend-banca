package it.appbancaria.web.dto;

/**
 * Corpo JSON degli errori: {@code {"errore": "..."}}.
 *
 * <p>Forma minima condivisa da tutti gli errori previsti in questo step: credenziali
 * errate (controller) e token mancante o non valido (filtro di sicurezza). Lo STEP 6 la
 * estende in un formato unico per tutta l'API, con piu dettagli.
 *
 * @param errore messaggio leggibile, in italiano
 */
public record ErroreRisposta(String errore) {
}
