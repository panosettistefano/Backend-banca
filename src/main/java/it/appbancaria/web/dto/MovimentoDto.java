package it.appbancaria.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
 * @param id              chiave del movimento
 * @param dataMovimento   data e ora di registrazione
 * @param descrizione     descrizione mostrata all'utente
 * @param tipo            nome della costante di {@code TipoMovimento}
 * @param importo         importo sempre positivo, in valuta del conto
 * @param importoConSegno importo col segno: positivo se entrata, negativo se uscita
 */
@Schema(description = "Movimento di conto")
public record MovimentoDto(
        @Schema(description = "Chiave del movimento", example = "27")
        Long id,

        @Schema(description = "Data e ora del movimento", example = "2026-09-29T23:21:25.532941")
        LocalDateTime dataMovimento,

        @Schema(description = "Descrizione mostrata all'utente", example = "Pagamento utenze demo")
        String descrizione,

        @Schema(description = "Tipo del movimento", example = "PRELIEVO",
                allowableValues = {"SALDO_INIZIALE", "VERSAMENTO", "PRELIEVO", "TRASFERIMENTO_USCITA", "TRASFERIMENTO_ENTRATA"})
        String tipo,

        @Schema(description = "Importo sempre positivo, nella valuta del conto", example = "48.00")
        BigDecimal importo,

        @Schema(description = "Importo col segno: negativo se il movimento e un'uscita", example = "-48.00")
        BigDecimal importoConSegno) {

    /** Costruisce il DTO a partire dall'entita. */
    public static MovimentoDto da(Movimento movimento) {
        return new MovimentoDto(movimento.getId(), movimento.getDataMovimento(),
                movimento.getDescrizione(), movimento.getTipo().name(),
                movimento.getImporto(), movimento.getImportoConSegno());
    }
}
