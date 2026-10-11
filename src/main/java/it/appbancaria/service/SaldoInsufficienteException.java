package it.appbancaria.service;

/**
 * Il saldo del conto di origine non copre l'importo del trasferimento.
 *
 * <p>Succede in due momenti diversi: alla richiesta, se il saldo non basta gia allora; alla
 * conferma, se nel frattempo il saldo e sceso (nessun fondo viene prenotato alla richiesta).
 * Nel secondo caso il trasferimento viene registrato come RIFIUTATO e la conferma risponde
 * 409: la transazione che rifiuta e, essa stessa, completata per intero.
 */
public class SaldoInsufficienteException extends RuntimeException {

    public SaldoInsufficienteException() {
        super("Saldo insufficiente");
    }
}
