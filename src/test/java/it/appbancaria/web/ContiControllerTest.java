package it.appbancaria.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.appbancaria.TestcontainersConfiguration;
import it.appbancaria.TokenDiProva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Conti, movimenti e isolamento tra clienti, sul contesto completo e sul MySQL di
 * Testcontainers.
 *
 * <p>Ne gli id dei conti ne le date dei filtri sono scritti a mano. Gli id si ricavano da
 * un login vero e da {@code GET /api/accounts}; le date si ricavano dai movimenti stessi,
 * perche il seed le genera con {@code NOW(6)} al momento della migrazione: in un container
 * appena creato i movimenti coprono gli ultimi 90 giorni, quindi le stesse date assolute
 * non valgono sia per il database di sviluppo sia per quello dei test.
 *
 * <p>Il seed distribuisce 21 movimenti su circa 90 giorni: la data mediana e le date
 * estreme tagliano quindi davvero qualcosa, ed e quello che i test verificano.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ContiControllerTest {

    private static final Set<String> TIPI_ENTRATA =
            Set.of("SALDO_INIZIALE", "VERSAMENTO", "TRASFERIMENTO_ENTRATA");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String tokenMario;
    private String tokenGiulia;

    @BeforeEach
    void autenticaIClienti() throws Exception {
        tokenMario = TokenDiProva.diMario(mockMvc, objectMapper);
        tokenGiulia = TokenDiProva.diGiulia(mockMvc, objectMapper);
    }

    @Test
    @DisplayName("Il cliente vede i suoi conti, in euro e con saldo positivo")
    void contiDelCliente() throws Exception {
        List<JsonNode> conti = conti(tokenMario);

        assertThat(conti).isNotEmpty();
        assertThat(conti).allSatisfy(conto -> {
            assertThat(conto.get("codiceConto").asText()).isNotBlank();
            assertThat(conto.get("valuta").asText()).isEqualTo("EUR");
            assertThat(conto.get("saldo").decimalValue()).isPositive();
        });
    }

    @Test
    @DisplayName("L'elenco dei conti e solo del cliente autenticato")
    void elenchiSeparatiTraClienti() throws Exception {
        List<Long> contiDiMario = idDeiConti(conti(tokenMario));
        List<Long> contiDiGiulia = idDeiConti(conti(tokenGiulia));

        assertThat(contiDiMario).isNotEmpty();
        assertThat(contiDiGiulia).isNotEmpty();
        assertThat(contiDiMario).doesNotContainAnyElementsOf(contiDiGiulia);
    }

    @Test
    @DisplayName("Il conto di un altro cliente risponde 404, come un conto inesistente")
    void contoDiUnAltroCliente() throws Exception {
        long contoDiGiulia = idDeiConti(conti(tokenGiulia)).get(0);

        mockMvc.perform(get("/api/accounts/" + contoDiGiulia + "/movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.errore").value("Conto non trovato"));
    }

    @Test
    @DisplayName("Un conto che non esiste risponde 404 con lo stesso corpo")
    void contoInesistente() throws Exception {
        mockMvc.perform(get("/api/accounts/999999/movements")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errore").value("Conto non trovato"));
    }

    @Test
    @DisplayName("I movimenti arrivano dal piu recente e con l'importo col segno giusto")
    void movimentiOrdinatiEColSegno() throws Exception {
        List<JsonNode> movimenti = movimenti(tokenMario, contoDiMario(), "");

        assertThat(movimenti).isNotEmpty();
        assertThat(dateDi(movimenti)).isSortedAccordingTo(Comparator.reverseOrder());

        assertThat(movimenti).allSatisfy(movimento -> {
            assertThat(movimento.get("importo").decimalValue()).isPositive();
            assertThat(movimento.get("importoConSegno").decimalValue())
                    .isEqualByComparingTo(importoColSegno(movimento));
        });
    }

    @Test
    @DisplayName("Il filtro da tiene solo i movimenti dal giorno indicato in avanti")
    void filtroDallaDataIniziale() throws Exception {
        long conto = contoDiMario();
        List<JsonNode> completi = movimenti(tokenMario, conto, "");
        LocalDate ultimoGiorno = dataPiuRecente(completi).toLocalDate();

        List<JsonNode> filtrati = movimenti(tokenMario, conto, "?da=" + ultimoGiorno);

        assertThat(filtrati).isNotEmpty().hasSizeLessThan(completi.size());
        assertThat(dateDi(filtrati)).allMatch(data -> !data.toLocalDate().isBefore(ultimoGiorno));
    }

    @Test
    @DisplayName("Il filtro a tiene solo i movimenti fino al giorno indicato")
    void filtroDataFinale() throws Exception {
        long conto = contoDiMario();
        List<JsonNode> completi = movimenti(tokenMario, conto, "");
        LocalDate giornoMediano = dataMediana(completi).toLocalDate();

        List<JsonNode> filtrati = movimenti(tokenMario, conto, "?a=" + giornoMediano);

        assertThat(filtrati).isNotEmpty().hasSizeLessThan(completi.size());
        assertThat(dateDi(filtrati)).allMatch(data -> !data.toLocalDate().isAfter(giornoMediano));
    }

    @Test
    @DisplayName("La data finale comprende tutta la giornata, non solo la mezzanotte")
    void dataFinaleInclusaFinoAFineGiornata() throws Exception {
        long conto = contoDiMario();
        List<JsonNode> completi = movimenti(tokenMario, conto, "");
        LocalDate primoGiorno = dataPiuVecchia(completi).toLocalDate();

        // Il movimento piu vecchio non e a mezzanotte: se "a" fermasse la giornata alle
        // 00:00, questo filtro restituirebbe una lista vuota.
        List<JsonNode> filtrati = movimenti(tokenMario, conto, "?a=" + primoGiorno);

        assertThat(filtrati).isNotEmpty();
        assertThat(dateDi(filtrati)).allMatch(data -> data.toLocalDate().equals(primoGiorno));
    }

    @Test
    @DisplayName("Il filtro con entrambe le date restringe ai soli movimenti di quei giorni")
    void filtroConEntrambeLeDate() throws Exception {
        long conto = contoDiMario();
        List<JsonNode> completi = movimenti(tokenMario, conto, "");
        LocalDate giornoMediano = dataMediana(completi).toLocalDate();

        // Intervallo che copre l'estremo piu vecchio e il piu recente: la lista completa.
        List<JsonNode> tuttoIlPeriodo = movimenti(tokenMario, conto,
                "?da=" + dataPiuVecchia(completi).toLocalDate()
                        + "&a=" + dataPiuRecente(completi).toLocalDate());
        assertThat(tuttoIlPeriodo).hasSameSizeAs(completi);

        // Un solo giorno, come inizio e come fine: restano i movimenti di quel giorno.
        List<JsonNode> soloDa = movimenti(tokenMario, conto, "?da=" + giornoMediano);
        List<JsonNode> soloA = movimenti(tokenMario, conto, "?a=" + giornoMediano);
        List<JsonNode> unGiornoSolo = movimenti(tokenMario, conto,
                "?da=" + giornoMediano + "&a=" + giornoMediano);

        assertThat(unGiornoSolo).isNotEmpty();
        assertThat(unGiornoSolo.size()).isLessThanOrEqualTo(Math.min(soloDa.size(), soloA.size()));
        assertThat(dateDi(unGiornoSolo)).allMatch(data -> data.toLocalDate().equals(giornoMediano));
    }

    @Test
    @DisplayName("Una data iniziale successiva a quella finale non e un errore: lista vuota")
    void intervalloRovesciatoListaVuota() throws Exception {
        long conto = contoDiMario();
        List<JsonNode> completi = movimenti(tokenMario, conto, "");

        List<JsonNode> movimenti = movimenti(tokenMario, conto,
                "?da=" + dataPiuRecente(completi).toLocalDate().plusDays(1)
                        + "&a=" + dataPiuVecchia(completi).toLocalDate());

        assertThat(movimenti).isEmpty();
    }

    @Test
    @DisplayName("Una data malformata produce 400")
    void dataMalformata() throws Exception {
        mockMvc.perform(get("/api/accounts/" + contoDiMario() + "/movements?da=non-una-data")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario))
                .andExpect(status().isBadRequest());
    }

    // ------------------------------------------------------------------
    // Aiuti
    // ------------------------------------------------------------------

    private List<JsonNode> conti(String token) throws Exception {
        String corpo = mockMvc.perform(get("/api/accounts")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return leggiLista(corpo);
    }

    /** Movimenti di un conto; {@code query} e la parte dopo il punto interrogativo, vuota se non serve. */
    private List<JsonNode> movimenti(String token, long contoId, String query) throws Exception {
        String corpo = mockMvc.perform(get("/api/accounts/" + contoId + "/movements" + query)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return leggiLista(corpo);
    }

    private List<JsonNode> leggiLista(String json) throws Exception {
        List<JsonNode> righe = new ArrayList<>();
        objectMapper.readTree(json).forEach(righe::add);
        return righe;
    }

    private List<Long> idDeiConti(List<JsonNode> conti) {
        return conti.stream().map(conto -> conto.get("id").asLong()).toList();
    }

    private List<LocalDateTime> dateDi(List<JsonNode> movimenti) {
        return movimenti.stream()
                .map(movimento -> LocalDateTime.parse(movimento.get("dataMovimento").asText()))
                .toList();
    }

    private LocalDateTime dataPiuVecchia(List<JsonNode> movimenti) {
        return dateDi(movimenti).stream().min(Comparator.naturalOrder()).orElseThrow();
    }

    private LocalDateTime dataPiuRecente(List<JsonNode> movimenti) {
        return dateDi(movimenti).stream().max(Comparator.naturalOrder()).orElseThrow();
    }

    /** La data del movimento che sta a meta elenco: con l'elenco ordinato dal piu recente, divide il periodo. */
    private LocalDateTime dataMediana(List<JsonNode> movimenti) {
        List<LocalDateTime> date = dateDi(movimenti);
        return date.get(date.size() / 2);
    }

    /** Il conto di Mario usato dai test sui filtri: il primo del suo elenco. */
    private long contoDiMario() throws Exception {
        return idDeiConti(conti(tokenMario)).get(0);
    }

    /** L'importo col segno atteso, ricavato dal tipo del movimento. */
    private BigDecimal importoColSegno(JsonNode movimento) {
        BigDecimal importo = movimento.get("importo").decimalValue();
        return TIPI_ENTRATA.contains(movimento.get("tipo").asText()) ? importo : importo.negate();
    }
}
