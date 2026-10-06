-- =====================================================================
-- V1__schema.sql — Struttura del database (nessun dato)
--
-- MySQL 8.4 / InnoDB / utf8mb4
-- Applicazione dimostrativa: nessun dato bancario reale.
--
-- ATTENZIONE: questa migrazione NON va piu modificata dopo essere stata
-- applicata. Flyway ne calcola un checksum e, se il file cambia,
-- l'applicazione non parte piu. Per evolvere lo schema si aggiunge un
-- nuovo file con il numero successivo (V3__..., V4__...).
--
-- Convenzioni:
--   - tabelle e colonne in italiano, snake_case
--   - importi SEMPRE DECIMAL(15,2): mai FLOAT o DOUBLE
--   - date DATETIME(6) (non TIMESTAMP: subisce la timezone di sessione)
--   - nomi dei vincoli prefissati con la tabella: in MySQL i nomi di
--     CHECK e FOREIGN KEY devono essere unici nell'intero schema
--   - l'importo dei movimenti e sempre POSITIVO: il segno lo da il tipo
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- utente — anagrafica e credenziali di accesso.
-- La password e conservata SOLO come hash BCrypt, mai in chiaro.
-- Il ruolo e previsto dal modello dati; oggi esiste solo CLIENTE,
-- ADMIN e predisposto per future estensioni (nessun endpoint admin).
-- ---------------------------------------------------------------------
CREATE TABLE utente (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    email         VARCHAR(180) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,          -- BCrypt = 60 caratteri; 100 lascia margine
    ruolo         VARCHAR(20)  NOT NULL,
    nome          VARCHAR(80)  NOT NULL,
    cognome       VARCHAR(80)  NOT NULL,
    creato_il     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_utente_email   UNIQUE (email),
    CONSTRAINT chk_utente_ruolo  CHECK (ruolo IN ('CLIENTE', 'ADMIN'))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- conto — saldo materializzato, aggiornato solo dentro le transazioni
-- dei trasferimenti. Nessun fido: il saldo non puo mai andare sotto zero.
-- ---------------------------------------------------------------------
CREATE TABLE conto (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    codice_conto VARCHAR(34)   NOT NULL,          -- formato tipo IBAN, massimo 34 caratteri
    titolare_id  BIGINT        NOT NULL,
    valuta       VARCHAR(3)    NOT NULL,          -- codice ISO 4217
    saldo        DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    creato_il    DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_conto_codice_conto UNIQUE (codice_conto),
    CONSTRAINT fk_conto_titolare     FOREIGN KEY (titolare_id) REFERENCES utente (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_conto_valuta      CHECK (valuta IN ('EUR', 'USD', 'GBP')),
    CONSTRAINT chk_conto_saldo       CHECK (saldo >= 0),
    KEY idx_conto_titolare (titolare_id)          -- serve GET /api/accounts
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- trasferimento — macchina a stati IN_ATTESA -> COMPLETATO | RIFIUTATO.
--
-- La creazione non tocca alcun saldo e non prenota fondi: e solo una
-- richiesta registrata. E' la conferma a eseguire l'operazione, in una
-- sola transazione insieme ai due movimenti.
--
-- conto_origine_id e conto_destinazione_id sono chiavi esterne verso
-- conto: il vincolo chk_trasferimento_conti_distinti impedisce che una
-- riga punti allo stesso conto due volte.
-- ---------------------------------------------------------------------
CREATE TABLE trasferimento (
    id                    BIGINT        NOT NULL AUTO_INCREMENT,
    riferimento           VARCHAR(36)   NOT NULL,   -- UUID mostrato all'utente
    utente_id             BIGINT        NOT NULL,   -- chi ha richiesto il trasferimento
    conto_origine_id      BIGINT        NOT NULL,
    conto_destinazione_id BIGINT        NOT NULL,
    valuta                VARCHAR(3)    NOT NULL,
    importo               DECIMAL(15,2) NOT NULL,
    causale               VARCHAR(140)  NULL,
    stato                 VARCHAR(16)   NOT NULL,
    motivo_rifiuto        VARCHAR(255)  NULL,
    creato_il             DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    confermato_il         DATETIME(6)   NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_trasferimento_riferimento UNIQUE (riferimento),
    CONSTRAINT fk_trasferimento_utente      FOREIGN KEY (utente_id) REFERENCES utente (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_trasferimento_origine     FOREIGN KEY (conto_origine_id) REFERENCES conto (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_trasferimento_destinazione FOREIGN KEY (conto_destinazione_id) REFERENCES conto (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_trasferimento_importo    CHECK (importo > 0),
    CONSTRAINT chk_trasferimento_conti      CHECK (conto_origine_id <> conto_destinazione_id),
    CONSTRAINT chk_trasferimento_stato      CHECK (stato IN ('IN_ATTESA', 'COMPLETATO', 'RIFIUTATO')),
    -- Un trasferimento risulta confermato solo se ha una data di conferma, e viceversa
    CONSTRAINT chk_trasferimento_conferma   CHECK (
        (stato = 'COMPLETATO' AND confermato_il IS NOT NULL) OR
        (stato <> 'COMPLETATO' AND confermato_il IS NULL)
    ),
    -- Il motivo di rifiuto esiste solo per i trasferimenti rifiutati
    CONSTRAINT chk_trasferimento_motivo     CHECK (stato = 'RIFIUTATO' OR motivo_rifiuto IS NULL),
    KEY idx_trasferimento_origine      (conto_origine_id),
    KEY idx_trasferimento_destinazione (conto_destinazione_id),
    KEY idx_trasferimento_utente_stato (utente_id, stato, creato_il)   -- elenco dei trasferimenti in attesa
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------
-- movimento — una riga per ogni soldo che entra o esce da un conto.
--
-- L'importo e memorizzato sempre POSITIVO: il verso lo determina il tipo.
-- La valuta non e ripetuta qui: e quella del conto (join, non duplicazione).
--
-- uk_movimento_trasferimento_tipo garantisce che un trasferimento non
-- possa generare piu di un movimento per tipo: al massimo un'uscita e
-- un'entrata, anche in caso di bug applicativo.
-- ---------------------------------------------------------------------
CREATE TABLE movimento (
    id               BIGINT        NOT NULL AUTO_INCREMENT,
    conto_id         BIGINT        NOT NULL,
    data_movimento   DATETIME(6)   NOT NULL,
    descrizione      VARCHAR(255)  NOT NULL,
    tipo             VARCHAR(24)   NOT NULL,
    importo          DECIMAL(15,2) NOT NULL,
    trasferimento_id BIGINT        NULL,
    creato_il        DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT fk_movimento_conto          FOREIGN KEY (conto_id) REFERENCES conto (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT fk_movimento_trasferimento  FOREIGN KEY (trasferimento_id) REFERENCES trasferimento (id)
        ON DELETE RESTRICT ON UPDATE RESTRICT,
    CONSTRAINT chk_movimento_importo       CHECK (importo > 0),
    CONSTRAINT chk_movimento_tipo          CHECK (tipo IN (
        'SALDO_INIZIALE', 'VERSAMENTO', 'PRELIEVO', 'TRASFERIMENTO_USCITA', 'TRASFERIMENTO_ENTRATA')),
    -- Se il movimento nasce da un trasferimento, il tipo deve essere di trasferimento (e viceversa)
    CONSTRAINT chk_movimento_riferimento   CHECK (
        (trasferimento_id IS NOT NULL AND tipo LIKE 'TRASFERIMENTO%') OR
        (trasferimento_id IS NULL     AND tipo NOT LIKE 'TRASFERIMENTO%')
    ),
    CONSTRAINT uk_movimento_trasferimento_tipo UNIQUE (trasferimento_id, tipo),
    KEY idx_movimento_conto_data (conto_id, data_movimento)   -- filtro per conto e intervallo di date
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
