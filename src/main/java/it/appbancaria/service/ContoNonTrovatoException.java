package it.appbancaria.service;

/**
 * Conto inesistente oppure di un altro cliente.
 *
 * <p>Un solo tipo di eccezione per entrambi i casi, con lo stesso messaggio: la risposta
 * non deve far capire se un conto esiste ma non e nostro (nessuna enumerazione dei conti).
 */
public class ContoNonTrovatoException extends RuntimeException {

    public ContoNonTrovatoException() {
        super("Conto non trovato");
    }
}
