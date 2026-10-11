package it.appbancaria.service;

/**
 * Richiesta di trasferimento formalmente corretta ma non ammessa dalle regole di dominio:
 * i due conti coincidono oppure non hanno la stessa valuta.
 *
 * <p>E' cosa diversa da un errore di formato (importo assente, negativo, con troppi
 * decimali): quello lo intercetta la validazione del DTO prima ancora di entrare nel
 * servizio.
 */
public class RichiestaNonValidaException extends RuntimeException {

    public RichiestaNonValidaException(String messaggio) {
        super(messaggio);
    }
}
