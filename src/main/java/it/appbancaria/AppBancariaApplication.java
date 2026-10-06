package it.appbancaria;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Punto di ingresso dell'API didattica AppBancaria.
 *
 * <p>L'applicazione e esclusivamente dimostrativa: non esegue operazioni bancarie reali
 * e non tratta dati o credenziali reali.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class AppBancariaApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppBancariaApplication.class, args);
    }
}
