package it.appbancaria.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.appbancaria.model.StatoTrasferimento;
import it.appbancaria.security.UtenteAutenticato;
import it.appbancaria.service.ServizioTrasferimenti;
import it.appbancaria.web.dto.ErroreRisposta;
import it.appbancaria.web.dto.RichiestaTrasferimento;
import it.appbancaria.web.dto.TrasferimentoDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Trasferimenti simulati: richiesta, conferma ed elenco per stato.
 *
 * <p>Il cliente arriva dal token ({@link AuthenticationPrincipal}), mai da un parametro
 * della richiesta: si possono solo richiedere e confermare trasferimenti propri. Gli
 * errori li converte in risposte JSON il {@link GestoreErrori}.
 */
@RestController
@RequestMapping("/api/transfers")
@Tag(name = "Trasferimenti")
public class TrasferimentiController {

    private final ServizioTrasferimenti servizioTrasferimenti;

    public TrasferimentiController(ServizioTrasferimenti servizioTrasferimenti) {
        this.servizioTrasferimenti = servizioTrasferimenti;
    }

    @Operation(summary = "Richiede un trasferimento fra due conti propri",
            description = "Registra la richiesta in stato IN_ATTESA: non muove ancora il denaro e non prenota "
                    + "fondi, controlla solo che il saldo basti in questo momento. L'esecuzione avviene con "
                    + "la conferma.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Trasferimento registrato in attesa"),
            @ApiResponse(responseCode = "400", description = "Dati non validi, conti coincidenti o valute diverse",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class))),
            @ApiResponse(responseCode = "401", description = "Token mancante o non valido",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class))),
            @ApiResponse(responseCode = "404", description = "Conto inesistente o di un altro cliente",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class))),
            @ApiResponse(responseCode = "409", description = "Saldo insufficiente",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class)))
    })
    @PostMapping
    public ResponseEntity<TrasferimentoDto> richiedi(@Valid @RequestBody RichiestaTrasferimento richiesta,
                                                     @AuthenticationPrincipal UtenteAutenticato cliente) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servizioTrasferimenti.richiedi(richiesta, cliente.id()));
    }

    @Operation(summary = "Conferma un trasferimento in attesa",
            description = "Esegue il trasferimento in una sola transazione: aggiorna i due saldi e registra i "
                    + "due movimenti collegati. Se alla conferma il saldo non basta piu, il trasferimento "
                    + "viene registrato come RIFIUTATO con il motivo e la risposta e 409; una seconda "
                    + "conferma di un trasferimento gia chiuso risponde 409 senza muovere nulla.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trasferimento completato: saldi e movimenti aggiornati"),
            @ApiResponse(responseCode = "401", description = "Token mancante o non valido",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class))),
            @ApiResponse(responseCode = "404", description = "Trasferimento inesistente o di un altro cliente",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class))),
            @ApiResponse(responseCode = "409", description = "Saldo insufficiente o trasferimento gia chiuso",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class)))
    })
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Object> conferma(
            @Parameter(description = "Chiave del trasferimento", example = "1")
            @PathVariable Long id,

            @AuthenticationPrincipal UtenteAutenticato cliente) {
        TrasferimentoDto esito = servizioTrasferimenti.conferma(id, cliente.id());

        if (esito.rifiutato()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ErroreRisposta.di(HttpStatus.CONFLICT, esito.motivoRifiuto(), "/api/transfers/" + id + "/confirm"));
        }
        return ResponseEntity.ok(esito);
    }

    @Operation(summary = "Elenca i trasferimenti del cliente per stato",
            description = "Senza il parametro stato elenca quelli ancora da confermare (IN_ATTESA). "
                    + "Uno stato che non esiste produce 400.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trasferimenti del cliente nello stato richiesto"),
            @ApiResponse(responseCode = "400", description = "Stato non riconosciuto",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class))),
            @ApiResponse(responseCode = "401", description = "Token mancante o non valido",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class)))
    })
    @GetMapping
    public List<TrasferimentoDto> elenco(
            @Parameter(description = "Stato da elencare", example = "IN_ATTESA")
            @RequestParam(required = false, defaultValue = "IN_ATTESA") StatoTrasferimento stato,

            @AuthenticationPrincipal UtenteAutenticato cliente) {
        return servizioTrasferimenti.elenco(cliente.id(), stato);
    }
}
