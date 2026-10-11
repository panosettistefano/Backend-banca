package it.appbancaria.web;

import it.appbancaria.service.CredenzialiNonValideException;
import it.appbancaria.service.ServizioAutenticazione;
import it.appbancaria.web.dto.ErroreRisposta;
import it.appbancaria.web.dto.RichiestaLogin;
import it.appbancaria.web.dto.RispostaLogin;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint di autenticazione.
 *
 * <p>E l'unica rotta aperta dell'API insieme a health check e documentazione: e la porta
 * da cui si entra, quindi non puo pretendere un token.
 */
@RestController
@RequestMapping("/api/auth")
public class AutenticazioneController {

    private final ServizioAutenticazione servizioAutenticazione;

    public AutenticazioneController(ServizioAutenticazione servizioAutenticazione) {
        this.servizioAutenticazione = servizioAutenticazione;
    }

    /** Verifica le credenziali e restituisce il token; 401 se non sono corrette. */
    @PostMapping("/login")
    public RispostaLogin login(@Valid @RequestBody RichiestaLogin richiesta) {
        return servizioAutenticazione.autentica(richiesta);
    }

    /**
     * Credenziali errate: 401 con lo stesso messaggio sia per l'email sconosciuta sia per
     * la password sbagliata. La gestione resta locale al controller; un gestore unico per
     * tutta l'API arriva allo STEP 6.
     */
    @ExceptionHandler(CredenzialiNonValideException.class)
    public ResponseEntity<ErroreRisposta> credenzialiNonValide(CredenzialiNonValideException eccezione) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErroreRisposta(eccezione.getMessage()));
    }
}
