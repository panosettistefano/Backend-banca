package it.appbancaria.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import it.appbancaria.model.Utente;

/**
 * Dati del cliente esposti dall'API.
 *
 * <p>Solo i campi che l'interfaccia mostra. {@code passwordHash} resta nell'entita e non
 * ha modo di uscire dal server: e il motivo per cui esiste questo DTO invece di
 * serializzare direttamente {@link Utente}.
 */
@Schema(description = "Dati essenziali del cliente autenticato")
public record UtenteDto(
        @Schema(description = "Chiave del cliente", example = "1")
        Long id,

        @Schema(description = "Nome", example = "Mario")
        String nome,

        @Schema(description = "Cognome", example = "Rossi")
        String cognome,

        @Schema(description = "Email di accesso", example = "mario.rossi@demo.it")
        String email,

        @Schema(description = "Ruolo del cliente", example = "CLIENTE", allowableValues = {"CLIENTE", "ADMIN"})
        String ruolo) {

    /** Costruisce il DTO a partire dall'entita. */
    public static UtenteDto da(Utente utente) {
        return new UtenteDto(utente.getId(), utente.getNome(), utente.getCognome(),
                utente.getEmail(), utente.getRuolo().name());
    }
}
