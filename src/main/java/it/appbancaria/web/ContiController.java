package it.appbancaria.web;

import it.appbancaria.security.UtenteAutenticato;
import it.appbancaria.service.ContoNonTrovatoException;
import it.appbancaria.service.ServizioConti;
import it.appbancaria.web.dto.ContoDto;
import it.appbancaria.web.dto.ErroreRisposta;
import it.appbancaria.web.dto.MovimentoDto;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
 * della richiesta: non esiste modo di chiedere i conti di qualcun altro.
 */
@RestController
@RequestMapping("/api/accounts")
public class ContiController {

    private final ServizioConti servizioConti;

    public ContiController(ServizioConti servizioConti) {
        this.servizioConti = servizioConti;
    }

    /** I conti del cliente autenticato, dal piu vecchio al piu recente. */
    @GetMapping
    public List<ContoDto> conti(@AuthenticationPrincipal UtenteAutenticato cliente) {
        return servizioConti.contiDi(cliente.id());
    }

    /**
     * I movimenti di un conto del cliente autenticato, dal piu recente al meno recente.
     *
     * @param id  conto richiesto
     * @param da  primo giorno incluso, formato ISO {@code yyyy-MM-dd}, opzionale
     * @param a   ultimo giorno incluso, formato ISO {@code yyyy-MM-dd}, opzionale
     */
    @GetMapping("/{id}/movements")
    public List<MovimentoDto> movimenti(
            @PathVariable Long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate da,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate a,
            @AuthenticationPrincipal UtenteAutenticato cliente) {
        return servizioConti.movimentiDi(id, cliente.id(), da, a);
    }

    /**
     * Conto inesistente o di un altro cliente: 404, senza distinguere i due casi. Il
     * gestore unico per tutti gli errori dell'API arriva allo STEP 6.
     */
    @ExceptionHandler(ContoNonTrovatoException.class)
    public ResponseEntity<ErroreRisposta> contoNonTrovato(ContoNonTrovatoException eccezione) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErroreRisposta(eccezione.getMessage()));
    }
}
