package it.appbancaria.model;

/**
 * Tipo di un movimento, come ammesso dal vincolo {@code chk_movimento_tipo}.
 *
 * <p>L'importo di un movimento e sempre positivo: il verso (entrata o uscita) lo
 * stabilisce il tipo. Le due regole qui sotto derivano da quella del database e
 * servono a non ripetere in ogni punto del codice la stessa condizione.
 */
public enum TipoMovimento {

    /** Apertura del conto: e sempre un'entrata. */
    SALDO_INIZIALE,

    /** Denaro versato sul conto: entrata. */
    VERSAMENTO,

    /** Denaro prelevato dal conto: uscita. */
    PRELIEVO,

    /** Uscita generata dalla conferma di un trasferimento. */
    TRASFERIMENTO_USCITA,

    /** Entrata generata dalla conferma di un trasferimento. */
    TRASFERIMENTO_ENTRATA;

    /**
     * Indica se il movimento aumenta il saldo del conto.
     *
     * <p>La stessa regola e scritta in SQL nella riconciliazione dei saldi in
     * {@code V2__seed_demo.sql}: se cambia qui, va cambiata anche la.
     */
    public boolean isEntrata() {
        return this == SALDO_INIZIALE || this == VERSAMENTO || this == TRASFERIMENTO_ENTRATA;
    }

    /**
     * Indica se il movimento nasce dalla conferma di un trasferimento: in quel caso
     * deve avere un riferimento al trasferimento, come impone {@code chk_movimento_riferimento}.
     */
    public boolean isTrasferimento() {
        return this == TRASFERIMENTO_USCITA || this == TRASFERIMENTO_ENTRATA;
    }
}
