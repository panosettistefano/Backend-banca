package it.appbancaria;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Database di test: un MySQL vero, in un container usa e getta.
 *
 * <p>I test di integrazione non girano su un database in memoria: userebbero un altro
 * motore SQL e non verificherebbero ne lo schema Flyway ne i vincoli CHECK di MySQL,
 * che sono proprio una delle cose da collaudare.
 *
 * <p>Serve Docker attivo. {@code @ServiceConnection} collega il container alla sorgente
 * dati dell'applicazione senza dover scrivere l'URL a mano, e Spring Boot si occupa di
 * avviare e fermare il container insieme al contesto di test.
 *
 * <p>L'immagine e la stessa di {@code docker-compose.yml}, con i parametri di connessione
 * allineati: i test girano nella stessa configurazione della produzione locale.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    /** Stessa versione pinnata del docker-compose: mai "latest". */
    private static final String IMMAGINE_MYSQL = "mysql:8.4.11";

    @Bean
    @ServiceConnection
    MySQLContainer<?> mysql() {
        return new MySQLContainer<>(DockerImageName.parse(IMMAGINE_MYSQL))
                .withDatabaseName("appbancaria")
                .withUsername("demo")
                .withPassword("demo")
                // Come nell'URL JDBC di application.yml: le date viaggiano in UTC e il
                // server MySQL del container lavora in UTC, cosi i filtri per data non slittano.
                .withUrlParam("connectionTimeZone", "UTC")
                .withUrlParam("allowPublicKeyRetrieval", "true");
    }
}
