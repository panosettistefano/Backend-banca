package it.appbancaria.service;

import it.appbancaria.model.Utente;
import it.appbancaria.repository.UtenteRepository;
import it.appbancaria.security.ServizioJwt;
import it.appbancaria.security.TokenGenerato;
import it.appbancaria.security.UtenteAutenticato;
import it.appbancaria.web.dto.RichiestaLogin;
import it.appbancaria.web.dto.RispostaLogin;
import it.appbancaria.web.dto.UtenteDto;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Verifica le credenziali e, se corrette, emette il token.
 *
 * <p>Il confronto e esplicito ({@code findByEmail} + {@code matches}) invece di passare
 * dall'{@code AuthenticationManager}: cosi in un punto solo si legge cosa succede al
 * login, senza tirare in mezzo provider e sessioni che questa API non usa.
 *
 * <p>Quando l'email non esiste, {@code filter} non esegue nemmeno {@code matches}: nessun
 * hash da confrontare, nessun calcolo inutile.
 */
@Service
public class ServizioAutenticazione {

    private final UtenteRepository utenteRepository;
    private final PasswordEncoder codificatorePassword;
    private final ServizioJwt servizioJwt;

    public ServizioAutenticazione(UtenteRepository utenteRepository,
                                  PasswordEncoder codificatorePassword,
                                  ServizioJwt servizioJwt) {
        this.utenteRepository = utenteRepository;
        this.codificatorePassword = codificatorePassword;
        this.servizioJwt = servizioJwt;
    }

    /**
     * @return token e dati del cliente, pronti per la risposta di login
     * @throws CredenzialiNonValideException email sconosciuta o password errata
     */
    public RispostaLogin autentica(RichiestaLogin richiesta) {
        Utente utente = utenteRepository.findByEmail(richiesta.email())
                .filter(trovato -> codificatorePassword.matches(richiesta.password(), trovato.getPasswordHash()))
                .orElseThrow(CredenzialiNonValideException::new);

        UtenteAutenticato autenticato =
                new UtenteAutenticato(utente.getId(), utente.getEmail(), utente.getRuolo());
        TokenGenerato generato = servizioJwt.generaToken(autenticato);

        return new RispostaLogin(generato.token(), "Bearer", generato.scadeIl(), UtenteDto.da(utente));
    }
}
