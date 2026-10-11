package it.appbancaria.service;

/**
 * Email sconosciuta oppure password errata.
 *
 * <p>Un solo tipo di eccezione per entrambi i casi, con lo stesso messaggio: la risposta
 * non deve far capire se l'email esiste o meno (nessuna enumerazione degli utenti).
 */
public class CredenzialiNonValideException extends RuntimeException {

    public CredenzialiNonValideException() {
        super("Credenziali non valide");
    }
}
