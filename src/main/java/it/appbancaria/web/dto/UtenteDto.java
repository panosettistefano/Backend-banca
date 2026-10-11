package it.appbancaria.web.dto;

import it.appbancaria.model.Utente;

/**
 * Dati del cliente esposti dall'API.
 *
 * <p>Solo i campi che l'interfaccia mostra. {@code passwordHash} resta nell'entita e non
 * ha modo di uscire dal server: e il motivo per cui esiste questo DTO invece di
 * serializzare direttamente {@link Utente}.
 */
public record UtenteDto(Long id, String nome, String cognome, String email, String ruolo) {

    /** Costruisce il DTO a partire dall'entita. */
    public static UtenteDto da(Utente utente) {
        return new UtenteDto(utente.getId(), utente.getNome(), utente.getCognome(),
                utente.getEmail(), utente.getRuolo().name());
    }
}
