-- =====================================================================
-- V2__seed_demo.sql — Dati dimostrativi (nessun dato reale)
--
-- Contenuto:
--   - 2 clienti demo        (mario.rossi@demo.it, giulia.bianchi@demo.it)
--   - 4 conti in EUR        (2 per cliente)
--   - 1 saldo iniziale + 20 movimenti per conto, distribuiti su ~80 giorni
--
-- Password demo di ENTRAMBI i clienti:  Demo1234!
-- Gli hash BCrypt qui sotto sono stati generati una tantum e non
-- contengono la password in chiaro in nessuna forma.
--
-- ---------------------------------------------------------------------
-- COME AGGIUNGERE ALTRI DATI DEMO (procedura, vedi anche il README)
--   1. NON modificare questo file: una migrazione gia applicata non si
--      tocca, altrimenti Flyway blocca l'avvio per checksum diverso.
--   2. Creare un nuovo file con il numero successivo, per esempio
--      V3__seed_altri_clienti.sql, nella stessa cartella.
--   3. Inserire i dati usando le chiavi naturali (email, codice_conto)
--      e non gli id, che sono generati dal database.
--   4. Se si aggiungono movimenti, rieseguire in coda al file la
--      riconciliazione dei saldi (l'UPDATE in fondo a questo script):
--      il saldo del conto deve restare uguale alla somma dei movimenti.
--   5. Le password vanno inserite solo come hash BCrypt.
--   6. Riavviare l'API: Flyway applica la nuova migrazione da solo.
-- =====================================================================

SET NAMES utf8mb4;

-- ---------------------------------------------------------------------
-- Clienti demo
-- ---------------------------------------------------------------------
INSERT INTO utente (email, password_hash, ruolo, nome, cognome) VALUES
    ('mario.rossi@demo.it',
     '$2a$10$d0lKtSy0K6U9Nr1E/lazPO59NH0QouFuni30X.GTrJ8nn8zuUIH9S',
     'CLIENTE', 'Mario', 'Rossi'),
    ('giulia.bianchi@demo.it',
     '$2a$10$jsfipXgdTu6HWgI/ukxggOFJ4R13f9s947n2j9dYr.cm5n3GmQ7CC',
     'CLIENTE', 'Giulia', 'Bianchi');

-- ---------------------------------------------------------------------
-- Conti: il saldo parte da zero e viene calcolato in fondo allo script
-- come somma dei movimenti (cosi l'invariante saldo = somma e garantita).
-- ---------------------------------------------------------------------
INSERT INTO conto (codice_conto, titolare_id, valuta, saldo)
SELECT v.codice_conto, u.id, 'EUR', 0.00
FROM (
          SELECT 'IT60X0542811101000000123456' AS codice_conto, 'mario.rossi@demo.it'   AS email
    UNION SELECT 'IT60X0542811101000000654321',              'mario.rossi@demo.it'
    UNION SELECT 'IT91X0300203280000400161234',              'giulia.bianchi@demo.it'
    UNION SELECT 'IT91X0300203280000400169876',              'giulia.bianchi@demo.it'
) AS v
JOIN utente u ON u.email = v.email;

-- ---------------------------------------------------------------------
-- Saldo iniziale di apertura, con importi diversi per conto.
-- ---------------------------------------------------------------------
INSERT INTO movimento (conto_id, data_movimento, descrizione, tipo, importo)
SELECT c.id,
       DATE_SUB(NOW(6), INTERVAL 90 DAY),
       'Saldo iniziale di apertura conto',
       'SALDO_INIZIALE',
       v.importo_iniziale
FROM (
          SELECT 'IT60X0542811101000000123456' AS codice_conto, 2500.00 AS importo_iniziale
    UNION SELECT 'IT60X0542811101000000654321',                7800.00
    UNION SELECT 'IT91X0300203280000400161234',                1450.00
    UNION SELECT 'IT91X0300203280000400169876',                9300.00
) AS v
JOIN conto c ON c.codice_conto = v.codice_conto;

-- ---------------------------------------------------------------------
-- 20 movimenti per ogni conto esistente, distribuiti sugli ultimi ~80 giorni.
-- Il ciclo genera descrizioni, tipi e importi alternati e realistici.
--
-- Estensione: aggiungendo altri conti (in questo file o in un file
-- successivo) e rieseguendo questo blocco, i movimenti vengono generati
-- automaticamente anche per i nuovi conti.
-- ---------------------------------------------------------------------
INSERT INTO movimento (conto_id, data_movimento, descrizione, tipo, importo)
WITH RECURSIVE serie (n) AS (
    SELECT 1
    UNION ALL
    SELECT n + 1 FROM serie WHERE n < 20
)
SELECT c.id,
       DATE_SUB(NOW(6), INTERVAL (serie.n * 4 + c.id) DAY) + INTERVAL (7 + c.id) HOUR,
       CASE serie.n % 5
           WHEN 0 THEN 'Stipendio mensile demo'
           WHEN 1 THEN 'Pagamento utenze demo'
           WHEN 2 THEN 'Bonifico ricevuto demo'
           WHEN 3 THEN 'Prelievo ATM demo'
           ELSE        'Acquisto POS demo'
       END,
       CASE serie.n % 5
           WHEN 0 THEN 'VERSAMENTO'
           WHEN 1 THEN 'PRELIEVO'
           WHEN 2 THEN 'VERSAMENTO'
           WHEN 3 THEN 'PRELIEVO'
           ELSE        'PRELIEVO'
       END,
       CASE serie.n % 5
           WHEN 0 THEN 1500.00 + serie.n
           WHEN 1 THEN   45.00 + (serie.n * 3)
           WHEN 2 THEN  120.00 + (serie.n * 7)
           WHEN 3 THEN   20.00 + (serie.n * 2)
           ELSE          15.50 + serie.n
       END
FROM conto c
CROSS JOIN serie;

-- ---------------------------------------------------------------------
-- Riconciliazione dei saldi: il saldo di ogni conto diventa la somma
-- algebrica dei suoi movimenti (entrate meno uscite).
--
-- Questa e l'invariante del progetto, verificata anche dai test:
--     conto.saldo = SOMMA(entrate) - SOMMA(uscite)
-- Va rieseguita in coda a ogni migrazione che aggiunge movimenti.
-- ---------------------------------------------------------------------
UPDATE conto c
SET c.saldo = (
    SELECT COALESCE(SUM(
               CASE
                   WHEN m.tipo IN ('SALDO_INIZIALE', 'VERSAMENTO', 'TRASFERIMENTO_ENTRATA')
                       THEN m.importo
                   ELSE -m.importo
               END), 0.00)
    FROM movimento m
    WHERE m.conto_id = c.id
);
