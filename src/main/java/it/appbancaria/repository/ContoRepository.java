package it.appbancaria.repository;

import it.appbancaria.model.Conto;
import org.springframework.data.jpa.repository.JpaRepository;
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

    /** Verifica l'esistenza di un codice conto, che e univoco. */
    boolean existsByCodiceConto(String codiceConto);
}
