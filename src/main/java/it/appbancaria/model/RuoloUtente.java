package it.appbancaria.model;

/**
 * Ruolo di un utente dell'applicazione.
 *
 * <p>Oggi esiste un solo ruolo utilizzabile ({@link #CLIENTE}): {@link #ADMIN} e previsto
 * dal modello dati per estensioni future, ma nessun endpoint lo richiede.
 * I valori corrispondono al vincolo {@code chk_utente_ruolo} dello schema.
 */
public enum RuoloUtente {

    CLIENTE,
    ADMIN
}
