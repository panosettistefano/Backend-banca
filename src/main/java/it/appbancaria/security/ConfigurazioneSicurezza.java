package it.appbancaria.security;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Regole di sicurezza dell'API: tutto chiuso tranne login, health check e documentazione.
 *
 * <p>L'API e stateless: nessuna sessione HTTP, nessun cookie, nessun form di login. Ogni
 * richiesta si identifica con il token. Per questo CSRF e disattivato (protegge le
 * richieste basate su cookie, che qui non esistono) insieme a formLogin, httpBasic e
 * logout, che non hanno senso in un'API di questo tipo.
 *
 * <p>Le rotte aperte: il login, che e la porta da cui si entra; {@code /actuator/health},
 * usato da Docker e dagli script per capire se l'API e viva; le pagine di Swagger UI, che
 * documentano l'API e vanno lette prima di avere un token.
 *
 * <p>La protezione dei metodi ({@code @PreAuthorize} e simili) arriva allo STEP 4, quando
 * ci saranno regole per ruolo da esprimere.
 */
@Configuration
@EnableWebSecurity
public class ConfigurazioneSicurezza {

    private final FiltroAutenticazioneJwt filtroJwt;
    private final PuntoIngressoNonAutenticato puntoIngresso;

    public ConfigurazioneSicurezza(FiltroAutenticazioneJwt filtroJwt,
                                   PuntoIngressoNonAutenticato puntoIngresso) {
        this.filtroJwt = filtroJwt;
        this.puntoIngresso = puntoIngresso;
    }

    @Bean
    public SecurityFilterChain catenaDiSicurezza(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(sessione ->
                        sessione.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(regole -> regole
                        // La dispatch di errore non e una richiesta del client: quando una
                        // risposta esce con un errore (400 di validazione, 404 di rotta
                        // inesistente) il contenitore riesegue la catena su /error per
                        // comporre il corpo. Senza questa regola la sicurezza la tratterebbe
                        // come una richiesta anonima e il 401 dell'entry point coprirebbe lo
                        // status vero, facendo sembrare "non autenticato" ogni 400 o 404.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers("/actuator/health", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(gestione ->
                        gestione.authenticationEntryPoint(puntoIngresso))
                // Il filtro JWT precede il filtro che, nelle applicazioni con sessione,
                // leggerebbe username e password dal form: qui il token e l'unica via.
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Algoritmo di hashing delle password: BCrypt, lo stesso con cui sono stati prodotti
     * gli hash del seed demo. Al login si confronta con {@code matches}, mai confrontando
     * due stringhe.
     */
    @Bean
    public PasswordEncoder codificatorePassword() {
        return new BCryptPasswordEncoder();
    }
}
