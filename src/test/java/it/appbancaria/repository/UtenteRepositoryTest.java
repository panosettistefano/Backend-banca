package it.appbancaria.repository;

import it.appbancaria.DatiDiProva;
import it.appbancaria.TestcontainersConfiguration;
import it.appbancaria.Vincoli;
import it.appbancaria.model.RuoloUtente;
import it.appbancaria.model.Utente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ricerche e vincoli sui clienti.
 *
 * <p>I test che partono dai dati demo usano il seed di {@code V2__seed_demo.sql}: sono
 * anche una verifica che la migrazione e il modello JPA combacino.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class UtenteRepositoryTest {

    @Autowired
    private UtenteRepository utenteRepository;

    @Test
    @DisplayName("Trova il cliente demo dalla sua email")
    void trovaClienteDemoPerEmail() {
        Utente mario = utenteRepository.findByEmail("mario.rossi@demo.it").orElseThrow();

        assertThat(mario.getNome()).isEqualTo("Mario");
        assertThat(mario.getCognome()).isEqualTo("Rossi");
        assertThat(mario.getRuolo()).isEqualTo(RuoloUtente.CLIENTE);
        assertThat(mario.getNomeCompleto()).isEqualTo("Mario Rossi");
        assertThat(mario.getId()).isNotNull();
        assertThat(mario.getCreatoIl()).isNotNull();
    }

    @Test
    @DisplayName("Un'email sconosciuta restituisce Optional vuoto, senza errori")
    void emailSconosciutaRestituisceVuoto() {
        assertThat(utenteRepository.findByEmail("nessuno@demo.it")).isEmpty();
        assertThat(utenteRepository.existsByEmail("nessuno@demo.it")).isFalse();
        assertThat(utenteRepository.existsByEmail("mario.rossi@demo.it")).isTrue();
    }

    @Test
    @DisplayName("Due clienti non possono avere la stessa email")
    void rifiutaEmailDuplicata() {
        Utente doppione = DatiDiProva.utente("mario.rossi@demo.it");

        Vincoli.rifiutatoDalDatabase(() -> utenteRepository.saveAndFlush(doppione), "uk_utente_email");
    }

    @Test
    @DisplayName("Salvataggio e rilettura conservano i dati del cliente")
    void salvaERileggeUnCliente() {
        Utente salvato = utenteRepository.saveAndFlush(DatiDiProva.utente("nuovo.cliente@prova.it"));

        Utente riletto = utenteRepository.findByEmail("nuovo.cliente@prova.it").orElseThrow();
        assertThat(riletto.getId()).isEqualTo(salvato.getId());
        assertThat(riletto.getPasswordHash()).isEqualTo(DatiDiProva.HASH_FINTO);
    }
}
