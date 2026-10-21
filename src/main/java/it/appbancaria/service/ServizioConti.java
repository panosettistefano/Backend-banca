package it.appbancaria.service;

import it.appbancaria.model.Movimento;
import it.appbancaria.repository.ContoRepository;
import it.appbancaria.repository.MovimentoRepository;
import it.appbancaria.web.dto.ContoDto;
import it.appbancaria.web.dto.MovimentoDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Conti e movimenti del cliente autenticato.
 *
 * <p>L'isolamento tra clienti sta nei dati, non nei ruoli: ogni ricerca parte dall'id del
 * cliente autenticato, quindi il conto di un altro non e raggiungibile nemmeno
 * conoscendone l'id. Per chi lo chiede l'esito e identico a quello di un conto che non
 * esiste: 404.
 *
 * <p>{@code readOnly = true} perche le due operazioni leggono soltanto, e il mapping verso
 * i DTO avviene dentro la transazione, dove le associazioni LAZY sono ancora caricabili
 * ({@link ContoDto} legge il titolare del conto).
 */
@Service
@Transactional(readOnly = true)
public class ServizioConti {

    private final ContoRepository contoRepository;
    private final MovimentoRepository movimentoRepository;

    public ServizioConti(ContoRepository contoRepository, MovimentoRepository movimentoRepository) {
        this.contoRepository = contoRepository;
        this.movimentoRepository = movimentoRepository;
    }

    /** I conti del cliente, dal piu vecchio al piu recente. */
    public List<ContoDto> contiDi(Long clienteId) {
        return contoRepository.findByTitolareIdOrderByIdAsc(clienteId)
                .stream()
                .map(ContoDto::da)
                .toList();
    }

    /**
     * I movimenti di un conto del cliente, dal piu recente al meno recente.
     *
     * <p>{@code da} e {@code a} sono giorni interi: la data iniziale vale dalla mezzanotte,
     * la finale fino a fine giornata. Le combinazioni possibili sono quattro e la scelta
     * fra le ricerche del repository e esplicita, cosi la query eseguita si legge subito.
     *
     * @param contoId   conto richiesto
     * @param clienteId cliente autenticato: il conto deve essere suo
     * @param da        primo giorno incluso, oppure {@code null}
     * @param a         ultimo giorno incluso, oppure {@code null}
     * @throws ContoNonTrovatoException conto inesistente o di un altro cliente
     */
    public List<MovimentoDto> movimentiDi(Long contoId, Long clienteId, LocalDate da, LocalDate a) {
        // La ricerca filtrata per titolare e anche il controllo di appartenenza: se il conto
        // esiste ma e di un altro, qui non si trova e la risposta e la stessa di un conto
        // inesistente.
        contoRepository.findByIdAndTitolareId(contoId, clienteId)
                .orElseThrow(ContoNonTrovatoException::new);

        List<Movimento> movimenti;
        if (da != null && a != null) {
            movimenti = movimentoRepository.findByContoIdAndDataMovimentoBetweenOrderByDataMovimentoDesc(
                    contoId, inizioGiornata(da), fineGiornata(a));
        } else if (da != null) {
            movimenti = movimentoRepository.findByContoIdAndDataMovimentoGreaterThanEqualOrderByDataMovimentoDesc(
                    contoId, inizioGiornata(da));
        } else if (a != null) {
            movimenti = movimentoRepository.findByContoIdAndDataMovimentoLessThanEqualOrderByDataMovimentoDesc(
                    contoId, fineGiornata(a));
        } else {
            movimenti = movimentoRepository.findByContoIdOrderByDataMovimentoDesc(contoId);
        }

        return movimenti.stream().map(MovimentoDto::da).toList();
    }

    /** La data iniziale comprende tutta la giornata, dalla mezzanotte. */
    private LocalDateTime inizioGiornata(LocalDate giorno) {
        return giorno.atStartOfDay();
    }

    /**
     * La data finale comprende tutta la giornata. L'ultimo istante utile si ferma a
     * 999999 microsecondi e non usa {@link LocalTime#MAX}: la colonna e {@code DATETIME(6)}
     * e 999999999 nanosecondi arrotonderebbero al giorno successivo, includendo un
     * movimento di troppo.
     */
    private LocalDateTime fineGiornata(LocalDate giorno) {
        return giorno.atTime(23, 59, 59, 999_999_000);
    }
}
