package it.appbancaria.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Parametri del token JWT, letti da {@code application.yml} sotto {@code appbancaria.jwt}.
 *
 * <p>La chiave deve essere lunga almeno 32 byte (256 bit): HS256 rifiuta le chiavi piu
 * corte con {@code WeakKeyException}. Quella nel file di configurazione e una chiave
 * dimostrativa, dichiarata tale nel README; in un sistema reale arriverebbe da una
 * variabile d'ambiente o da un gestore di segreti, mai dal repository.
 *
 * <p>E un record: il binding avviene per costruttore, quindi i valori sono immutabili e
 * non servono setter. La registrazione la fa {@code @ConfigurationPropertiesScan}, gia
 * presente sulla classe principale dell'applicazione.
 *
 * @param chiave   segreto condiviso per firmare e verificare i token (almeno 32 byte)
 * @param scadenza durata del token dall'emissione, es. {@code PT2H}
 */
@ConfigurationProperties(prefix = "appbancaria.jwt")
public record ConfigurazioneJwt(String chiave, Duration scadenza) {
}
