package it.appbancaria.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import it.appbancaria.model.RuoloUtente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Firma, scadenza e contenuto dei token.
 *
 * <p>Test di sola logica: nessun contesto Spring, nessun database, nessun Docker. Il
 * servizio si costruisce a mano con una configurazione di prova, quindi questi test
 * girano in millisecondi e restano validi anche senza Docker attivo.
 */
class ServizioJwtTest {

    /** 38 byte: sopra il minimo di 32 richiesto da HS256. */
    private static final String CHIAVE = "chiave-di-prova-lunga-almeno-32-byte!!";
    private static final String ALTRA_CHIAVE = "chiave-diversa-lunga-almeno-32-byte!!";

    private static final UtenteAutenticato MARIO =
            new UtenteAutenticato(1L, "mario.rossi@demo.it", RuoloUtente.CLIENTE);

    private final ServizioJwt servizioJwt =
            new ServizioJwt(new ConfigurazioneJwt(CHIAVE, Duration.ofHours(2)));

    @Test
    @DisplayName("Il token e in tre parti e contiene email, ruolo e una scadenza futura")
    void tokenConEmailRuoloEScadenza() {
        TokenGenerato generato = servizioJwt.generaToken(MARIO);
        String token = generato.token();

        // Formato JWT: header.payload.firma
        assertThat(token.split("\\.")).hasSize(3);
        assertThat(servizioJwt.emailDaToken(token)).isEqualTo("mario.rossi@demo.it");
        assertThat(generato.scadeIl()).isAfter(Instant.now());

        // Il ruolo viaggia come claim, leggibile senza la chiave (non e un segreto:
        // la firma garantisce che nessuno possa cambiarlo).
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]),
                StandardCharsets.UTF_8);
        assertThat(payload).contains("\"ruolo\":\"CLIENTE\"");
    }

    @Test
    @DisplayName("Un token scaduto viene rifiutato")
    void tokenScadutoRifiutato() {
        // Scadenza gia passata: si genera con una configurazione di prova, senza aspettare.
        ServizioJwt servizioScaduto =
                new ServizioJwt(new ConfigurazioneJwt(CHIAVE, Duration.ofSeconds(-5)));
        String tokenScaduto = servizioScaduto.generaToken(MARIO).token();

        assertThatThrownBy(() -> servizioJwt.emailDaToken(tokenScaduto))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    @DisplayName("Un token firmato con un'altra chiave viene rifiutato")
    void firmaEstraneaRifiutata() {
        ServizioJwt servizioEstraneo =
                new ServizioJwt(new ConfigurazioneJwt(ALTRA_CHIAVE, Duration.ofHours(2)));
        String tokenEstraneo = servizioEstraneo.generaToken(MARIO).token();

        assertThatThrownBy(() -> servizioJwt.emailDaToken(tokenEstraneo))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    @DisplayName("Il principal espone il ruolo come autorita ROLE_*")
    void autoritaDalRuolo() {
        assertThat(MARIO.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_CLIENTE");
    }
}
