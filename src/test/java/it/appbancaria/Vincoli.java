package it.appbancaria;

import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.springframework.dao.DataAccessException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Asserzioni sui vincoli del database, condivise dai test dei repository.
 *
 * <p><strong>Perche non basta verificare il tipo di eccezione.</strong> Su MySQL una
 * violazione di un vincolo {@code CHECK} arriva a Spring come {@code JpaSystemException}:
 * il driver la segnala come errore generico (SQLState {@code HY000}) e non come violazione
 * di integrita, quindi Hibernate non la classifica come {@code ConstraintViolationException}.
 * Il tipo di eccezione da solo non distingue "dato non valido" da "guasto del database".
 *
 * <p>Quello che conta e che il database abbia <em>rifiutato</em> la scrittura, e il messaggio
 * dell'errore dice esattamente quale vincolo e stato violato: ed e quello che si verifica
 * qui, indicando il nome del vincolo dello schema. Un vincolo scritto male, o rimosso,
 * fa fallire il test.
 */
public final class Vincoli {

    private Vincoli() {
    }

    /**
     * Verifica che la scrittura sia stata rifiutata dal vincolo indicato.
     *
     * @param operazione  la scrittura da tentare (di solito un {@code saveAndFlush})
     * @param nomeVincolo nome del vincolo nello schema, es. {@code chk_conto_saldo}
     */
    public static void rifiutatoDalDatabase(ThrowingCallable operazione, String nomeVincolo) {
        assertThatThrownBy(operazione)
                .as("la scrittura deve essere rifiutata dal vincolo %s", nomeVincolo)
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining(nomeVincolo);
    }
}
