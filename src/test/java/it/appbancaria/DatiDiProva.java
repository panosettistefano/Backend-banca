package it.appbancaria;

import it.appbancaria.model.Conto;
import it.appbancaria.model.Movimento;
import it.appbancaria.model.RuoloUtente;
import it.appbancaria.model.TipoMovimento;
import it.appbancaria.model.Utente;
import it.appbancaria.model.Valuta;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Piccola fabbrica di dati di prova, condivisa dai test dei repository.
 *
 * <p>Serve a tenere i test leggibili: ogni test descrive solo la situazione che gli
 * interessa, senza ripetere ogni volta la costruzione di cliente, conto e movimento.
 *
 * <p>Email e codici conto usati qui sono diversi da quelli dei dati demo: cosi i test
 * possono convivere con il seed di Flyway senza violare i vincoli di unicita.
 */
public final class DatiDiProva {

    /**
     * Hash BCrypt finto ma della forma giusta: i test non devono mai contenere
     * una password in chiaro, nemmeno per sbaglio.
     */
    public static final String HASH_FINTO = "$2a$10$hashfintodiprova000000000000000000000000000000000000000000";

    private DatiDiProva() {
    }

    /** Un cliente con dati anagrafici di comodo. */
    public static Utente utente(String email) {
        return new Utente(email, HASH_FINTO, RuoloUtente.CLIENTE, "Cliente", "Di Prova");
    }

    /** Un conto in euro con saldo iniziale zero. */
    public static Conto conto(Utente titolare, String codiceConto) {
        return new Conto(codiceConto, titolare, Valuta.EUR);
    }

    /** Un conto in euro con un saldo gia impostato. */
    public static Conto conto(Utente titolare, String codiceConto, String saldo) {
        Conto conto = conto(titolare, codiceConto);
        conto.setSaldo(new BigDecimal(saldo));
        return conto;
    }

    /** Un movimento "normale" (non collegato a un trasferimento). */
    public static Movimento movimento(Conto conto, LocalDateTime data, TipoMovimento tipo, String importo) {
        return new Movimento(conto, data, "Movimento di prova", tipo, new BigDecimal(importo));
    }
}
