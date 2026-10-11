package it.appbancaria.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.appbancaria.model.StatoTrasferimento;
import it.appbancaria.model.Trasferimento;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Dati del trasferimento esposti dall'API.
 *
 * <p>Contiene tutto cio che serve all'interfaccia per mostrare la richiesta e il suo esito:
 * il codice pubblico ({@code riferimento}), lo stato, i due conti con i loro saldi attuali
 * e, se c'e stato un rifiuto, il motivo.
 *
 * @param id                chiave interna, usata nelle URL di conferma
 * @param riferimento       codice pubblico del trasferimento (UUID)
 * @param stato             IN_ATTESA, COMPLETATO oppure RIFIUTATO
 * @param importo           importo trasferito, sempre positivo
 * @param valuta            valuta dei due conti
 * @param causale           causale scritta dal cliente, se presente
 * @param creatoIl          istante della richiesta
 * @param confermatoIl      istante dell'esecuzione, solo per i trasferimenti completati
 * @param motivoRifiuto     motivo del rifiuto, solo per i trasferimenti rifiutati
 * @param contoOrigine      conto addebitato
 * @param contoDestinazione conto accreditato
 */
@Schema(description = "Trasferimento simulato: richiesta, esito e conti coinvolti")
public record TrasferimentoDto(
        @Schema(description = "Chiave del trasferimento, usata nelle URL di conferma", example = "1")
        Long id,

        @Schema(description = "Codice pubblico del trasferimento", example = "4328d73f-333a-4c5e-a83f-23896b611412")
        String riferimento,

        @Schema(description = "Stato del trasferimento", example = "IN_ATTESA",
                allowableValues = {"IN_ATTESA", "COMPLETATO", "RIFIUTATO"})
        String stato,

        @Schema(description = "Importo trasferito, sempre positivo", example = "100.00")
        BigDecimal importo,

        @Schema(description = "Valuta dei due conti", example = "EUR")
        String valuta,

        @Schema(description = "Causale scritta dal cliente", example = "Regalo di compleanno")
        String causale,

        @Schema(description = "Istante della richiesta", example = "2026-10-11T14:20:00.123456")
        LocalDateTime creatoIl,

        @Schema(description = "Istante dell'esecuzione: presente solo per i trasferimenti completati",
                example = "2026-10-11T14:25:00.123456")
        LocalDateTime confermatoIl,

        @Schema(description = "Motivo del rifiuto: presente solo per i trasferimenti rifiutati",
                example = "Saldo insufficiente")
        String motivoRifiuto,

        @Schema(description = "Conto addebitato")
        ContoDto contoOrigine,

        @Schema(description = "Conto accreditato")
        ContoDto contoDestinazione) {

    /** Costruisce il DTO a partire dall'entita (i conti vanno letti dentro la transazione). */
    public static TrasferimentoDto da(Trasferimento trasferimento) {
        return new TrasferimentoDto(trasferimento.getId(), trasferimento.getRiferimento(),
                trasferimento.getStato().name(), trasferimento.getImporto(),
                trasferimento.getValuta().name(), trasferimento.getCausale(),
                trasferimento.getCreatoIl(), trasferimento.getConfermatoIl(),
                trasferimento.getMotivoRifiuto(),
                ContoDto.da(trasferimento.getContoOrigine()),
                ContoDto.da(trasferimento.getContoDestinazione()));
    }

    /** Vero se la conferma ha rifiutato il trasferimento: la risposta e 409, non 200. */
    public boolean rifiutato() {
        return StatoTrasferimento.RIFIUTATO.name().equals(stato);
    }
}
