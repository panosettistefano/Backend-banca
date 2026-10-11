package it.appbancaria.web;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Documentazione OpenAPI servita da Swagger UI su {@code /swagger-ui.html}.
 *
 * <p>Descrive l'API una volta sola: il token si dichiara qui come schema di sicurezza
 * {@code bearerAuth} ed e richiesto da tutti gli endpoint tranne il login, che lo esclude
 * con {@code @SecurityRequirements}. Da Swagger UI si puo quindi fare il login, copiare il
 * token e usarlo con il pulsante "Authorize" per provare le altre chiamate.
 *
 * <p>La rotta della documentazione e aperta (vedi {@code ConfigurazioneSicurezza}): va
 * letta prima di avere un token.
 */
@Configuration
public class ConfigurazioneOpenApi {

    /** Nome dello schema di sicurezza, usato anche nelle annotazioni dei controller. */
    public static final String SCHEMA_TOKEN = "bearerAuth";

    @Bean
    public OpenAPI documentazioneApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("AppBancaria - API dimostrativa")
                        .version("1.0")
                        .description("""
                                API didattica per consultare conti e movimenti e per simulare trasferimenti fra
                                conti. Nessun dato reale: clienti, conti e movimenti sono fittizi e nessuna
                                operazione ha effetti fuori da questo database.

                                Come provarla: eseguire POST /api/auth/login con un cliente demo
                                (mario.rossi@demo.it / Demo1234!), copiare il token dalla risposta e incollarlo
                                in "Authorize". Il token vale 2 ore.
                                """))
                .components(new Components().addSecuritySchemes(SCHEMA_TOKEN, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT del login, da inviare come Authorization: Bearer <token>")))
                .addSecurityItem(new SecurityRequirement().addList(SCHEMA_TOKEN))
                .tags(List.of(
                        new Tag().name("Autenticazione").description("Login e token"),
                        new Tag().name("Conti e movimenti").description("Conti del cliente autenticato e loro movimenti"),
                        new Tag().name("Trasferimenti").description("Trasferimenti simulati in due fasi: richiesta e conferma")));
    }
}
