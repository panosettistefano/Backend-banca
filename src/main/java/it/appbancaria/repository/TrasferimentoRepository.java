package it.appbancaria.repository;

import it.appbancaria.model.StatoTrasferimento;
import it.appbancaria.model.Trasferimento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Accesso ai trasferimenti simulati.
 *
 * <p>La ricerca per riferimento (l'UUID mostrato all'utente) e la ricerca per stato,
 * che alimenta l'elenco dei trasferimenti da confermare, sono sempre limitate al cliente
 * autenticato: anche qui l'isolamento tra clienti passa dal nome del metodo.
 */
@Repository
public interface TrasferimentoRepository extends JpaRepository<Trasferimento, Long> {

    /** Cerca un trasferimento dal suo riferimento pubblico, che e univoco. */
    Optional<Trasferimento> findByRiferimento(String riferimento);

    /** Trasferimenti di un cliente in un certo stato, dal piu recente al piu vecchio. */
    List<Trasferimento> findByUtenteIdAndStatoOrderByCreatoIlDesc(Long utenteId, StatoTrasferimento stato);

    /**
     * Un trasferimento del cliente con la riga bloccata fino alla fine della transazione
     * ({@code SELECT ... FOR UPDATE}): due conferme in parallelo si mettono in fila e solo
     * la prima esegue, la seconda trova lo stato ormai cambiato.
     *
     * <p>Il testo fra "find" e "By" e ignorato da Spring Data: la query e la stessa di
     * {@code findByIdAndUtenteId}, cambia solo il blocco.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Trasferimento> findConBloccoByIdAndUtenteId(Long id, Long utenteId);
}
