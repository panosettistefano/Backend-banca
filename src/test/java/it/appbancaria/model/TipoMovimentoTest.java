package it.appbancaria.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test di logica pura sul tipo di movimento: nessun database, nessun contesto Spring,
 * girano in millisecondi.
 */
class TipoMovimentoTest {

    @Test
    @DisplayName("Solo saldo iniziale, versamento e trasferimento in entrata aumentano il saldo")
    void entrateAumentanoIlSaldo() {
        assertThat(TipoMovimento.SALDO_INIZIALE.isEntrata()).isTrue();
        assertThat(TipoMovimento.VERSAMENTO.isEntrata()).isTrue();
        assertThat(TipoMovimento.TRASFERIMENTO_ENTRATA.isEntrata()).isTrue();

        assertThat(TipoMovimento.PRELIEVO.isEntrata()).isFalse();
        assertThat(TipoMovimento.TRASFERIMENTO_USCITA.isEntrata()).isFalse();
    }

    @Test
    @DisplayName("I tipi di trasferimento sono esattamente i due collegati a un trasferimento")
    void soloITrasferimentiSonoCollegatiAUnTrasferimento() {
        assertThat(TipoMovimento.TRASFERIMENTO_USCITA.isTrasferimento()).isTrue();
        assertThat(TipoMovimento.TRASFERIMENTO_ENTRATA.isTrasferimento()).isTrue();

        assertThat(TipoMovimento.SALDO_INIZIALE.isTrasferimento()).isFalse();
        assertThat(TipoMovimento.VERSAMENTO.isTrasferimento()).isFalse();
        assertThat(TipoMovimento.PRELIEVO.isTrasferimento()).isFalse();
    }
}
