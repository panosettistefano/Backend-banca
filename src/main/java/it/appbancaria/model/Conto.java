package it.appbancaria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Conto corrente di un cliente.
 *
 * <p>Il saldo e <strong>materializzato</strong>: non si calcola sommando i movimenti a ogni
 * lettura, ma viene aggiornato solo dentro le transazioni dei trasferimenti. L'invariante
 * {@code saldo = somma delle entrate - somma delle uscite} e garantita da quelle transazioni
 * ed e verificata dai test sui dati demo.
 *
 * <p>Gli importi usano {@link BigDecimal} (colonna {@code DECIMAL(15,2)}): mai {@code double},
 * che non rappresenta esattamente i centesimi.
 *
 * <p>Mappa la tabella {@code conto} di {@code V1__schema.sql}.
 */
@Entity
@Table(name = "conto")
public class Conto {

    /** Chiave primaria generata dal database ({@code AUTO_INCREMENT}). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Codice del conto in formato tipo IBAN, univoco (vincolo {@code uk_conto_codice_conto}). */
    @Column(name = "codice_conto", nullable = false, unique = true, length = 34)
    private String codiceConto;

    /**
     * Cliente titolare del conto.
     *
     * <p>{@link FetchType#LAZY}: il titolare si legge dal database solo se serve davvero.
     * Va quindi letto dentro una transazione aperta ({@code open-in-view: false}).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "titolare_id", nullable = false)
    private Utente titolare;

    /** Valuta del conto: tutti gli importi del conto sono espressi in questa valuta. */
    @Enumerated(EnumType.STRING)
    @Column(name = "valuta", nullable = false, length = 3)
    private Valuta valuta;

    /** Saldo attuale. Il vincolo {@code chk_conto_saldo} impedisce che scenda sotto zero. */
    @Column(name = "saldo", nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    /** Istante di apertura del conto, valorizzato da Hibernate al primo salvataggio. */
    @CreationTimestamp
    @Column(name = "creato_il", nullable = false, updatable = false)
    private LocalDateTime creatoIl;

    /** Costruttore richiesto da JPA: non va usato direttamente nel codice applicativo. */
    protected Conto() {
    }

    /** Apre un conto per il titolare indicato, con saldo iniziale zero. */
    public Conto(String codiceConto, Utente titolare, Valuta valuta) {
        this.codiceConto = codiceConto;
        this.titolare = titolare;
        this.valuta = valuta;
    }

    public Long getId() {
        return id;
    }

    public String getCodiceConto() {
        return codiceConto;
    }

    public void setCodiceConto(String codiceConto) {
        this.codiceConto = codiceConto;
    }

    public Utente getTitolare() {
        return titolare;
    }

    public void setTitolare(Utente titolare) {
        this.titolare = titolare;
    }

    public Valuta getValuta() {
        return valuta;
    }

    public void setValuta(Valuta valuta) {
        this.valuta = valuta;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public LocalDateTime getCreatoIl() {
        return creatoIl;
    }
}
