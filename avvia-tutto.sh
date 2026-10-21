#!/usr/bin/env bash
# =====================================================================
# AppBancaria — avvio dello stack completo dal repository backend
#
# Uso:
#   ./avvia-tutto.sh            avvia MySQL, API e frontend
#   ./avvia-tutto.sh --build    ricostruisce le immagini e avvia
#   ./avvia-tutto.sh --stop     ferma i container (i dati MySQL restano)
#   ./avvia-tutto.sh --logs     segue i log dei container
#   ./avvia-tutto.sh --help     mostra questo messaggio
#
# Il repo appbancaria-web deve trovarsi accanto a questo, con lo stesso nome.
# =====================================================================

set -euo pipefail

RADICE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$RADICE"

errore() {
    echo "ERRORE: $*" >&2
}

mostra_uso() {
    cat <<'EOF'
Uso: ./avvia-tutto.sh [opzione]

Senza opzioni  avvia MySQL, API e frontend (usa le immagini esistenti)
--build        ricostruisce le immagini prima di avviare
--stop         ferma i container (i dati MySQL restano nel volume)
--logs         segue i log dei container
--help, -h     mostra questo messaggio
EOF
}

mostra_indirizzi() {
    cat <<'EOF'

Stack pronto:
  Frontend:  http://localhost:4200
  API:       http://localhost:8080
  Swagger:   http://localhost:8080/swagger-ui.html

Credenziali demo:
  mario.rossi@demo.it / Demo1234!
  giulia.bianchi@demo.it / Demo1234!

Per fermare tutto: ./avvia-tutto.sh --stop
EOF
}

porta_occupata() {
    if command -v ss >/dev/null 2>&1; then
        ss -ltn 2>/dev/null | awk '{print $4}' | grep -qE "[:.]${1}$"
    else
        return 1
    fi
}

RICOSTRUISCI=0

case "${1:-}" in
    --stop)
        docker compose down
        exit 0
        ;;
    --logs)
        docker compose logs -f
        exit 0
        ;;
    --build)
        RICOSTRUISCI=1
        ;;
    --help|-h)
        mostra_uso
        exit 0
        ;;
    "")
        ;;
    *)
        errore "opzione sconosciuta: $1"
        mostra_uso
        exit 1
        ;;
esac

if [ ! -f docker-compose.yml ]; then
    errore "docker-compose.yml non trovato in $RADICE."
    exit 1
fi

if [ ! -f "$RADICE/../appbancaria-web/Dockerfile" ]; then
    errore "il repository appbancaria-web non e accanto a $(basename "$RADICE")."
    echo "  Clona appbancaria-api e appbancaria-web nella stessa cartella e riprova."
    exit 1
fi

if ! command -v docker >/dev/null 2>&1; then
    errore "Docker non e installato o non e nel PATH."
    exit 1
fi

if ! docker info >/dev/null 2>&1; then
    errore "Docker non e attivo. Avvia il servizio Docker e riprova."
    exit 1
fi

if ! docker compose version >/dev/null 2>&1; then
    errore "Docker Compose v2 non e disponibile."
    exit 1
fi

if [ -z "$(docker compose ps -q 2>/dev/null)" ]; then
    for porta in 8080 4200 3306; do
        if porta_occupata "$porta"; then
            errore "la porta $porta e gia in uso."
            case "$porta" in
                8080)
                    echo "  Ferma l'altro processo sulla 8080 (per esempio il progetto esame:"
                    echo "  docker stop esame_frontend esame_api esame_db) e riprova."
                    ;;
                4200)
                    echo "  Ferma il dev server Angular (ng serve) e riprova."
                    ;;
                3306)
                    echo "  Ferma l'altro MySQL o libera la porta 3306 e riprova."
                    ;;
            esac
            exit 1
        fi
    done
fi

echo "Avvio dello stack AppBancaria (MySQL + API + frontend)..."
if [ "$RICOSTRUISCI" -eq 1 ]; then
    docker compose up -d --build
else
    docker compose up -d
fi

echo "Attendo che i servizi siano pronti (massimo 180 secondi)..."
for _ in $(seq 1 60); do
    api="$(docker inspect -f '{{.State.Health.Status}}' appbancaria-api 2>/dev/null || true)"
    web="$(docker inspect -f '{{.State.Health.Status}}' appbancaria-web 2>/dev/null || true)"
    if [ "$api" = "healthy" ] && [ "$web" = "healthy" ]; then
        mostra_indirizzi
        exit 0
    fi
    sleep 3
done

errore "lo stack non e diventato sano entro 180 secondi."
docker compose ps
docker compose logs --tail=40
exit 1
