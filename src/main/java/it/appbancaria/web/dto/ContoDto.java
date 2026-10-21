package it.appbancaria.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.appbancaria.model.Conto;

import java.math.BigDecimal;

/**
 * Dati del conto esposti dall'API.
 *
 * <p>Il titolare viene letto dentro la transazione che costruisce il DTO: la relazione
 * {@code Conto.titolare} e LAZY e con {@code open-in-view: false} non va toccata fuori
 * da una transazione aperta.
 *
 * <p>Il saldo e un numero JSON con due decimali, come la colonna {@code DECIMAL(15,2)}:
 * mai un {@code double}, che non rappresenta esattamente i centesimi.
 *
 * @param id          chiave del conto
 * @param codiceConto codice in formato IBAN
 * @param intestatario nome e cognome del titolare
 * @param valuta      codice ISO 4217 della valuta del conto
 * @param saldo       saldo attuale nella valuta del conto
 */
@Schema(description = "Conto del cliente autenticato")
public record ContoDto(
        @Schema(description = "Chiave del conto", example = "3")
        Long id,

        @Schema(description = "Codice del conto in formato IBAN", example = "IT60X0542811101000000123456")
        String codiceConto,

        @Schema(description = "Intestatario del conto", example = "Mario Rossi")
        String intestatario,

        @Schema(description = "Codice ISO 4217 della valuta", example = "EUR")
        String valuta,

        @Schema(description = "Saldo attuale, nella valuta del conto", example = "8742.00")
        BigDecimal saldo) {

    /** Costruisce il DTO a partire dall'entita (va chiamato dentro una transazione). */
    public static ContoDto da(Conto conto) {
        return new ContoDto(conto.getId(), conto.getCodiceConto(),
                conto.getTitolare().getNomeCompleto(), conto.getValuta().name(), conto.getSaldo());
    }
}
