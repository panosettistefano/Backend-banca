package it.appbancaria.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Corpo della richiesta di login.
 *
 * <p>La {@code password} viaggia in chiaro solo dentro questa richiesta, in locale e su
 * HTTP di sviluppo; sul server viene confrontata con l'hash e non viene mai salvata ne
 * registrata nei log.
 *
 * @param email    email di accesso
 * @param password password in chiaro, da confrontare con l'hash BCrypt
 */
@Schema(description = "Credenziali di accesso del cliente demo")
public record RichiestaLogin(
        @Schema(description = "Email di accesso", example = "mario.rossi@demo.it")
        @NotBlank(message = "L'email e obbligatoria")
        @Email(message = "L'email non e valida")
        String email,

        @Schema(description = "Password del cliente demo", example = "Demo1234!")
        @NotBlank(message = "La password e obbligatoria")
        String password) {
}
