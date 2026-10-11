package it.appbancaria.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.appbancaria.security.UtenteAutenticato;
import it.appbancaria.service.ServizioConti;
import it.appbancaria.web.dto.ContoDto;
import it.appbancaria.web.dto.ErroreRisposta;
import it.appbancaria.web.dto.MovimentoDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Conti e movimenti del cliente autenticato.
 *
 * <p>Il cliente arriva dal token ({@link AuthenticationPrincipal}), mai da un parametro
 * della richiesta: non esiste modo di chiedere i conti di qualcun altro. Gli errori li
 * converte in risposte JSON il {@link GestoreErrori}.
 */
@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Conti e movimenti")
public class ContiController {

    private final ServizioConti servizioConti;

    public ContiController(ServizioConti servizioConti) {
        this.servizioConti = servizioConti;
    }

    @Operation(summary = "Elenca i conti del cliente autenticato",
            description = "Restituisce i conti del cliente del token, dal piu vecchio al piu recente. "
                    + "Il campo intestatario non compare: ogni conto restituito e gia suo.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Conti del cliente"),
            @ApiResponse(responseCode = "401", description = "Token mancante o non valido",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class)))
    })
    @GetMapping
    public List<ContoDto> conti(@AuthenticationPrincipal UtenteAutenticato cliente) {
        return servizioConti.contiDi(cliente.id());
    }

    @Operation(summary = "Elenca i movimenti di un conto",
            description = "Movimenti del conto indicato, dal piu recente al meno recente. I filtri da e a "
                    + "sono giorni interi e opzionali: da comprende tutta la giornata indicata, a pure. "
                    + "Un conto di un altro cliente risponde 404 come un conto inesistente.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Movimenti del conto, dal piu recente"),
            @ApiResponse(responseCode = "400", description = "Data non in formato ISO (yyyy-MM-dd)",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class))),
            @ApiResponse(responseCode = "401", description = "Token mancante o non valido",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class))),
            @ApiResponse(responseCode = "404", description = "Conto inesistente o di un altro cliente",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class)))
    })
    @GetMapping("/{id}/movements")
    public List<MovimentoDto> movimenti(
            @Parameter(description = "Chiave del conto", example = "3")
            @PathVariable Long id,

            @Parameter(description = "Primo giorno incluso, formato yyyy-MM-dd", example = "2026-09-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate da,

            @Parameter(description = "Ultimo giorno incluso, formato yyyy-MM-dd", example = "2026-09-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate a,

            @AuthenticationPrincipal UtenteAutenticato cliente) {
        return servizioConti.movimentiDi(id, cliente.id(), da, a);
    }
}
