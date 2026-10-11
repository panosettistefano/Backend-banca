package it.appbancaria.repository;

import it.appbancaria.model.Conto;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Accesso ai conti correnti.
 *
 * <p>Le due ricerche "per titolare" sono il cuore dell'isolamento tra clienti: un conto
 * si legge sempre insieme all'id del cliente autenticato, cosi un cliente non puo
 * raggiungere il conto di un altro nemmeno indovinandone l'id.
 */
@Repository
public interface ContoRepository extends JpaRepository<Conto, Long> {

    /**
     * Tutti i conti di un cliente, dal piu vecchio al piu recente.
     * Alimenta {@code GET /api/accounts}.
     */
    List<Conto> findByTitolareIdOrderByIdAsc(Long titolareId);

    /**
     * Un conto, ma solo se appartiene al cliente indicato: se l'id esiste ma e di un altro
     * cliente il risultato e vuoto, e l'API rispondera 404 senza rivelare che il conto esiste.
     */
    Optional<Conto> findByIdAndTitolareId(Long id, Long titolareId);

    /**
     * Come {@code findByIdAndTitolareId}, ma blocca la riga del conto fino alla fine della
     * transazione ({@code SELECT ... FOR UPDATE}). Serve alla conferma di un trasferimento,
     * dove il saldo viene riletto e poi modificato: senza il blocco due conferme in
     * parallelo leggerebbero lo stesso saldo.
     *
     * <p>Il testo fra "find" e "By" e ignorato da Spring Data: la query e la stessa di
     * {@code findByIdAndTitolareId}, cambia solo il blocco.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Conto> findConBloccoByIdAndTitolareId(Long id, Long titolareId);

    /** Verifica l'esistenza di un codice conto, che e univoco. */
    boolean existsByCodiceConto(String codiceConto);
}
