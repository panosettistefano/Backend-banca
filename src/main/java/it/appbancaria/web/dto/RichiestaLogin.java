package it.appbancaria.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Corpo della richiesta di login.
 *
 * <p>La {@code password} viaggia in chiaro solo dentro questa richiesta, in locale e su
 * HTTP di sviluppo; sul server viene confrontata con l'hash e non viene mai salvata ne
 * registrata nei log.
 *
 * <p>Un corpo non valido produce il 400 predefinito di Spring: la gestione uniforme degli
 * errori arriva allo STEP 6.
 *
 * @param email    email di accesso
 * @param password password in chiaro, da confrontare con l'hash BCrypt
 */
public record RichiestaLogin(
        @NotBlank(message = "L'email e obbligatoria")
        @Email(message = "L'email non e valida")
        String email,

        @NotBlank(message = "La password e obbligatoria")
        String password) {
}
