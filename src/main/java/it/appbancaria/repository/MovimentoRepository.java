package it.appbancaria.repository;

import it.appbancaria.model.Movimento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Accesso ai movimenti di conto.
 *
 * <p>I filtri di {@code GET /api/accounts/{id}/movements?da=&a=} sono opzionali e le
 * combinazioni possibili sono quattro: entrambe le date, solo la data iniziale, solo la
 * data finale, nessuna delle due. Sono scritte come quattro ricerche distinte invece di
 * una sola con parametri nulli: la query e sempre esplicita e non serve nessun JPQL.
 *
 * <p>L'ordinamento e sempre dal movimento piu recente al meno recente, come richiesto
 * dai requisiti, e sfrutta l'indice {@code idx_movimento_conto_data}.
 */
@Repository
public interface MovimentoRepository extends JpaRepository<Movimento, Long> {

    /** Movimenti di un conto, senza filtri. */
    List<Movimento> findByContoIdOrderByDataMovimentoDesc(Long contoId);

    /** Movimenti di un conto compresi fra le due date (estremi inclusi). */
    List<Movimento> findByContoIdAndDataMovimentoBetweenOrderByDataMovimentoDesc(
            Long contoId, LocalDateTime da, LocalDateTime a);

    /** Movimenti di un conto dalla data indicata in avanti (estremo incluso). */
    List<Movimento> findByContoIdAndDataMovimentoGreaterThanEqualOrderByDataMovimentoDesc(
            Long contoId, LocalDateTime da);

    /** Movimenti di un conto fino alla data indicata (estremo incluso). */
    List<Movimento> findByContoIdAndDataMovimentoLessThanEqualOrderByDataMovimentoDesc(
            Long contoId, LocalDateTime a);
}
