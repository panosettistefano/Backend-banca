package it.appbancaria.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.appbancaria.ApiDiProva;
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
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Trasferimenti simulati: richiesta, conferma, elenco e isolamento tra clienti.
 *
 * <p>I conti usati sono quelli dei clienti demo, ma i saldi non sono mai dati per scontati:
 * ogni test li rilegge dall'API prima di agire, perche i test che confermano trasferimenti
 * spostano denaro vero (nel database di prova) e l'ordine di esecuzione non e garantito.
 *
 * <p>Le verifiche di atomicita piu fini (annullamento per vincolo del database e due
 * conferme in parallelo) stanno in {@code ServizioTrasferimentiTest}, che lavora con conti
 * creati apposta.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TrasferimentiControllerTest {

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
    @DisplayName("La richiesta registra il trasferimento in attesa senza muovere il denaro")
    void richiestaNonMuoveIlDenaro() throws Exception {
        long origine = contoDi(tokenMario, 0);
        long destinazione = contoDi(tokenMario, 1);
        BigDecimal saldoPrima = saldoDi(tokenMario, origine);

        JsonNode creato = richiedi(tokenMario, origine, destinazione, new BigDecimal("25.00"), "Prova demo");

        assertThat(creato.get("id").asLong()).isPositive();
        assertThat(creato.get("riferimento").asText()).isNotBlank();
        assertThat(creato.get("stato").asText()).isEqualTo("IN_ATTESA");
        assertThat(creato.get("importo").decimalValue()).isEqualByComparingTo("25.00");
        assertThat(creato.get("creatoIl").isNull()).isFalse();
        assertThat(creato.get("confermatoIl").isNull()).isTrue();
        assertThat(creato.get("motivoRifiuto").isNull()).isTrue();
        assertThat(creato.get("contoOrigine").get("id").asLong()).isEqualTo(origine);
        assertThat(creato.get("contoDestinazione").get("id").asLong()).isEqualTo(destinazione);

        assertThat(saldoDi(tokenMario, origine)).isEqualByComparingTo(saldoPrima);
    }

    @Test
    @DisplayName("La richiesta con saldo insufficiente risponde 409 e non registra nulla")
    void richiestaConSaldoInsufficiente() throws Exception {
        long origine = contoDi(tokenMario, 0);
        long destinazione = contoDi(tokenMario, 1);
        BigDecimal troppo = saldoDi(tokenMario, origine).add(new BigDecimal("1000.00"));

        int inAttesaPrima = inAttesa(tokenMario).size();

        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRichiesta(origine, destinazione, troppo, null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errore").value("Saldo insufficiente"));

        assertThat(inAttesa(tokenMario)).hasSize(inAttesaPrima);
    }

    @Test
    @DisplayName("Un importo nullo, negativo o con troppi decimali produce 400")
    void richiestaConImportoNonValido() throws Exception {
        long origine = contoDi(tokenMario, 0);
        long destinazione = contoDi(tokenMario, 1);

        for (Object importo : List.of(0, -5, new BigDecimal("10.999"), "dieci")) {
            mockMvc.perform(post("/api/transfers")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(corpoRichiesta(origine, destinazione, importo, null)))
                    .andExpect(status().isBadRequest());
        }

        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRichiesta(origine, destinazione, null, null)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Un trasferimento verso lo stesso conto produce 400")
    void richiestaConContiCoincidenti() throws Exception {
        long origine = contoDi(tokenMario, 0);

        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRichiesta(origine, origine, new BigDecimal("10.00"), null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errore").value("I due conti devono essere diversi"));
    }

    @Test
    @DisplayName("Un conto di un altro cliente produce 404, come un conto inesistente")
    void richiestaConContiDiUnAltroCliente() throws Exception {
        long mioConto = contoDi(tokenMario, 0);
        long contoDiGiulia = contoDi(tokenGiulia, 0);

        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRichiesta(contoDiGiulia, mioConto, new BigDecimal("10.00"), null)))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRichiesta(mioConto, contoDiGiulia, new BigDecimal("10.00"), null)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Un conto che non esiste produce 404")
    void richiestaConContoInesistente() throws Exception {
        mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRichiesta(999999L, contoDi(tokenMario, 0), new BigDecimal("10.00"), null)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errore").value("Conto non trovato"));
    }

    @Test
    @DisplayName("La conferma aggiorna i due saldi e registra i due movimenti collegati")
    void confermaEsegueIlTrasferimento() throws Exception {
        long origine = contoDi(tokenMario, 0);
        long destinazione = contoDi(tokenMario, 1);
        BigDecimal saldoOrigine = saldoDi(tokenMario, origine);
        BigDecimal saldoDestinazione = saldoDi(tokenMario, destinazione);
        BigDecimal importo = new BigDecimal("25.00");

        long id = richiedi(tokenMario, origine, destinazione, importo, "Regalo di prova").get("id").asLong();

        JsonNode esito = conferma(tokenMario, id);

        assertThat(esito.get("stato").asText()).isEqualTo("COMPLETATO");
        assertThat(esito.get("confermatoIl").isNull()).isFalse();
        assertThat(saldoDi(tokenMario, origine)).isEqualByComparingTo(saldoOrigine.subtract(importo));
        assertThat(saldoDi(tokenMario, destinazione)).isEqualByComparingTo(saldoDestinazione.add(importo));

        JsonNode uscita = movimentoPiuRecente(tokenMario, origine);
        assertThat(uscita.get("tipo").asText()).isEqualTo("TRASFERIMENTO_USCITA");
        assertThat(uscita.get("importoConSegno").decimalValue()).isEqualByComparingTo(importo.negate());
        assertThat(uscita.get("descrizione").asText())
                .startsWith("Trasferimento verso")
                .contains("Regalo di prova");

        JsonNode entrata = movimentoPiuRecente(tokenMario, destinazione);
        assertThat(entrata.get("tipo").asText()).isEqualTo("TRASFERIMENTO_ENTRATA");
        assertThat(entrata.get("importoConSegno").decimalValue()).isEqualByComparingTo(importo);
    }

    @Test
    @DisplayName("Una seconda conferma risponde 409 e non muove piu nulla")
    void confermaRipetutaNonMuoveNulla() throws Exception {
        long origine = contoDi(tokenMario, 0);
        long destinazione = contoDi(tokenMario, 1);
        long id = richiedi(tokenMario, origine, destinazione, new BigDecimal("15.00"), null).get("id").asLong();

        conferma(tokenMario, id);
        BigDecimal saldoDopoLaPrima = saldoDi(tokenMario, origine);

        mockMvc.perform(post("/api/transfers/" + id + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errore").value("Il trasferimento non e piu in attesa"));

        assertThat(saldoDi(tokenMario, origine)).isEqualByComparingTo(saldoDopoLaPrima);
    }

    @Test
    @DisplayName("Il trasferimento di un altro cliente non si puo confermare: 404")
    void confermaDiUnTrasferimentoAltrui() throws Exception {
        long id = richiedi(tokenGiulia, contoDi(tokenGiulia, 0), contoDi(tokenGiulia, 1),
                new BigDecimal("10.00"), null).get("id").asLong();

        mockMvc.perform(post("/api/transfers/" + id + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errore").value("Trasferimento non trovato"));

        // Per Giulia il trasferimento e ancora li, da confermare.
        assertThat(inAttesa(tokenGiulia)).extracting(riferimento -> riferimento.get("id").asLong())
                .contains(id);
    }

    @Test
    @DisplayName("La conferma di un trasferimento inesistente produce 404")
    void confermaDiUnTrasferimentoInesistente() throws Exception {
        mockMvc.perform(post("/api/transfers/999999/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errore").value("Trasferimento non trovato"));
    }

    @Test
    @DisplayName("Se il saldo non basta piu, la conferma rifiuta il trasferimento e non muove nulla")
    void confermaConSaldoNelFrattempoInsufficiente() throws Exception {
        long origine = contoDi(tokenMario, 0);
        long destinazione = contoDi(tokenMario, 1);

        // Due richieste da poco piu di meta saldo ciascuna: passano entrambe la validazione
        // della richiesta, ma la seconda conferma trovera il saldo ormai insufficiente.
        BigDecimal importo = saldoDi(tokenMario, origine)
                .multiply(new BigDecimal("0.6"))
                .setScale(2, RoundingMode.DOWN);

        long primoTrasferimento = richiedi(tokenMario, origine, destinazione, importo, null).get("id").asLong();
        long secondoTrasferimento = richiedi(tokenMario, origine, destinazione, importo, null).get("id").asLong();

        conferma(tokenMario, primoTrasferimento);
        BigDecimal saldoDopoIlPrimo = saldoDi(tokenMario, origine);

        mockMvc.perform(post("/api/transfers/" + secondoTrasferimento + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errore").value("Saldo insufficiente"));

        assertThat(saldoDi(tokenMario, origine)).isEqualByComparingTo(saldoDopoIlPrimo);

        // Il rifiuto resta scritto: il trasferimento non e piu in attesa e ha il motivo.
        JsonNode rifiutato = ApiDiProva.lista(mockMvc, objectMapper, tokenMario, "/api/transfers?stato=RIFIUTATO")
                .stream()
                .filter(trasferimento -> trasferimento.get("id").asLong() == secondoTrasferimento)
                .findFirst()
                .orElseThrow();
        assertThat(rifiutato.get("motivoRifiuto").asText()).isEqualTo("Saldo insufficiente");
        assertThat(rifiutato.get("confermatoIl").isNull()).isTrue();
    }

    @Test
    @DisplayName("L'elenco mostra solo i trasferimenti del cliente, filtrati per stato")
    void elencoPerStatoEIsolamento() throws Exception {
        long origine = contoDi(tokenMario, 0);
        long destinazione = contoDi(tokenMario, 1);
        long id = richiedi(tokenMario, origine, destinazione, new BigDecimal("5.00"), null).get("id").asLong();

        assertThat(inAttesa(tokenMario)).extracting(trasferimento -> trasferimento.get("id").asLong())
                .contains(id);
        assertThat(inAttesa(tokenGiulia)).extracting(trasferimento -> trasferimento.get("id").asLong())
                .doesNotContain(id);

        conferma(tokenMario, id);
        assertThat(inAttesa(tokenMario)).extracting(trasferimento -> trasferimento.get("id").asLong())
                .doesNotContain(id);

        mockMvc.perform(get("/api/transfers?stato=IN_ESISTENTE")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenMario))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/transfers"))
                .andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------
    // Aiuti
    // ------------------------------------------------------------------

    /** Crea un trasferimento e ne restituisce il JSON, pretendendo il 201. */
    private JsonNode richiedi(String token, long origine, long destinazione,
                              Object importo, String causale) throws Exception {
        String risposta = mockMvc.perform(post("/api/transfers")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpoRichiesta(origine, destinazione, importo, causale)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(risposta);
    }

    /** Conferma un trasferimento e ne restituisce il JSON, pretendendo il 200. */
    private JsonNode conferma(String token, long trasferimentoId) throws Exception {
        String risposta = mockMvc.perform(post("/api/transfers/" + trasferimentoId + "/confirm")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(risposta);
    }

    /** Corpo JSON della richiesta, scritto come lo manderebbe un client esterno. */
    private String corpoRichiesta(Long origine, Long destinazione, Object importo, String causale) throws Exception {
        Map<String, Object> corpo = new LinkedHashMap<>();
        corpo.put("contoOrigineId", origine);
        corpo.put("contoDestinazioneId", destinazione);
        corpo.put("importo", importo);
        if (causale != null) {
            corpo.put("causale", causale);
        }
        return objectMapper.writeValueAsString(corpo);
    }

    private List<JsonNode> conti(String token) throws Exception {
        return ApiDiProva.lista(mockMvc, objectMapper, token, "/api/accounts");
    }

    private long contoDi(String token, int posizione) throws Exception {
        return conti(token).get(posizione).get("id").asLong();
    }

    private BigDecimal saldoDi(String token, long contoId) throws Exception {
        return conti(token).stream()
                .filter(conto -> conto.get("id").asLong() == contoId)
                .findFirst()
                .orElseThrow()
                .get("saldo")
                .decimalValue();
    }

    private List<JsonNode> inAttesa(String token) throws Exception {
        return ApiDiProva.lista(mockMvc, objectMapper, token, "/api/transfers");
    }

    /** Il movimento in cima all'elenco, cioe il piu recente. */
    private JsonNode movimentoPiuRecente(String token, long contoId) throws Exception {
        return ApiDiProva.lista(mockMvc, objectMapper, token, "/api/accounts/" + contoId + "/movements").get(0);
    }
}
