package it.appbancaria.security;

import it.appbancaria.model.RuoloUtente;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Identita del cliente autenticato, ricostruita dal token a ogni richiesta.
 *
 * <p>Non e l'entita JPA {@code Utente}: e un valore immutabile con i soli dati che servono
 * a decidere se e cosa il cliente puo fare. Essendo un {@link UserDetails}, Spring Security
 * lo usa come principal e i controller potranno riceverlo con {@code @AuthenticationPrincipal}
 * (servira dallo STEP 4 per risalire ai conti del cliente).
 *
 * @param id    chiave del cliente, per risalire ai suoi dati senza ripartire dall'email
 * @param email email di accesso, usata anche come {@code username}
 * @param ruolo ruolo del cliente, esposto come autorita {@code ROLE_*}
 */
public record UtenteAutenticato(Long id, String email, RuoloUtente ruolo) implements UserDetails {

    /** Ruolo nel formato atteso da Spring Security: {@code ROLE_CLIENTE}, {@code ROLE_ADMIN}. */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + ruolo.name()));
    }

    /**
     * Nessuna password: l'autenticazione e gia avvenuta, al login o verificando la firma
     * del token. Il valore non viene mai usato per un confronto.
     */
    @Override
    public String getPassword() {
        return null;
    }

    /** L'email e il nome con cui il cliente si autentica. */
    @Override
    public String getUsername() {
        return email;
    }

    // Nessuno di questi stati e previsto dal modello dati: l'account c'e ed e attivo.
    // I metodi esistono perche li richiede l'interfaccia UserDetails.

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
