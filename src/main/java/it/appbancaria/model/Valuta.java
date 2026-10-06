package it.appbancaria.model;

/**
 * Codice valuta ISO 4217 di un conto, come ammesso dal vincolo {@code chk_conto_valuta}.
 *
 * <p>Il nome della costante e gia il codice a tre lettere usato nel database e nel JSON:
 * non serve nessuna conversione.
 */
public enum Valuta {

    EUR,
    USD,
    GBP
}
