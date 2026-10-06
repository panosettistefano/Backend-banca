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
 * Movimento di conto: una riga per ogni soldo entrato o uscito.
 *
 * <p>L'importo e memorizzato <strong>sempre positivo</strong>: il verso lo determina
 * {@link TipoMovimento#isEntrata()}. La valuta non e ripetuta qui perche e quella del conto.
 *
 * <p>Mappa la tabella {@code movimento} di {@code V1__schema.sql}. Il vincolo
 * {@code uk_movimento_trasferimento_tipo} impedisce che uno stesso trasferimento generi
 * piu di un movimento per tipo: al massimo un'uscita e un'entrata, anche in caso di bug.
 */
@Entity
@Table(name = "movimento")
public class Movimento {

    /** Chiave primaria generata dal database ({@code AUTO_INCREMENT}). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Conto a cui il movimento appartiene. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conto_id", nullable = false)
    private Conto conto;

    /** Data e ora in cui il movimento e stato registrato: e il campo su cui si filtra. */
    @Column(name = "data_movimento", nullable = false)
    private LocalDateTime dataMovimento;

    /** Descrizione mostrata all'utente (es. "Pagamento utenze demo"). */
    @Column(name = "descrizione", nullable = false, length = 255)
    private String descrizione;

    /** Tipo del movimento: da solo determina il verso (entrata o uscita). */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 24)
    private TipoMovimento tipo;

    /** Importo sempre positivo in valuta del conto ({@code DECIMAL(15,2)}). */
    @Column(name = "importo", nullable = false, precision = 15, scale = 2)
    private BigDecimal importo;

    /**
     * Trasferimento che ha generato il movimento, oppure {@code null} per i movimenti
     * di altra origine (saldo iniziale, versamenti, prelievi).
     * Il vincolo {@code chk_movimento_riferimento} tiene coerenti tipo e riferimento.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trasferimento_id")
    private Trasferimento trasferimento;

    /** Istante di inserimento della riga, valorizzato da Hibernate al primo salvataggio. */
    @CreationTimestamp
    @Column(name = "creato_il", nullable = false, updatable = false)
    private LocalDateTime creatoIl;

    /** Costruttore richiesto da JPA: non va usato direttamente nel codice applicativo. */
    protected Movimento() {
    }

    /** Crea un movimento senza trasferimento collegato (saldo iniziale, versamento, prelievo). */
    public Movimento(Conto conto, LocalDateTime dataMovimento, String descrizione,
                     TipoMovimento tipo, BigDecimal importo) {
        this.conto = conto;
        this.dataMovimento = dataMovimento;
        this.descrizione = descrizione;
        this.tipo = tipo;
        this.importo = importo;
    }

    /**
     * Importo con il segno: positivo se il movimento e un'entrata, negativo se e un'uscita.
     * E' la forma da mostrare all'utente e da sommare per ricavare il saldo.
     */
    public BigDecimal getImportoConSegno() {
        return tipo.isEntrata() ? importo : importo.negate();
    }

    public Long getId() {
        return id;
    }

    public Conto getConto() {
        return conto;
    }

    public void setConto(Conto conto) {
        this.conto = conto;
    }

    public LocalDateTime getDataMovimento() {
        return dataMovimento;
    }

    public void setDataMovimento(LocalDateTime dataMovimento) {
        this.dataMovimento = dataMovimento;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public void setDescrizione(String descrizione) {
        this.descrizione = descrizione;
    }

    public TipoMovimento getTipo() {
        return tipo;
    }

    public void setTipo(TipoMovimento tipo) {
        this.tipo = tipo;
    }

    public BigDecimal getImporto() {
        return importo;
    }

    public void setImporto(BigDecimal importo) {
        this.importo = importo;
    }

    public Trasferimento getTrasferimento() {
        return trasferimento;
    }

    public void setTrasferimento(Trasferimento trasferimento) {
        this.trasferimento = trasferimento;
    }

    public LocalDateTime getCreatoIl() {
        return creatoIl;
    }
}
