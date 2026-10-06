package it.appbancaria.repository;

import it.appbancaria.DatiDiProva;
import it.appbancaria.TestcontainersConfiguration;
import it.appbancaria.Vincoli;
import it.appbancaria.model.Conto;
import it.appbancaria.model.Movimento;
import it.appbancaria.model.StatoTrasferimento;
import it.appbancaria.model.TipoMovimento;
import it.appbancaria.model.Trasferimento;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ricerche e vincoli dei trasferimenti.
 *
 * <p>I vincoli verificati qui non sono un dettaglio burocratico: sono le regole che
 * impediscono di registrare un trasferimento verso se stesso, con importo nullo o in uno
 * stato incoerente con la data di conferma. La logica applicativa li rispettera, ma il
 * database e l'ultima rete di sicurezza.
 */
@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(TestcontainersConfiguration.class)
class TrasferimentoRepositoryTest {

    @Autowired
    private TrasferimentoRepository trasferimentoRepository;

    @Autowired
    private MovimentoRepository movimentoRepository;

    @Autowired
    private ContoRepository contoRepository;

    @Autowired
    private UtenteRepository utenteRepository;

    private Utente cliente;
    private Conto contoOrigine;
    private Conto contoDestinazione;

    /** Un cliente con due conti, piu un secondo cliente con un conto solo. */
    @BeforeEach
    void preparaConti() {
        cliente = utenteRepository.save(DatiDiProva.utente("trasferimenti@prova.it"));
        contoOrigine = contoRepository.save(DatiDiProva.conto(cliente, "IT00X0000000000000000000011", "1000.00"));
        contoDestinazione = contoRepository.save(DatiDiProva.conto(cliente, "IT00X0000000000000000000012"));
    }

    @Test
    @DisplayName("Un trasferimento verso il conto di partenza viene rifiutato")
    void rifiutaContiUguali() {
        Trasferimento versoSeStesso = nuovoTrasferimento(cliente, contoOrigine, contoOrigine, "10.00");

        Vincoli.rifiutatoDalDatabase(() -> trasferimentoRepository.saveAndFlush(versoSeStesso),
                "chk_trasferimento_conti");
    }

    @Test
    @DisplayName("Un trasferimento di importo non positivo viene rifiutato")
    void rifiutaImportoNonPositivo() {
        Trasferimento importoZero = nuovoTrasferimento(cliente, contoOrigine, contoDestinazione, "0.00");

        Vincoli.rifiutatoDalDatabase(() -> trasferimentoRepository.saveAndFlush(importoZero),
                "chk_trasferimento_importo");
    }

    @Test
    @DisplayName("Un trasferimento COMPLETATO senza data di conferma viene rifiutato")
    void rifiutaCompletatoSenzaDataDiConferma() {
        Trasferimento completatoSenzaData = nuovoTrasferimento(cliente, contoOrigine, contoDestinazione, "10.00");
        completatoSenzaData.setStato(StatoTrasferimento.COMPLETATO);

        Vincoli.rifiutatoDalDatabase(() -> trasferimentoRepository.saveAndFlush(completatoSenzaData),
                "chk_trasferimento_conferma");
    }

    @Test
    @DisplayName("Un trasferimento in attesa non puo avere una data di conferma")
    void rifiutaDataDiConfermaSuUnTrasferimentoInAttesa() {
        Trasferimento inAttesaMaConfermato = nuovoTrasferimento(cliente, contoOrigine, contoDestinazione, "10.00");
        inAttesaMaConfermato.setConfermatoIl(LocalDateTime.now());

        Vincoli.rifiutatoDalDatabase(() -> trasferimentoRepository.saveAndFlush(inAttesaMaConfermato),
                "chk_trasferimento_conferma");
    }

    @Test
    @DisplayName("Un motivo di rifiuto su un trasferimento non rifiutato viene rifiutato")
    void rifiutaMotivoSuUnTrasferimentoInAttesa() {
        Trasferimento conMotivo = nuovoTrasferimento(cliente, contoOrigine, contoDestinazione, "10.00");
        conMotivo.setMotivoRifiuto("Motivo che non dovrebbe esistere");

        Vincoli.rifiutatoDalDatabase(() -> trasferimentoRepository.saveAndFlush(conMotivo),
                "chk_trasferimento_motivo");
    }

    @Test
    @DisplayName("Un trasferimento rifiutato e valido solo con il motivo di rifiuto")
    void trasferimentoRifiutatoCoerente() {
        Trasferimento rifiutato = nuovoTrasferimento(cliente, contoOrigine, contoDestinazione, "10.00");
        rifiutato.setStato(StatoTrasferimento.RIFIUTATO);
        rifiutato.setMotivoRifiuto("Saldo insufficiente");

        Trasferimento salvato = trasferimentoRepository.saveAndFlush(rifiutato);

        assertThat(salvato.getId()).isNotNull();
        assertThat(salvato.getStato()).isEqualTo(StatoTrasferimento.RIFIUTATO);
        assertThat(salvato.getConfermatoIl()).isNull();
    }

    @Test
    @DisplayName("Due trasferimenti non possono avere lo stesso riferimento")
    void rifiutaRiferimentoDuplicato() {
        Trasferimento primo = nuovoTrasferimento(cliente, contoOrigine, contoDestinazione, "10.00");
        trasferimentoRepository.saveAndFlush(primo);

        Trasferimento conLoStessoRiferimento = new Trasferimento(primo.getRiferimento(), cliente,
                contoOrigine, contoDestinazione, new BigDecimal("20.00"), null);

        Vincoli.rifiutatoDalDatabase(() -> trasferimentoRepository.saveAndFlush(conLoStessoRiferimento),
                "uk_trasferimento_riferimento");
    }

    @Test
    @DisplayName("Un trasferimento nasce IN_ATTESA, senza data di conferma e con la valuta del conto")
    void nuovoTrasferimentoNasceInAttesa() {
        Trasferimento salvato = trasferimentoRepository
                .saveAndFlush(nuovoTrasferimento(cliente, contoOrigine, contoDestinazione, "123.45"));

        assertThat(salvato.getStato()).isEqualTo(StatoTrasferimento.IN_ATTESA);
        assertThat(salvato.getConfermatoIl()).isNull();
        assertThat(salvato.getMotivoRifiuto()).isNull();
        assertThat(salvato.getValuta()).isEqualTo(contoOrigine.getValuta());
        assertThat(salvato.getImporto()).isEqualByComparingTo("123.45");
        assertThat(salvato.getCreatoIl()).isNotNull();
    }

    @Test
    @DisplayName("Si ritrova un trasferimento dal suo riferimento pubblico")
    void trovaPerRiferimento() {
        Trasferimento salvato = trasferimentoRepository
                .saveAndFlush(nuovoTrasferimento(cliente, contoOrigine, contoDestinazione, "10.00"));

        assertThat(trasferimentoRepository.findByRiferimento(salvato.getRiferimento())).isPresent();
        assertThat(trasferimentoRepository.findByRiferimento(UUID.randomUUID().toString())).isEmpty();
    }

    @Test
    @DisplayName("L'elenco per stato contiene solo i trasferimenti di quel cliente in quello stato")
    void elencoPerClienteEStato() {
        Trasferimento inAttesa = trasferimentoRepository
                .saveAndFlush(nuovoTrasferimento(cliente, contoOrigine, contoDestinazione, "10.00"));

        Trasferimento completato = nuovoTrasferimento(cliente, contoOrigine, contoDestinazione, "20.00");
        completato.setStato(StatoTrasferimento.COMPLETATO);
        completato.setConfermatoIl(LocalDateTime.now());
        trasferimentoRepository.saveAndFlush(completato);

        Utente altroCliente = utenteRepository.save(DatiDiProva.utente("altro.cliente@prova.it"));
        Conto contoAltrui = contoRepository.save(
                DatiDiProva.conto(altroCliente, "IT00X0000000000000000000013", "500.00"));
        Conto secondoContoAltrui = contoRepository.save(
                DatiDiProva.conto(altroCliente, "IT00X0000000000000000000014"));
        trasferimentoRepository.saveAndFlush(
                nuovoTrasferimento(altroCliente, contoAltrui, secondoContoAltrui, "30.00"));

        List<Trasferimento> daConfermare =
                trasferimentoRepository.findByUtenteIdAndStatoOrderByCreatoIlDesc(
                        cliente.getId(), StatoTrasferimento.IN_ATTESA);

        assertThat(daConfermare).extracting(Trasferimento::getId).containsExactly(inAttesa.getId());
        assertThat(daConfermare).extracting(t -> t.getUtente().getId()).containsOnly(cliente.getId());
    }

    @Test
    @DisplayName("Uno stesso trasferimento non puo generare due movimenti dello stesso tipo")
    void rifiutaDueMovimentiDelloStessoTipoPerLoStessoTrasferimento() {
        Trasferimento trasferimento = trasferimentoRepository
                .saveAndFlush(nuovoTrasferimento(cliente, contoOrigine, contoDestinazione, "50.00"));

        Movimento uscita = DatiDiProva.movimento(contoOrigine, LocalDateTime.now(),
                TipoMovimento.TRASFERIMENTO_USCITA, "50.00");
        uscita.setTrasferimento(trasferimento);
        movimentoRepository.saveAndFlush(uscita);

        Movimento secondaUscita = DatiDiProva.movimento(contoOrigine, LocalDateTime.now(),
                TipoMovimento.TRASFERIMENTO_USCITA, "50.00");
        secondaUscita.setTrasferimento(trasferimento);

        Vincoli.rifiutatoDalDatabase(() -> movimentoRepository.saveAndFlush(secondaUscita),
                "uk_movimento_trasferimento_tipo");
    }

    private Trasferimento nuovoTrasferimento(Utente utente, Conto origine, Conto destinazione, String importo) {
        return new Trasferimento(UUID.randomUUID().toString(), utente, origine, destinazione,
                new BigDecimal(importo), "Causale di prova");
    }
}
