package it.appbancaria.repository;

import it.appbancaria.DatiDiProva;
import it.appbancaria.TestcontainersConfiguration;
import it.appbancaria.Vincoli;
import it.appbancaria.model.Conto;
import it.appbancaria.model.Utente;
import it.appbancaria.model.Valuta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ricerche e vincoli sui conti.
 *
 * <p>Il test piu importante e quello sull'isolamento: le ricerche per titolare non devono
 * mai restituire i conti di un altro cliente, nemmeno conoscendone l'id.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class ContoRepositoryTest {

    @Autowired
    private ContoRepository contoRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Test
    @DisplayName("I conti di un cliente sono solo i suoi, mai quelli di un altro")
    void contiDelTitolareSoloISuoi() {
        Utente mario = utenteRepository.findByEmail("mario.rossi@demo.it").orElseThrow();
        Utente giulia = utenteRepository.findByEmail("giulia.bianchi@demo.it").orElseThrow();

        List<Conto> contiDiMario = contoRepository.findByTitolareIdOrderByIdAsc(mario.getId());
        List<Conto> contiDiGiulia = contoRepository.findByTitolareIdOrderByIdAsc(giulia.getId());

        assertThat(contiDiMario).isNotEmpty();
        assertThat(contiDiMario).allMatch(conto -> conto.getTitolare().getId().equals(mario.getId()));
        assertThat(contiDiGiulia).allMatch(conto -> conto.getTitolare().getId().equals(giulia.getId()));

        assertThat(contiDiMario).extracting(Conto::getCodiceConto)
                .doesNotContainAnyElementsOf(contiDiGiulia.stream().map(Conto::getCodiceConto).toList());
    }

    @Test
    @DisplayName("Leggere un conto di un altro cliente restituisce vuoto, come se non esistesse")
    void contoDiUnAltroClienteNonEVisibile() {
        Utente mario = utenteRepository.findByEmail("mario.rossi@demo.it").orElseThrow();
        Utente giulia = utenteRepository.findByEmail("giulia.bianchi@demo.it").orElseThrow();
        Conto contoDiMario = contoRepository.findByTitolareIdOrderByIdAsc(mario.getId()).getFirst();

        assertThat(contoRepository.findByIdAndTitolareId(contoDiMario.getId(), mario.getId())).isPresent();
        assertThat(contoRepository.findByIdAndTitolareId(contoDiMario.getId(), giulia.getId())).isEmpty();
        assertThat(contoRepository.findByIdAndTitolareId(-1L, mario.getId())).isEmpty();
    }

    @Test
    @DisplayName("Un cliente senza conti riceve una lista vuota")
    void clienteSenzaConti() {
        Utente nuovo = utenteRepository.save(DatiDiProva.utente("senza.conti@prova.it"));

        assertThat(contoRepository.findByTitolareIdOrderByIdAsc(nuovo.getId())).isEmpty();
    }

    @Test
    @DisplayName("I conti demo sono in euro e hanno un codice in formato IBAN")
    void contiDemoCoerenti() {
        List<Conto> conti = contoRepository.findAll();

        assertThat(conti).isNotEmpty();
        assertThat(conti).allMatch(conto -> conto.getValuta() == Valuta.EUR);
        assertThat(conti).allMatch(conto -> conto.getCodiceConto().length() == 27);
    }

    @Test
    @DisplayName("Un conto con saldo negativo viene rifiutato dal database")
    void rifiutaSaldoNegativo() {
        Utente cliente = utenteRepository.save(DatiDiProva.utente("saldo.negativo@prova.it"));
        Conto conto = DatiDiProva.conto(cliente, "IT00X0000000000000000000003", "-0.01");

        Vincoli.rifiutatoDalDatabase(() -> contoRepository.saveAndFlush(conto), "chk_conto_saldo");
    }

    @Test
    @DisplayName("Due conti non possono avere lo stesso codice")
    void rifiutaCodiceContoDuplicato() {
        Utente cliente = utenteRepository.save(DatiDiProva.utente("codice.duplicato@prova.it"));
        Conto conto = DatiDiProva.conto(cliente, "IT60X0542811101000000123456");   // codice di Mario

        Vincoli.rifiutatoDalDatabase(() -> contoRepository.saveAndFlush(conto), "uk_conto_codice_conto");
    }

    @Test
    @DisplayName("Il titolare di un conto e sempre un cliente salvato, con id e dati completi")
    void titolareDelContoEUnClienteSalvato() {
        Utente cliente = utenteRepository.save(DatiDiProva.utente("titolare@prova.it"));
        Conto conto = contoRepository.saveAndFlush(DatiDiProva.conto(cliente, "IT00X0000000000000000000004"));

        Conto riletto = contoRepository.findByIdAndTitolareId(conto.getId(), cliente.getId()).orElseThrow();
        assertThat(riletto.getTitolare().getEmail()).isEqualTo("titolare@prova.it");
        assertThat(riletto.getSaldo()).isEqualByComparingTo("0.00");
        assertThat(riletto.getCreatoIl()).isNotNull();
    }
}
