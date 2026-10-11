package it.appbancaria.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Corpo JSON di tutti gli errori dell'API: una forma sola, uguale per ogni endpoint e per
 * ogni causa, dal token mancante all'errore interno.
 *
 * <p>Il campo da mostrare all'utente e {@code errore}: e un messaggio in italiano, gia
 * pronto per l'interfaccia. {@code dettagli} elenca i campi che non hanno superato la
 * validazione, quando ce ne sono. {@code percorso}, {@code stato} e {@code istante}
 * servono a chi legge i log o segnala un problema.
 *
 * <p>Le risposte di errore nascono in due punti: dal {@code GestoreErrori} per tutto cio
 * che succede dentro i controller, e dal {@code PuntoIngressoNonAutenticato} per le
 * richieste fermate prima, quando il token manca o non e valido. Entrambi costruiscono il
 * corpo con le stesse fabbriche, cosi la forma non puo divergere.
 *
 * @param istante  momento in cui l'errore e stato prodotto
 * @param stato    codice HTTP, ripetuto nel corpo per comodita del client
 * @param errore   messaggio leggibile da mostrare all'utente
 * @param percorso percorso della richiesta che ha fallito
 * @param dettagli campi non validi, nel formato {@code campo: messaggio}; vuota se non ce ne sono
 */
@Schema(description = "Errore dell'API, sempre nella stessa forma")
public record ErroreRisposta(
        @Schema(description = "Momento in cui l'errore e stato prodotto", example = "2026-10-11T14:30:00.123456")
        LocalDateTime istante,

        @Schema(description = "Codice HTTP, ripetuto nel corpo per comodita del client", example = "404")
        int stato,

        @Schema(description = "Messaggio leggibile, da mostrare all'utente", example = "Conto non trovato")
        String errore,

        @Schema(description = "Percorso della richiesta che ha fallito", example = "/api/accounts/99/movements")
        String percorso,

        @Schema(description = "Campi non validi, nel formato campo: messaggio", example = "[\"importo: L'importo deve essere positivo\"]")
        List<String> dettagli) {

    /** Errore senza dettagli di validazione. */
    public static ErroreRisposta di(HttpStatus stato, String messaggio, String percorso) {
        return new ErroreRisposta(LocalDateTime.now(), stato.value(), messaggio, percorso, List.of());
    }

    /** Errore con l'elenco dei campi che non hanno superato la validazione. */
    public static ErroreRisposta conDettagli(HttpStatus stato, String messaggio, String percorso,
                                             List<String> dettagli) {
        return new ErroreRisposta(LocalDateTime.now(), stato.value(), messaggio, percorso, List.copyOf(dettagli));
    }
}
