package it.appbancaria.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Cliente dell'applicazione dimostrativa: anagrafica e credenziali di accesso.
 *
 * <p>La password non esiste in chiaro da nessuna parte: qui c'e solo l'hash BCrypt
 * prodotto in fase di registrazione (colonna {@code password_hash}).
 *
 * <p>Mappa la tabella {@code utente} di {@code V1__schema.sql}. Lo schema appartiene a
 * Flyway: {@code ddl-auto: validate} verifica soltanto che questa classe e la tabella
 * combacino, senza mai modificare il database.
 */
@Entity
@Table(name = "utente")
public class Utente {

    /** Chiave primaria generata dal database ({@code AUTO_INCREMENT}). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Email di accesso, univoca nell'intero sistema (vincolo {@code uk_utente_email}). */
    @Column(name = "email", nullable = false, unique = true, length = 180)
    private String email;

    /** Hash BCrypt della password (60 caratteri): mai la password in chiaro. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    /**
     * Ruolo dell'utente.
     * {@link EnumType#STRING} salva il nome della costante ("CLIENTE"), non la sua
     * posizione: aggiungere un ruolo in mezzo all'enum non falsa i dati esistenti.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "ruolo", nullable = false, length = 20)
    private RuoloUtente ruolo;

    /** Nome di battesimo. */
    @Column(name = "nome", nullable = false, length = 80)
    private String nome;

    /** Cognome. */
    @Column(name = "cognome", nullable = false, length = 80)
    private String cognome;

    /**
     * Istante di creazione, valorizzato da Hibernate al primo salvataggio.
     * La colonna ha anche un default nel database, ma cosi il valore e leggibile
     * subito dopo il salvataggio senza rileggere la riga.
     */
    @CreationTimestamp
    @Column(name = "creato_il", nullable = false, updatable = false)
    private LocalDateTime creatoIl;

    /** Costruttore richiesto da JPA: non va usato direttamente nel codice applicativo. */
    protected Utente() {
    }

    /** Crea un cliente con i dati essenziali; id e data di creazione li assegna la persistenza. */
    public Utente(String email, String passwordHash, RuoloUtente ruolo, String nome, String cognome) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.ruolo = ruolo;
        this.nome = nome;
        this.cognome = cognome;
    }

    /** Nome e cognome uniti, per i messaggi dell'interfaccia. */
    public String getNomeCompleto() {
        return nome + " " + cognome;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public RuoloUtente getRuolo() {
        return ruolo;
    }

    public void setRuolo(RuoloUtente ruolo) {
        this.ruolo = ruolo;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCognome() {
        return cognome;
    }

    public void setCognome(String cognome) {
        this.cognome = cognome;
    }

    public LocalDateTime getCreatoIl() {
        return creatoIl;
    }
}
