package it.appbancaria.web.dto;

import it.appbancaria.model.Movimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dati del movimento esposti dall'API.
 *
 * <p>L'importo viaggia due volte: positivo come e memorizzato ({@code importo}) e con il
 * segno del verso ({@code importoConSegno}), che e la forma da mostrare in una lista. Il
 * tipo resta una stringa ({@code PRELIEVO}, {@code VERSAMENTO}, ...), cosi l'interfaccia
 * puo distinguere entrate e uscite senza ricalcolare nulla.
 *
 * @param id             chiave del movimento
 * @param dataMovimento  data e ora di registrazione
 * @param descrizione    descrizione mostrata all'utente
 * @param tipo           nome della costante di {@code TipoMovimento}
 * @param importo        importo sempre positivo, in valuta del conto
 * @param importoConSegno importo col segno: positivo se entrata, negativo se uscita
 */
public record MovimentoDto(Long id, LocalDateTime dataMovimento, String descrizione,
                           String tipo, BigDecimal importo, BigDecimal importoConSegno) {

    /** Costruisce il DTO a partire dall'entita. */
    public static MovimentoDto da(Movimento movimento) {
        return new MovimentoDto(movimento.getId(), movimento.getDataMovimento(),
                movimento.getDescrizione(), movimento.getTipo().name(),
                movimento.getImporto(), movimento.getImportoConSegno());
    }
}
