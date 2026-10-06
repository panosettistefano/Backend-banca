package it.appbancaria.model;

/**
 * Stato di un trasferimento: macchina a stati {@code IN_ATTESA -> COMPLETATO | RIFIUTATO}.
 *
 * <p>La creazione di un trasferimento non muove denaro e non prenota fondi: registra solo
 * una richiesta in stato {@link #IN_ATTESA}. E' la conferma a eseguire l'operazione,
 * in una sola transazione insieme ai due movimenti.
 * I valori corrispondono al vincolo {@code chk_trasferimento_stato} dello schema.
 */
public enum StatoTrasferimento {

    /** Richiesto ma non ancora eseguito: nessun saldo e stato toccato. */
    IN_ATTESA,

    /** Eseguito: ha aggiornato i saldi, ha una data di conferma e i due movimenti. */
    COMPLETATO,

    /** Non eseguito: nessun saldo toccato, con un motivo di rifiuto valorizzato. */
    RIFIUTATO
}
