package it.appbancaria.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.appbancaria.service.ServizioAutenticazione;
import it.appbancaria.web.dto.ErroreRisposta;
import it.appbancaria.web.dto.RichiestaLogin;
import it.appbancaria.web.dto.RispostaLogin;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint di autenticazione.
 *
 * <p>E l'unica rotta aperta dell'API insieme a health check e documentazione: e la porta
 * da cui si entra, quindi non puo pretendere un token. Gli errori li converte in risposte
 * JSON il {@link GestoreErrori}.
 */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticazione")
public class AutenticazioneController {

    private final ServizioAutenticazione servizioAutenticazione;

    public AutenticazioneController(ServizioAutenticazione servizioAutenticazione) {
        this.servizioAutenticazione = servizioAutenticazione;
    }

    @Operation(summary = "Autentica il cliente demo",
            description = "Verifica email e password e restituisce il token JWT (valido 2 ore) da usare "
                    + "come Bearer nelle altre chiamate. Email sconosciuta e password errata danno la "
                    + "stessa risposta: non si scopre quali email esistono.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credenziali corrette: token e dati del cliente"),
            @ApiResponse(responseCode = "400", description = "Email o password assenti, o email non valida",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class))),
            @ApiResponse(responseCode = "401", description = "Credenziali errate",
                    content = @Content(schema = @Schema(implementation = ErroreRisposta.class)))
    })
    @SecurityRequirements
    @PostMapping("/login")
    public RispostaLogin login(@Valid @RequestBody RichiestaLogin richiesta) {
        return servizioAutenticazione.autentica(richiesta);
    }
}
