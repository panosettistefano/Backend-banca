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
 * Trasferimento simulato tra due conti distinti.
 *
 * <p>Nasce {@link StatoTrasferimento#IN_ATTESA} e non tocca alcun saldo: e solo una richiesta
 * registrata. La conferma esegue l'operazione in una sola transazione: aggiorna i due saldi
 * e inserisce i due movimenti collegati. Se qualcosa fallisce, non resta nulla a meta.
 *
 * <p>Mappa la tabella {@code trasferimento} di {@code V1__schema.sql}, compresi i vincoli
 * che legano stato, data di conferma e motivo di rifiuto.
 */
@Entity
@Table(name = "trasferimento")
public class Trasferimento {

    /** Chiave primaria generata dal database ({@code AUTO_INCREMENT}). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Codice pubblico del trasferimento (UUID), univoco: e l'identificativo da mostrare
     * all'utente e da usare nelle chiamate di conferma, al posto dell'id interno.
     */
    @Column(name = "riferimento", nullable = false, unique = true, length = 36)
    private String riferimento;

    /** Cliente che ha richiesto il trasferimento. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utente_id", nullable = false)
    private Utente utente;

    /** Conto da addebitare: deve appartenere a {@link #utente}. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conto_origine_id", nullable = false)
    private Conto contoOrigine;

    /** Conto da accreditare: il vincolo {@code chk_trasferimento_conti} lo vuole diverso dall'origine. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conto_destinazione_id", nullable = false)
    private Conto contoDestinazione;

    /** Valuta dell'importo, quella dei due conti. */
    @Enumerated(EnumType.STRING)
    @Column(name = "valuta", nullable = false, length = 3)
    private Valuta valuta;

    /** Importo da trasferire, sempre positivo ({@code chk_trasferimento_importo}). */
    @Column(name = "importo", nullable = false, precision = 15, scale = 2)
    private BigDecimal importo;

    /** Causale facoltativa scritta dal cliente. */
    @Column(name = "causale", length = 140)
    private String causale;

    /** Stato corrente nella macchina a stati. */
    @Enumerated(EnumType.STRING)
    @Column(name = "stato", nullable = false, length = 16)
    private StatoTrasferimento stato = StatoTrasferimento.IN_ATTESA;

    /** Motivo del rifiuto: valorizzato solo per i trasferimenti {@link StatoTrasferimento#RIFIUTATO}. */
    @Column(name = "motivo_rifiuto", length = 255)
    private String motivoRifiuto;

    /** Istante della richiesta, valorizzato da Hibernate al primo salvataggio. */
    @CreationTimestamp
    @Column(name = "creato_il", nullable = false, updatable = false)
    private LocalDateTime creatoIl;

    /** Istante di esecuzione: il vincolo {@code chk_trasferimento_conferma} lo vuole presente solo se COMPLETATO. */
    @Column(name = "confermato_il")
    private LocalDateTime confermatoIl;

    /** Costruttore richiesto da JPA: non va usato direttamente nel codice applicativo. */
    protected Trasferimento() {
    }

    /** Registra una richiesta di trasferimento in stato IN_ATTESA, senza muovere alcun saldo. */
    public Trasferimento(String riferimento, Utente utente, Conto contoOrigine, Conto contoDestinazione,
                         BigDecimal importo, String causale) {
        this.riferimento = riferimento;
        this.utente = utente;
        this.contoOrigine = contoOrigine;
        this.contoDestinazione = contoDestinazione;
        this.valuta = contoOrigine.getValuta();
        this.importo = importo;
        this.causale = causale;
    }

    public Long getId() {
        return id;
    }

    public String getRiferimento() {
        return riferimento;
    }

    public Utente getUtente() {
        return utente;
    }

    public Conto getContoOrigine() {
        return contoOrigine;
    }

    public Conto getContoDestinazione() {
        return contoDestinazione;
    }

    public Valuta getValuta() {
        return valuta;
    }

    public BigDecimal getImporto() {
        return importo;
    }

    public void setImporto(BigDecimal importo) {
        this.importo = importo;
    }

    public String getCausale() {
        return causale;
    }

    public void setCausale(String causale) {
        this.causale = causale;
    }

    public StatoTrasferimento getStato() {
        return stato;
    }

    public void setStato(StatoTrasferimento stato) {
        this.stato = stato;
    }

    public String getMotivoRifiuto() {
        return motivoRifiuto;
    }

    public void setMotivoRifiuto(String motivoRifiuto) {
        this.motivoRifiuto = motivoRifiuto;
    }

    public LocalDateTime getCreatoIl() {
        return creatoIl;
    }

    public LocalDateTime getConfermatoIl() {
        return confermatoIl;
    }

    public void setConfermatoIl(LocalDateTime confermatoIl) {
        this.confermatoIl = confermatoIl;
    }
}
