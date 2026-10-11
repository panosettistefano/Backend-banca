package it.appbancaria.service;

import it.appbancaria.DatiDiProva;
import it.appbancaria.TestcontainersConfiguration;
import it.appbancaria.model.Conto;
import it.appbancaria.model.Movimento;
import it.appbancaria.model.StatoTrasferimento;
import it.appbancaria.model.TipoMovimento;
import it.appbancaria.model.Utente;
import it.appbancaria.model.Valuta;
import it.appbancaria.repository.ContoRepository;
import it.appbancaria.repository.MovimentoRepository;
import it.appbancaria.repository.TrasferimentoRepository;
import it.appbancaria.repository.UtenteRepository;
import it.appbancaria.web.dto.RichiestaTrasferimento;
import it.appbancaria.web.dto.TrasferimentoDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Atomicita della conferma, provata sulla transazione vera e non sull'HTTP.
 *
 * <p>Due scenari che dall'API non si possono costruire: un vincolo del database che
 * rifiuta l'ultimo passo della transazione (annullamento totale) e due conferme in
 * parallelo sullo stesso trasferimento (esecuzione una volta sola).
 *
 * <p>I clienti e i conti di questi test sono creati qui, con email e codici conto dedicati:
 * le conferme spostano denaro davvero e i dati demo non vanno toccati. Ogni conto nasce con
 * il suo movimento di saldo iniziale, cosi l'invariante
 * {@code saldo = somma con segno dei movimenti} resta vera per tutti i conti del database
 * (lo verifica {@code DatiDemoTest}).
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class ServizioTrasferimentiTest {

    @Autowired
    private ServizioTrasferimenti servizioTrasferimenti;

    @Autowired
    private UtenteRepository utenteRepository;

    @Autowired
    private ContoRepository contoRepository;

    @Autowired
    private MovimentoRepository movimentoRepository;

    @Autowired
    private TrasferimentoRepository trasferimentoRepository;

    @Test
    @DisplayName("Se il database rifiuta un movimento, la transazione si annulla per intero")
    void annullamentoSeIlDatabaseRifiutaUnMovimento() {
        Utente cliente = cliente("cliente.rollback@prova.it");
        Conto origine = conto(cliente, "IT00X0000000000000000000001", "1000.00");
        Conto destinazione = conto(cliente, "IT00X0000000000000000000002", "1000.00");
        Conto contoDiDisturbo = conto(cliente, "IT00X0000000000000000000003", "1000.00");

        TrasferimentoDto richiesto = servizioTrasferimenti.richiedi(new RichiestaTrasferimento(
                origine.getId(), destinazione.getId(), new BigDecimal("100.00"), null), cliente.getId());

        // Un movimento che occupa la coppia (trasferimento, tipo) prima della conferma:
        // l'inserimento del movimento di uscita violera uk_movimento_trasferimento_tipo.
        // Serve un conto qualsiasi, non uno dei due del trasferimento; il saldo del conto
        // di disturbo viene abbassato di altrettanto, cosi l'invariante resta vera.
        Movimento intruso = new Movimento(contoDiDisturbo, LocalDateTime.now(), "Movimento di disturbo",
                TipoMovimento.TRASFERIMENTO_USCITA, new BigDecimal("1.00"));
        intruso.setTrasferimento(trasferimentoRepository.getReferenceById(richiesto.id()));
        movimentoRepository.saveAndFlush(intruso);
        contoDiDisturbo.setSaldo(new BigDecimal("999.00"));
        contoRepository.saveAndFlush(contoDiDisturbo);

        // La conferma fallisce a meta: i saldi erano gia stati toccati in memoria, ma la
        // transazione non arriva al commit e non resta niente.
        assertThatThrownBy(() -> servizioTrasferimenti.conferma(richiesto.id(), cliente.getId()))
                .as("la conferma deve fallire per il vincolo di unicita violato")
                .hasStackTraceContaining("uk_movimento_trasferimento_tipo");

        assertThat(statoDi(richiesto.id())).isEqualTo(StatoTrasferimento.IN_ATTESA);
        assertThat(saldoDi(origine.getId())).isEqualByComparingTo("1000.00");
        assertThat(saldoDi(destinazione.getId())).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Due conferme in parallelo eseguono il trasferimento una volta sola")
    void dueConfermeInParallelo() throws Exception {
        Utente cliente = cliente("cliente.concorrenza@prova.it");
        Conto origine = conto(cliente, "IT00X0000000000000000000011", "100.00");
        Conto destinazione = conto(cliente, "IT00X0000000000000000000012", "100.00");

        TrasferimentoDto richiesto = servizioTrasferimenti.richiedi(new RichiestaTrasferimento(
                origine.getId(), destinazione.getId(), new BigDecimal("30.00"), "Prova di concorrenza"),
                cliente.getId());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch via = new CountDownLatch(1);
        Callable<String> tentativo = () -> {
            via.await();
            try {
                return servizioTrasferimenti.conferma(richiesto.id(), cliente.getId()).stato();
            } catch (RuntimeException eccezione) {
                return eccezione.getClass().getSimpleName();
            }
        };

        Future<String> primo = pool.submit(tentativo);
        Future<String> secondo = pool.submit(tentativo);
        via.countDown();
        List<String> esiti = List.of(primo.get(30, TimeUnit.SECONDS), secondo.get(30, TimeUnit.SECONDS));
        pool.shutdown();

        // Il blocco sulla riga del trasferimento mette in fila le due conferme: la seconda
        // trova lo stato COMPLETATO e non esegue niente.
        assertThat(esiti).containsExactlyInAnyOrder("COMPLETATO", "TrasferimentoNonInAttesaException");
        assertThat(saldoDi(origine.getId())).isEqualByComparingTo("70.00");
        assertThat(saldoDi(destinazione.getId())).isEqualByComparingTo("130.00");

        // Due movimenti in tutto sul conto di origine: il saldo iniziale e l'uscita.
        assertThat(movimentoRepository.findByContoIdOrderByDataMovimentoDesc(origine.getId())).hasSize(2);
    }

    // ------------------------------------------------------------------
    // Aiuti
    // ------------------------------------------------------------------

    private Utente cliente(String email) {
        return utenteRepository.saveAndFlush(DatiDiProva.utente(email));
    }

    /** Un conto nuovo, con il movimento di saldo iniziale che ne giustifica il saldo. */
    private Conto conto(Utente titolare, String codiceConto, String saldoIniziale) {
        Conto conto = contoRepository.saveAndFlush(new Conto(codiceConto, titolare, Valuta.EUR));
        BigDecimal importo = new BigDecimal(saldoIniziale);

        conto.setSaldo(importo);
        movimentoRepository.saveAndFlush(new Movimento(conto, LocalDateTime.now(),
                "Saldo iniziale di prova", TipoMovimento.SALDO_INIZIALE, importo));
        return contoRepository.saveAndFlush(conto);
    }

    private StatoTrasferimento statoDi(Long trasferimentoId) {
        return trasferimentoRepository.findById(trasferimentoId).orElseThrow().getStato();
    }

    private BigDecimal saldoDi(Long contoId) {
        return contoRepository.findById(contoId).orElseThrow().getSaldo();
    }
}
