package it.appbancaria.repository;

import it.appbancaria.model.Utente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Accesso ai clienti registrati.
 *
 * <p>Estendendo {@link JpaRepository} si ottengono gratis le operazioni di base
 * ({@code save}, {@code findById}, {@code findAll}, ...); qui si aggiungono solo le
 * ricerche che servono davvero, scritte come query derivate dal nome del metodo.
 */
@Repository
public interface UtenteRepository extends JpaRepository<Utente, Long> {

    /** Cerca un cliente dalla sua email di accesso, che e univoca. */
    Optional<Utente> findByEmail(String email);

    /** Verifica se l'email e gia registrata, senza caricare l'intera entita. */
    boolean existsByEmail(String email);
}
