package it.appbancaria.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Corpo della richiesta di un trasferimento simulato.
 *
 * <p>I vincoli scritti qui coprono il formato dei dati. Le regole che dipendono dallo stato
 * del sistema (conti esistenti e del cliente, conti diversi, stessa valuta, saldo
 * capiente) le controlla il servizio, che puo leggere il database.
 *
 * @param contoOrigineId      conto da addebitare
 * @param contoDestinazioneId conto da accreditare
 * @param importo             importo positivo, al massimo due decimali
 * @param causale             testo facoltativo scritto dal cliente
 */
@Schema(description = "Richiesta di un trasferimento fra due conti del cliente autenticato")
public record RichiestaTrasferimento(
        @Schema(description = "Chiave del conto da addebitare", example = "3")
        @NotNull(message = "Il conto di origine e obbligatorio")
        Long contoOrigineId,

        @Schema(description = "Chiave del conto da accreditare", example = "4")
        @NotNull(message = "Il conto di destinazione e obbligatorio")
        Long contoDestinazioneId,

        @Schema(description = "Importo da trasferire, positivo e con al massimo due decimali", example = "100.00")
        @NotNull(message = "L'importo e obbligatorio")
        @DecimalMin(value = "0.01", message = "L'importo deve essere positivo")
        @Digits(integer = 13, fraction = 2, message = "L'importo puo avere al massimo due decimali")
        BigDecimal importo,

        @Schema(description = "Causale facoltativa, scritta dal cliente", example = "Regalo di compleanno")
        @Size(max = 140, message = "La causale puo avere al massimo 140 caratteri")
        String causale) {
}
