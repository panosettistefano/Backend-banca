package it.appbancaria.service;

/**
 * Trasferimento inesistente oppure di un altro cliente.
 *
 * <p>Un solo tipo di eccezione per entrambi i casi, con lo stesso messaggio: la risposta
 * non deve far capire se un trasferimento esiste ma non e nostro.
 */
public class TrasferimentoNonTrovatoException extends RuntimeException {

    public TrasferimentoNonTrovatoException() {
        super("Trasferimento non trovato");
    }
}
