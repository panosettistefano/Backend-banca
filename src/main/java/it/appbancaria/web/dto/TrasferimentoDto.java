package it.appbancaria.web.dto;

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
 * @param id               chiave interna, usata nelle URL di conferma
 * @param riferimento      codice pubblico del trasferimento (UUID)
 * @param stato            IN_ATTESA, COMPLETATO oppure RIFIUTATO
 * @param importo          importo trasferito, sempre positivo
 * @param valuta           valuta dei due conti
 * @param causale          causale scritta dal cliente, se presente
 * @param creatoIl         istante della richiesta
 * @param confermatoIl     istante dell'esecuzione, solo per i trasferimenti completati
 * @param motivoRifiuto    motivo del rifiuto, solo per i trasferimenti rifiutati
 * @param contoOrigine     conto addebitato
 * @param contoDestinazione conto accreditato
 */
public record TrasferimentoDto(Long id, String riferimento, String stato, BigDecimal importo,
                               String valuta, String causale, LocalDateTime creatoIl,
                               LocalDateTime confermatoIl, String motivoRifiuto,
                               ContoDto contoOrigine, ContoDto contoDestinazione) {

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
