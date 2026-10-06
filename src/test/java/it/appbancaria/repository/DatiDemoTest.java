package it.appbancaria.repository;

import it.appbancaria.TestcontainersConfiguration;
import it.appbancaria.model.Conto;
import it.appbancaria.model.Movimento;
import it.appbancaria.model.Utente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Coerenza dei dati demo caricati da {@code V2__seed_demo.sql}.
 *
 * <p>Questi test non verificano una funzionalita ma l'<strong>invariante del progetto</strong>:
 * il saldo di ogni conto deve essere uguale alla somma algebrica dei suoi movimenti
 * (entrate meno uscite). E' la stessa regola applicata dalla riconciliazione in coda al
 * seed e, in produzione, dalla transazione di conferma di un trasferimento.
 *
 * <p>Se qualcuno aggiunge movimenti con una nuova migrazione senza riscrivere i saldi,
 * il test fallisce e lo segnala subito.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class DatiDemoTest {

    @Autowired
    private ContoRepository contoRepository;

    @Autowired
    private MovimentoRepository movimentoRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    @Test
    @DisplayName("Il saldo di ogni conto e uguale alla somma con segno dei suoi movimenti")
    void saldoUgualeAllaSommaDeiMovimenti() {
        List<Conto> conti = contoRepository.findAll();
        assertThat(conti).isNotEmpty();

        for (Conto conto : conti) {
            List<Movimento> movimenti = movimentoRepository.findByContoIdOrderByDataMovimentoDesc(conto.getId());
            BigDecimal somma = movimenti.stream()
                    .map(Movimento::getImportoConSegno)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            assertThat(somma)
                    .as("saldo del conto %s", conto.getCodiceConto())
                    .isEqualByComparingTo(conto.getSaldo());
        }
    }

    @Test
    @DisplayName("Ogni conto ha almeno un movimento e un saldo mai negativo")
    void ogniContoHaMovimentiESaldoNonNegativo() {
        for (Conto conto : contoRepository.findAll()) {
            assertThat(movimentoRepository.findByContoIdOrderByDataMovimentoDesc(conto.getId()))
                    .as("movimenti del conto %s", conto.getCodiceConto())
                    .isNotEmpty();
            assertThat(conto.getSaldo()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }
    }

    @Test
    @DisplayName("I movimenti di ogni conto sono in ordine dal piu recente al meno recente")
    void movimentiOrdinatiDalPiuRecente() {
        for (Conto conto : contoRepository.findAll()) {
            List<Movimento> movimenti = movimentoRepository.findByContoIdOrderByDataMovimentoDesc(conto.getId());

            assertThat(movimenti).extracting(Movimento::getDataMovimento).isSortedAccordingTo(
                    java.util.Comparator.reverseOrder());
        }
    }

    @Test
    @DisplayName("Gli importi demo sono tutti positivi: il verso lo da il tipo, non il segno")
    void importiDemoSemprePositivi() {
        assertThat(movimentoRepository.findAll())
                .isNotEmpty()
                .allMatch(movimento -> movimento.getImporto().signum() > 0);
    }

    @Test
    @DisplayName("I due clienti demo esistono e la password e solo un hash BCrypt, mai in chiaro")
    void passwordDemoSoloComeHash() {
        Utente mario = utenteRepository.findByEmail("mario.rossi@demo.it").orElseThrow();
        Utente giulia = utenteRepository.findByEmail("giulia.bianchi@demo.it").orElseThrow();

        for (Utente utente : List.of(mario, giulia)) {
            String hash = utente.getPasswordHash();
            assertThat(hash)
                    .as("hash di %s", utente.getEmail())
                    .startsWith("$2")
                    .hasSize(60)
                    .isNotEqualTo("Demo1234!");
        }
        assertThat(mario.getNomeCompleto()).isEqualTo("Mario Rossi");
        assertThat(giulia.getNomeCompleto()).isEqualTo("Giulia Bianchi");
    }
}
