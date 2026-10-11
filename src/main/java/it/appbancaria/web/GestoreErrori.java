package it.appbancaria.web;

import it.appbancaria.service.ContoNonTrovatoException;
import it.appbancaria.service.CredenzialiNonValideException;
import it.appbancaria.service.RichiestaNonValidaException;
import it.appbancaria.service.SaldoInsufficienteException;
import it.appbancaria.service.TrasferimentoNonInAttesaException;
import it.appbancaria.service.TrasferimentoNonTrovatoException;
import it.appbancaria.web.dto.ErroreRisposta;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Gestore unico degli errori dell'API: da qui escono tutte le risposte di errore dei
 * controller, nella stessa forma ({@link ErroreRisposta}).
 *
 * <p>Prima ogni controller aveva i suoi gestori, con lo stesso codice ripetuto; ora c'e un
 * posto solo, e aggiungere un'eccezione significa aggiungere un metodo qui. Le eccezioni di
 * dominio portano il messaggio da mostrare all'utente; gli errori tecnici (corpo illeggibile,
 * parametro sbagliato, rotta inesistente) vengono tradotti in un messaggio comprensibile.
 *
 * <p>L'ultimo gestore prende qualunque eccezione non prevista: risponde 500 con un messaggio
 * generico, senza far uscire dettagli interni, e scrive l'eccezione vera nei log.
 *
 * <p>Le richieste fermate prima dei controller, quando il token manca o non e valido, non
 * passano di qui: le risponde {@code PuntoIngressoNonAutenticato}, con lo stesso corpo.
 */
@RestControllerAdvice
public class GestoreErrori {

    private static final Logger log = LoggerFactory.getLogger(GestoreErrori.class);

    /** Credenziali errate al login: 401. */
    @ExceptionHandler(CredenzialiNonValideException.class)
    public ResponseEntity<ErroreRisposta> credenzialiNonValide(CredenzialiNonValideException eccezione,
                                                               HttpServletRequest richiesta) {
        return risposta(HttpStatus.UNAUTHORIZED, eccezione.getMessage(), richiesta);
    }

    /** Conto o trasferimento inesistente, oppure di un altro cliente: 404. */
    @ExceptionHandler({ContoNonTrovatoException.class, TrasferimentoNonTrovatoException.class})
    public ResponseEntity<ErroreRisposta> nonTrovato(RuntimeException eccezione, HttpServletRequest richiesta) {
        return risposta(HttpStatus.NOT_FOUND, eccezione.getMessage(), richiesta);
    }

    /** Saldo insufficiente o trasferimento gia chiuso: 409. */
    @ExceptionHandler({SaldoInsufficienteException.class, TrasferimentoNonInAttesaException.class})
    public ResponseEntity<ErroreRisposta> conflitto(RuntimeException eccezione, HttpServletRequest richiesta) {
        return risposta(HttpStatus.CONFLICT, eccezione.getMessage(), richiesta);
    }

    /** Regole di dominio violate (conti coincidenti, valute diverse): 400. */
    @ExceptionHandler(RichiestaNonValidaException.class)
    public ResponseEntity<ErroreRisposta> richiestaNonValida(RichiestaNonValidaException eccezione,
                                                             HttpServletRequest richiesta) {
        return risposta(HttpStatus.BAD_REQUEST, eccezione.getMessage(), richiesta);
    }

    /** Validazione del corpo della richiesta: 400, con l'elenco dei campi che non vanno. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroreRisposta> corpoNonValido(MethodArgumentNotValidException eccezione,
                                                         HttpServletRequest richiesta) {
        List<String> dettagli = eccezione.getBindingResult().getFieldErrors().stream()
                .map(errore -> errore.getField() + ": " + errore.getDefaultMessage())
                .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErroreRisposta.conDettagli(HttpStatus.BAD_REQUEST, "Dati non validi",
                        richiesta.getRequestURI(), dettagli));
    }

    /** JSON scritto male o valore di tipo sbagliato nel corpo: 400. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroreRisposta> corpoIlleggibile(HttpMessageNotReadableException eccezione,
                                                           HttpServletRequest richiesta) {
        return risposta(HttpStatus.BAD_REQUEST, "Corpo della richiesta non leggibile", richiesta);
    }

    /** Parametro di query o di percorso del tipo sbagliato (es. una data non ISO): 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroreRisposta> parametroNonValido(MethodArgumentTypeMismatchException eccezione,
                                                             HttpServletRequest richiesta) {
        return risposta(HttpStatus.BAD_REQUEST, "Parametro non valido: " + eccezione.getName(), richiesta);
    }

    /** Parametro obbligatorio assente: 400. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroreRisposta> parametroMancante(MissingServletRequestParameterException eccezione,
                                                            HttpServletRequest richiesta) {
        return risposta(HttpStatus.BAD_REQUEST,
                "Parametro obbligatorio mancante: " + eccezione.getParameterName(), richiesta);
    }

    /** Rotta che non esiste: 404, con il corpo uniforme invece della pagina di errore. */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroreRisposta> rottaInesistente(NoResourceFoundException eccezione,
                                                           HttpServletRequest richiesta) {
        return risposta(HttpStatus.NOT_FOUND, "Risorsa non trovata", richiesta);
    }

    /** Metodo HTTP non previsto sulla rotta: 405. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroreRisposta> metodoNonAmmesso(HttpRequestMethodNotSupportedException eccezione,
                                                           HttpServletRequest richiesta) {
        return risposta(HttpStatus.METHOD_NOT_ALLOWED, "Metodo non ammesso", richiesta);
    }

    /** Qualunque altra eccezione: 500 con un messaggio generico, dettagli solo nei log. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroreRisposta> imprevisto(Exception eccezione, HttpServletRequest richiesta) {
        log.error("Errore non previsto su {} {}", richiesta.getMethod(), richiesta.getRequestURI(), eccezione);
        return risposta(HttpStatus.INTERNAL_SERVER_ERROR, "Errore interno del server", richiesta);
    }

    private ResponseEntity<ErroreRisposta> risposta(HttpStatus stato, String messaggio, HttpServletRequest richiesta) {
        return ResponseEntity.status(stato)
                .body(ErroreRisposta.di(stato, messaggio, richiesta.getRequestURI()));
    }
}
