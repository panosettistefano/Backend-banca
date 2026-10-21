# AppBancaria — API

API REST didattica per consultare conti e movimenti e simulare trasferimenti tra conti.
Nessun dato bancario reale: clienti, conti, movimenti e credenziali sono fittizi e le operazioni
non hanno alcun effetto fuori dal database demo.

## Stack

- Java 21 (target), Spring Boot 3.5.16
- Spring Security con JWT stateless HS256 e password BCrypt
- MySQL 8.4.11 con migrazioni Flyway
- Maven Wrapper (`mvnw`) e Testcontainers per i test
- Swagger UI / OpenAPI con springdoc 2.8.17

## Requisiti

- Docker e Docker Compose (modo consigliato)
- JDK 21 per far girare l'API senza Docker (il wrapper scarica Maven)
- Node 24.16 solo per il frontend nel repository `appbancaria-web`
- I due repository `appbancaria-api` e `appbancaria-web` clonati nella stessa cartella, con questi nomi
  (il compose costruisce il frontend da `../appbancaria-web`)

## Avvio rapido (stack completo)

Dal repository `appbancaria-api`, con `appbancaria-web` clonato nella stessa cartella:

```bash
./avvia-tutto.sh
```

Lo script controlla Docker e le porte `8080`, `4200` e `3306`, avvia MySQL, API e frontend e attende i
healthcheck. Le immagini vengono costruite se mancano; per ricostruirle: `./avvia-tutto.sh --build`.
Al termine:

| Servizio | Indirizzo |
|---|---|
| Frontend | http://localhost:4200 |
| API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

Per fermare tutto: `./avvia-tutto.sh --stop`. I dati restano nel volume.

## Solo backend

```bash
docker compose up -d mysql api
```

Avvia solo MySQL e API, senza il frontend (che il compose completo costruirebbe da `../appbancaria-web`).
Per seguire i log: `docker compose logs -f api`.

### Avvio locale dell'API

```bash
docker compose up -d mysql
./mvnw spring-boot:run
```

L'API usa di default `localhost:3306`, database `appbancaria`, utente `demo`. Le credenziali e la
chiave JWT si possono sovrascrivere con le variabili `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`,
`DB_PASSWORD` e `JWT_CHIAVE` (vedi `src/main/resources/application.yml`).

## Dati demo

| Cliente | Email | Password | Saldi iniziali |
|---|---|---|---|
| Mario Rossi | `mario.rossi@demo.it` | `Demo1234!` | 8.742,00 € e 14.042,00 € |
| Giulia Bianchi | `giulia.bianchi@demo.it` | `Demo1234!` | 7.692,00 € e 15.542,00 € |

Il seed contiene 84 movimenti distribuiti sugli ultimi ~80 giorni. Le password nel database sono hash
BCrypt. Per tornare ai dati iniziali: `docker compose down -v` e poi riavviare. Per aggiungere dati demo
nuovi si crea una migrazione Flyway successiva (per esempio `V3__seed_altri_clienti.sql`), senza mai
modificare una migrazione gia applicata.

## Endpoint

| Metodo ed endpoint | Scopo |
|---|---|
| `POST /api/auth/login` | login, restituisce il token JWT |
| `GET /api/accounts` | conti del cliente autenticato |
| `GET /api/accounts/{id}/movements?da=&a=` | movimenti del conto, filtri data opzionali |
| `POST /api/transfers` | crea un trasferimento `IN_ATTESA` |
| `POST /api/transfers/{id}/confirm` | esegue il trasferimento in transazione |
| `GET /api/transfers?stato=IN_ATTESA` | trasferimenti da confermare |

Gli errori hanno tutti la stessa forma:

```json
{
  "istante": "2026-10-21T18:30:00",
  "stato": 409,
  "errore": "Saldo insufficiente",
  "percorso": "/api/transfers/5/confirm",
  "dettagli": []
}
```

Il campo da mostrare all'utente è `errore`. La documentazione completa con esempi è in Swagger UI.
Il token va inviato come `Authorization: Bearer <token>`; scade dopo 2 ore.

## Test

```bash
./mvnw verify
```

La suite esegue **90 test**: unitari, di integrazione su MySQL reale (Testcontainers, immagine
`mysql:8.4.11`) e test di atomicità dei trasferimenti. Serve Docker attivo. I test coprono login,
autorizzazioni, isolamento tra clienti, validazione, filtri e rollback.

## CI/CD

- GitHub Actions: `.github/workflows/ci.yml`
- GitLab CI: `.gitlab-ci.yml`
- Istruzioni di push e prerequisiti del runner: [`CI_CD.md`](CI_CD.md)

## Struttura

```
src/main/java/it/appbancaria
  model/         entita JPA e enum
  repository/    repository Spring Data
  service/       logica di dominio e transazioni
  security/      JWT, filtro, configurazione Spring Security
  web/           controller e DTO
src/main/resources/db/migration   schema e seed Flyway
src/test/java  test unitari e di integrazione
```

## Problemi comuni

- **Porta 8080 occupata** da un altro progetto: fermare quel processo (per esempio
  `docker stop esame_frontend esame_api esame_db`) prima di avviare l'API.
- **Porta 3306 occupata** da un altro MySQL: fermarlo o cambiare la mappatura nel compose.
- **Flyway 11 e MySQL 8.4**: all'avvio compare il warning *"MySQL 8.4 is newer than this version of
  Flyway"*; è innocuo, le migrazioni vengono applicate.
- **Testcontainers**: i test di integrazione richiedono Docker attivo.
- **`appbancaria-web` non trovato**: il compose completo cerca il frontend in `../appbancaria-web`.
  Clonare i due repo nella stessa cartella e avviare da `appbancaria-api`.
- **Frontend**: richiede Node 24.16; vedere il README di `appbancaria-web`.
