package it.appbancaria.security;

import it.appbancaria.model.Utente;
import it.appbancaria.repository.UtenteRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Carica il cliente a partire dall'email contenuta nel token.
 *
 * <p>Il token dice chi sostiene di essere; questo servizio lo conferma leggendo il
 * database a ogni richiesta. E una query in piu per richiesta, ma in cambio un cliente
 * cancellato, o un token emesso prima di un cambio di ruolo, smette subito di funzionare.
 */
@Service
public class ServizioUtenti implements UserDetailsService {

    private final UtenteRepository utenteRepository;

    public ServizioUtenti(UtenteRepository utenteRepository) {
        this.utenteRepository = utenteRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Utente utente = utenteRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Cliente non trovato: " + email));
        return new UtenteAutenticato(utente.getId(), utente.getEmail(), utente.getRuolo());
    }
}
