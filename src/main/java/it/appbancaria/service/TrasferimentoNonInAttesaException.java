package it.appbancaria.service;

/**
 * Il trasferimento e gia stato confermato o rifiutato: non e piu confermabile.
 *
 * <p>E' la protezione lato server contro il doppio invio: la seconda conferma non muove
 * nulla, perche lo stato del trasferimento non e piu IN_ATTESA. La macchina a stati non
 * torna indietro: non esistono annullamento ne nuovo tentativo.
 */
public class TrasferimentoNonInAttesaException extends RuntimeException {

    public TrasferimentoNonInAttesaException() {
        super("Il trasferimento non e piu in attesa");
    }
}
