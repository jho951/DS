#!/usr/bin/env bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${SCRIPT_DIR}/docker-compose.yml"

case "$1" in
  all)
    echo "🚀 Build + Up + Logs 시작..."
    docker compose -f "$COMPOSE_FILE" build --no-cache
    docker compose -f "$COMPOSE_FILE" up -d
    echo "✅ 컨테이너 기동 완료, 로그 출력 중..."
    docker logs drawing-app --tail=200 -f
    ;;
  build)
    docker compose -f "$COMPOSE_FILE" build
    ;;
  up)
    docker compose -f "$COMPOSE_FILE" up -d
    ;;
  down)
    docker compose -f "$COMPOSE_FILE" down
    ;;
  logs)
    docker compose -f "$COMPOSE_FILE" logs -f
    ;;
  restart)
    docker compose -f "$COMPOSE_FILE" down
    docker compose -f "$COMPOSE_FILE" up -d
    ;;
  nuke)
    docker compose -f "$COMPOSE_FILE" down -v
    docker image prune -f
    ;;
  *)
    echo "Usage: $0 {all|build|up|down|logs|restart|nuke}"
    exit 1
    ;;
esac
