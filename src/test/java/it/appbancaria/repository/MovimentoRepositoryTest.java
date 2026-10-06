package it.appbancaria.repository;

import it.appbancaria.DatiDiProva;
import it.appbancaria.TestcontainersConfiguration;
import it.appbancaria.Vincoli;
import it.appbancaria.model.Conto;
import it.appbancaria.model.Movimento;
import it.appbancaria.model.TipoMovimento;
import it.appbancaria.model.Utente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Filtri e vincoli dei movimenti.
 *
 * <p>Gira su un MySQL vero in container: lo schema arriva dalle migrazioni Flyway e
 * {@code ddl-auto: validate} verifica che le entita combacino con le tabelle. Ogni test
 * e racchiuso in una transazione che viene annullata alla fine, quindi i dati creati qui
 * non restano nel database.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class MovimentoRepositoryTest {

    private static final String CODICE_CONTO = "IT00X0000000000000000000001";
    private static final String CODICE_CONTO_ALTRO = "IT00X0000000000000000000002";

    private static final LocalDateTime GENNAIO = LocalDateTime.of(2026, 1, 5, 9, 0);
    private static final LocalDateTime FEBBRAIO = LocalDateTime.of(2026, 2, 10, 12, 30);
    private static final LocalDateTime MARZO = LocalDateTime.of(2026, 3, 10, 18, 45);
    private static final LocalDateTime APRILE = LocalDateTime.of(2026, 4, 20, 8, 15);

    @Autowired
    private MovimentoRepository movimentoRepository;

    @Autowired
    private ContoRepository contoRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    private Conto conto;

    /** Un conto con quattro movimenti a date note, piu un secondo conto di disturbo. */
    @BeforeEach
    void preparaDueContiConMovimenti() {
        Utente cliente = utenteRepository.save(DatiDiProva.utente("filtri.movimenti@prova.it"));
        conto = contoRepository.save(DatiDiProva.conto(cliente, CODICE_CONTO));

        movimentoRepository.save(DatiDiProva.movimento(conto, GENNAIO, TipoMovimento.SALDO_INIZIALE, "2500.00"));
        movimentoRepository.save(DatiDiProva.movimento(conto, FEBBRAIO, TipoMovimento.VERSAMENTO, "1500.00"));
        movimentoRepository.save(DatiDiProva.movimento(conto, MARZO, TipoMovimento.PRELIEVO, "45.00"));
        movimentoRepository.save(DatiDiProva.movimento(conto, APRILE, TipoMovimento.VERSAMENTO, "120.00"));

        Conto altroConto = contoRepository.save(DatiDiProva.conto(cliente, CODICE_CONTO_ALTRO));
        movimentoRepository.save(DatiDiProva.movimento(altroConto, MARZO, TipoMovimento.PRELIEVO, "999.00"));
    }

    @Test
    @DisplayName("Senza filtri restituisce tutti i movimenti del conto, dal piu recente al meno recente")
    void senzaFiltriOrdinaDalPiuRecente() {
        List<Movimento> movimenti = movimentoRepository.findByContoIdOrderByDataMovimentoDesc(conto.getId());

        assertThat(movimenti).hasSize(4);
        assertThat(movimenti).extracting(Movimento::getDataMovimento)
                .containsExactly(APRILE, MARZO, FEBBRAIO, GENNAIO);
    }

    @Test
    @DisplayName("Il filtro per intervallo di date restituisce solo i movimenti del periodo, estremi inclusi")
    void intervalloDateRestituisceSoloIlPeriodo() {
        List<Movimento> movimenti = movimentoRepository
                .findByContoIdAndDataMovimentoBetweenOrderByDataMovimentoDesc(
                        conto.getId(), LocalDateTime.of(2026, 2, 1, 0, 0), LocalDateTime.of(2026, 3, 31, 23, 59, 59));

        assertThat(movimenti).extracting(Movimento::getDataMovimento).containsExactly(MARZO, FEBBRAIO);
        assertThat(movimenti).extracting(Movimento::getDescrizione)
                .containsOnly("Movimento di prova");
    }

    @Test
    @DisplayName("Gli estremi dell'intervallo sono compresi: passando le date dei movimenti li ritrovo entrambi")
    void estremiDellIntervalloInclusi() {
        List<Movimento> movimenti = movimentoRepository
                .findByContoIdAndDataMovimentoBetweenOrderByDataMovimentoDesc(conto.getId(), GENNAIO, FEBBRAIO);

        assertThat(movimenti).extracting(Movimento::getDataMovimento).containsExactly(FEBBRAIO, GENNAIO);
    }

    @Test
    @DisplayName("Con la sola data iniziale restituisce i movimenti da quella data in avanti")
    void soloDataIniziale() {
        List<Movimento> movimenti = movimentoRepository
                .findByContoIdAndDataMovimentoGreaterThanEqualOrderByDataMovimentoDesc(
                        conto.getId(), LocalDateTime.of(2026, 3, 1, 0, 0));

        assertThat(movimenti).extracting(Movimento::getDataMovimento).containsExactly(APRILE, MARZO);
    }

    @Test
    @DisplayName("Con la sola data finale restituisce i movimenti fino a quella data")
    void soloDataFinale() {
        List<Movimento> movimenti = movimentoRepository
                .findByContoIdAndDataMovimentoLessThanEqualOrderByDataMovimentoDesc(
                        conto.getId(), LocalDateTime.of(2026, 2, 28, 23, 59, 59));

        assertThat(movimenti).extracting(Movimento::getDataMovimento).containsExactly(FEBBRAIO, GENNAIO);
    }

    @Test
    @DisplayName("Un intervallo senza movimenti restituisce una lista vuota, non un errore")
    void intervalloVuotoRestituisceListaVuota() {
        List<Movimento> movimenti = movimentoRepository
                .findByContoIdAndDataMovimentoBetweenOrderByDataMovimentoDesc(
                        conto.getId(), LocalDateTime.of(2027, 1, 1, 0, 0), LocalDateTime.of(2027, 12, 31, 23, 59, 59));

        assertThat(movimenti).isEmpty();
    }

    @Test
    @DisplayName("I movimenti di un altro conto non compaiono mai nei risultati")
    void nonRestituisceIMovimentiDiAltriConti() {
        List<Movimento> delConto = movimentoRepository.findByContoIdOrderByDataMovimentoDesc(conto.getId());
        List<Movimento> dellAltroConto = movimentoRepository.findByContoIdOrderByDataMovimentoDesc(
                contoRepository.findAll().stream()
                        .filter(c -> CODICE_CONTO_ALTRO.equals(c.getCodiceConto()))
                        .findFirst()
                        .orElseThrow()
                        .getId());

        assertThat(delConto).hasSize(4);
        assertThat(dellAltroConto).hasSize(1);
        assertThat(delConto).extracting(Movimento::getConto)
                .allMatch(c -> c.getId().equals(conto.getId()));
    }

    @Test
    @DisplayName("L'importo con segno e positivo per le entrate e negativo per le uscite")
    void importoConSegnoSecondoIlTipo() {
        List<Movimento> movimenti = movimentoRepository.findByContoIdOrderByDataMovimentoDesc(conto.getId());

        assertThat(movimenti).extracting(Movimento::getImportoConSegno)
                .containsExactly(new BigDecimal("120.00"), new BigDecimal("-45.00"),
                        new BigDecimal("1500.00"), new BigDecimal("2500.00"));
    }

    @Test
    @DisplayName("Un movimento con importo non positivo viene rifiutato dal database")
    void rifiutaImportoNonPositivo() {
        Movimento senzaImporto = DatiDiProva.movimento(conto, APRILE, TipoMovimento.VERSAMENTO, "0.00");

        Vincoli.rifiutatoDalDatabase(() -> movimentoRepository.saveAndFlush(senzaImporto), "chk_movimento_importo");
    }

    @Test
    @DisplayName("Un movimento di trasferimento senza riferimento al trasferimento viene rifiutato")
    void rifiutaMovimentoDiTrasferimentoSenzaRiferimento() {
        Movimento uscitaSenzaTrasferimento =
                DatiDiProva.movimento(conto, APRILE, TipoMovimento.TRASFERIMENTO_USCITA, "50.00");

        Vincoli.rifiutatoDalDatabase(() -> movimentoRepository.saveAndFlush(uscitaSenzaTrasferimento),
                "chk_movimento_riferimento");
    }
}
