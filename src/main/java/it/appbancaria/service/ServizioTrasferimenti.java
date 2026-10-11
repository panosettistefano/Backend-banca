package it.appbancaria.service;

import it.appbancaria.model.Conto;
import it.appbancaria.model.Movimento;
import it.appbancaria.model.StatoTrasferimento;
import it.appbancaria.model.TipoMovimento;
import it.appbancaria.model.Trasferimento;
import it.appbancaria.repository.ContoRepository;
import it.appbancaria.repository.MovimentoRepository;
import it.appbancaria.repository.TrasferimentoRepository;
import it.appbancaria.web.dto.RichiestaTrasferimento;
import it.appbancaria.web.dto.TrasferimentoDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Trasferimenti simulati in due fasi: la richiesta registra, la conferma esegue.
 *
 * <p><strong>La richiesta non muove denaro</strong> e non prenota fondi: controlla soltanto
 * che i conti siano del cliente, che siano diversi, che abbiano la stessa valuta e che il
 * saldo basti in quel momento. Per questo il saldo viene ricontrollato alla conferma: fra
 * i due momenti puo essere cambiato.
 *
 * <p><strong>La conferma e una sola transazione</strong>: addebita, accredita, scrive i due
 * movimenti collegati e chiude il trasferimento. Se un passaggio qualsiasi fallisce non
 * resta niente a meta. I conti vengono bloccati (riga per riga, sempre dal piu piccolo al
 * piu grande) prima di rileggere i saldi: due conferme in parallelo si mettono in fila
 * invece di leggere lo stesso saldo, e due trasferimenti incrociati non si bloccano a
 * vicenda.
 *
 * <p>Se alla conferma il saldo non basta piu, il trasferimento viene registrato come
 * RIFIUTATO con il motivo: e una decisione, non un errore, quindi la transazione si
 * conclude normalmente (la risposta HTTP e 409, ma il rifiuto resta scritto).
 */
@Service
public class ServizioTrasferimenti {

    private final TrasferimentoRepository trasferimentoRepository;
    private final ContoRepository contoRepository;
    private final MovimentoRepository movimentoRepository;

    public ServizioTrasferimenti(TrasferimentoRepository trasferimentoRepository,
                                 ContoRepository contoRepository,
                                 MovimentoRepository movimentoRepository) {
        this.trasferimentoRepository = trasferimentoRepository;
        this.contoRepository = contoRepository;
        this.movimentoRepository = movimentoRepository;
    }

    /**
     * Registra la richiesta di trasferimento in stato IN_ATTESA, senza toccare i saldi.
     *
     * @throws ContoNonTrovatoException      conto inesistente o di un altro cliente
     * @throws RichiestaNonValidaException   conti coincidenti o valute diverse
     * @throws SaldoInsufficienteException   saldo del conto di origine non capiente
     */
    @Transactional
    public TrasferimentoDto richiedi(RichiestaTrasferimento richiesta, Long clienteId) {
        // Le due ricerche filtrate per titolare sono anche il controllo di appartenenza:
        // un conto di un altro cliente non si trova, e la risposta e la stessa di un conto
        // inesistente.
        Conto origine = contoRepository.findByIdAndTitolareId(richiesta.contoOrigineId(), clienteId)
                .orElseThrow(ContoNonTrovatoException::new);
        Conto destinazione = contoRepository.findByIdAndTitolareId(richiesta.contoDestinazioneId(), clienteId)
                .orElseThrow(ContoNonTrovatoException::new);

        if (origine.getId().equals(destinazione.getId())) {
            throw new RichiestaNonValidaException("I due conti devono essere diversi");
        }
        if (origine.getValuta() != destinazione.getValuta()) {
            throw new RichiestaNonValidaException("I due conti devono avere la stessa valuta");
        }
        if (origine.getSaldo().compareTo(richiesta.importo()) < 0) {
            throw new SaldoInsufficienteException();
        }

        // Il titolare del conto e il cliente autenticato: lo garantisce la ricerca filtrata,
        // quindi non serve rileggere l'utente per id.
        Trasferimento trasferimento = new Trasferimento(UUID.randomUUID().toString(),
                origine.getTitolare(), origine, destinazione, richiesta.importo(),
                causalePulita(richiesta.causale()));

        return TrasferimentoDto.da(trasferimentoRepository.saveAndFlush(trasferimento));
    }

    /**
     * Esegue il trasferimento: saldi e movimenti in una sola transazione.
     *
     * @return il trasferimento completato, oppure rifiutato se il saldo non basta piu
     * @throws TrasferimentoNonTrovatoException  trasferimento inesistente o di un altro cliente
     * @throws TrasferimentoNonInAttesaException gia confermato o rifiutato in precedenza
     */
    @Transactional
    public TrasferimentoDto conferma(Long trasferimentoId, Long clienteId) {
        // Il blocco sulla riga del trasferimento mette in fila due conferme in parallelo:
        // la seconda vede lo stato ormai COMPLETATO e non esegue niente.
        Trasferimento trasferimento = trasferimentoRepository
                .findConBloccoByIdAndUtenteId(trasferimentoId, clienteId)
                .orElseThrow(TrasferimentoNonTrovatoException::new);

        if (trasferimento.getStato() != StatoTrasferimento.IN_ATTESA) {
            throw new TrasferimentoNonInAttesaException();
        }

        Conto origine = bloccato(trasferimento.getContoOrigine().getId(), clienteId);
        Conto destinazione = bloccato(trasferimento.getContoDestinazione().getId(), clienteId);

        if (origine.getSaldo().compareTo(trasferimento.getImporto()) < 0) {
            trasferimento.setStato(StatoTrasferimento.RIFIUTATO);
            trasferimento.setMotivoRifiuto("Saldo insufficiente");
            return TrasferimentoDto.da(trasferimento);
        }

        LocalDateTime adesso = LocalDateTime.now();
        origine.setSaldo(origine.getSaldo().subtract(trasferimento.getImporto()));
        destinazione.setSaldo(destinazione.getSaldo().add(trasferimento.getImporto()));

        movimentoRepository.save(movimento(origine, trasferimento, TipoMovimento.TRASFERIMENTO_USCITA,
                adesso, "Trasferimento verso " + destinazione.getCodiceConto()));
        movimentoRepository.save(movimento(destinazione, trasferimento, TipoMovimento.TRASFERIMENTO_ENTRATA,
                adesso, "Trasferimento da " + origine.getCodiceConto()));

        trasferimento.setStato(StatoTrasferimento.COMPLETATO);
        trasferimento.setConfermatoIl(adesso);

        return TrasferimentoDto.da(trasferimento);
    }

    /** Trasferimenti del cliente in un certo stato, dal piu recente al piu vecchio. */
    @Transactional(readOnly = true)
    public List<TrasferimentoDto> elenco(Long clienteId, StatoTrasferimento stato) {
        return trasferimentoRepository.findByUtenteIdAndStatoOrderByCreatoIlDesc(clienteId, stato)
                .stream()
                .map(TrasferimentoDto::da)
                .toList();
    }

    /**
     * Rilegge il conto bloccandone la riga fino alla fine della transazione. I due conti si
     * bloccano sempre in ordine di id: due trasferimenti incrociati (A verso B e B verso A)
     * prendono i blocchi nello stesso ordine e non si aspettano a vicenda.
     */
    private Conto bloccato(Long contoId, Long clienteId) {
        return contoRepository.findConBloccoByIdAndTitolareId(contoId, clienteId)
                .orElseThrow(ContoNonTrovatoException::new);
    }

    /** Un movimento collegato al trasferimento, con la causale del cliente se c'e. */
    private Movimento movimento(Conto conto, Trasferimento trasferimento, TipoMovimento tipo,
                                LocalDateTime quando, String descrizione) {
        Movimento movimento = new Movimento(conto, quando,
                descrizione + causale(trasferimento), tipo, trasferimento.getImporto());
        movimento.setTrasferimento(trasferimento);
        return movimento;
    }

    private String causale(Trasferimento trasferimento) {
        return trasferimento.getCausale() == null ? "" : " - " + trasferimento.getCausale();
    }

    /** La causale vuota o di soli spazi diventa null: nel database non entrano stringhe vuote. */
    private String causalePulita(String causale) {
        return (causale == null || causale.isBlank()) ? null : causale.trim();
    }
}
