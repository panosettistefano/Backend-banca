package it.appbancaria.web;

import it.appbancaria.model.StatoTrasferimento;
import it.appbancaria.security.UtenteAutenticato;
import it.appbancaria.service.ContoNonTrovatoException;
import it.appbancaria.service.RichiestaNonValidaException;
import it.appbancaria.service.SaldoInsufficienteException;
import it.appbancaria.service.ServizioTrasferimenti;
import it.appbancaria.service.TrasferimentoNonInAttesaException;
import it.appbancaria.service.TrasferimentoNonTrovatoException;
import it.appbancaria.web.dto.ErroreRisposta;
import it.appbancaria.web.dto.RichiestaTrasferimento;
import it.appbancaria.web.dto.TrasferimentoDto;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Trasferimenti simulati: richiesta, conferma ed elenco di quelli da confermare.
 *
 * <p>Il cliente arriva dal token ({@link AuthenticationPrincipal}), mai da un parametro
 * della richiesta: si possono solo richiedere e confermare trasferimenti propri.
 *
 * <p>I gestori degli errori stanno ancora qui, controller per controller: il gestore unico
 * per tutta l'API arriva allo STEP 6.
 */
@RestController
@RequestMapping("/api/transfers")
public class TrasferimentiController {

    private final ServizioTrasferimenti servizioTrasferimenti;

    public TrasferimentiController(ServizioTrasferimenti servizioTrasferimenti) {
        this.servizioTrasferimenti = servizioTrasferimenti;
    }

    /** Registra la richiesta: 201 con il trasferimento in attesa, nessun saldo toccato. */
    @PostMapping
    public ResponseEntity<TrasferimentoDto> richiedi(@Valid @RequestBody RichiestaTrasferimento richiesta,
                                                     @AuthenticationPrincipal UtenteAutenticato cliente) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(servizioTrasferimenti.richiedi(richiesta, cliente.id()));
    }

    /**
     * Esegue il trasferimento. Se la conferma trova il saldo insufficiente il
     * trasferimento viene rifiutato (e registrato come tale) e la risposta e 409, come lo
     * e la conferma ripetuta di un trasferimento gia chiuso.
     */
    @PostMapping("/{id}/confirm")
    public ResponseEntity<Object> conferma(@PathVariable Long id,
                                           @AuthenticationPrincipal UtenteAutenticato cliente) {
        TrasferimentoDto esito = servizioTrasferimenti.conferma(id, cliente.id());

        if (esito.rifiutato()) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(new ErroreRisposta(esito.motivoRifiuto()));
        }
        return ResponseEntity.ok(esito);
    }

    /**
     * Trasferimenti del cliente, per stato: senza parametro quelli ancora da confermare.
     * Uno stato che non esiste produce 400 (conversione predefinita di Spring).
     */
    @GetMapping
    public List<TrasferimentoDto> elenco(
            @RequestParam(required = false, defaultValue = "IN_ATTESA") StatoTrasferimento stato,
            @AuthenticationPrincipal UtenteAutenticato cliente) {
        return servizioTrasferimenti.elenco(cliente.id(), stato);
    }

    /** Conto o trasferimento inesistente, oppure di un altro cliente: sempre 404. */
    @ExceptionHandler({ContoNonTrovatoException.class, TrasferimentoNonTrovatoException.class})
    public ResponseEntity<ErroreRisposta> nonTrovato(RuntimeException eccezione) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroreRisposta(eccezione.getMessage()));
    }

    /** Saldo insufficiente o trasferimento gia chiuso: 409. */
    @ExceptionHandler({SaldoInsufficienteException.class, TrasferimentoNonInAttesaException.class})
    public ResponseEntity<ErroreRisposta> conflitto(RuntimeException eccezione) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroreRisposta(eccezione.getMessage()));
    }

    /** Regole di dominio violate: 400. */
    @ExceptionHandler(RichiestaNonValidaException.class)
    public ResponseEntity<ErroreRisposta> richiestaNonValida(RichiestaNonValidaException eccezione) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErroreRisposta(eccezione.getMessage()));
    }
}
