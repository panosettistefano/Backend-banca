# CI/CD

Pipeline pronte per GitHub Actions e GitLab CI.

## Cosa esegue

| Piattaforma | File | Job |
|---|---|---|
| GitHub Actions | `.github/workflows/ci.yml` | `test` (JDK 21 + Testcontainers) → `immagine-docker` |
| GitLab CI | `.gitlab-ci.yml` | `test` (Maven + MySQL in Docker-in-Docker) → `immagine-docker` |

Il job di test esegue `mvnw verify`: compila, applica le migrazioni su un MySQL usa-e-getta e lancia
tutti i test. Il job immagine costruisce `appbancaria-api` dal `Dockerfile`. Non servono credenziali
ne secret.

## Primo push

Creare due repository vuoti (senza README) e aggiungere i remote:

```bash
git branch -M main
git remote add github git@github.com:<utente>/appbancaria-api.git
git remote add gitlab git@gitlab.com:<utente>/appbancaria-api.git
git push -u github main
git push -u gitlab main
```

Le pipeline partono al primo push. Su GitHub non serve altro. Su GitLab il job `test` usa
Docker-in-Docker: il runner deve poter avviare container privilegiati (o avere il socket Docker
montato), altrimenti la suite con Testcontainers non puo partire.

## Verifica in locale

```bash
./mvnw -B verify
docker build -t appbancaria-api:locale .
```

La CI non pubblica l'immagine in un registry: la build serve a verificare il `Dockerfile`. Per avviare
lo stack completo in locale, dal repository backend (con `appbancaria-web` nella stessa cartella):

```bash
./avvia-tutto.sh
```
