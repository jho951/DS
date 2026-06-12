#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_PROJECT_NAME="editor-service"
ACTION="${1:-up}"
ENV_NAME="${2:-dev}"
shift $(( $# > 0 ? 1 : 0 )) || true
shift $(( $# > 0 ? 1 : 0 )) || true

usage() {
  echo "Usage: ./scripts/run.docker.sh [up|down|build|logs|ps|restart|nuke] [dev|prod] [docker compose options]" >&2
}

case "$ACTION" in
  up|down|build|logs|ps|restart|nuke) ;;
  *) usage; exit 1 ;;
esac

case "$ENV_NAME" in
  dev|prod)
    COMPOSE_FILE="$PROJECT_ROOT/docker/$ENV_NAME/compose.yml"
    BUILD_COMPOSE_FILE="$PROJECT_ROOT/docker/compose.build.yml"
    ;;
  *) usage; exit 1 ;;
esac

resolve_env_file() {
  local preferred="$PROJECT_ROOT/.env.$ENV_NAME"
  if [[ -f "$preferred" ]]; then
    echo "$preferred"
    return 0
  fi

  local fallback="$PROJECT_ROOT/.env.example"
  if [[ -f "$fallback" ]]; then
    echo "$fallback"
    return 0
  fi

  echo ""
}

gradle_property() {
  local key="$1"
  local gradle_properties="${HOME}/.gradle/gradle.properties"
  [[ -f "$gradle_properties" ]] || return 0
  awk -F= -v key="$key" '$1 == key { print $2; exit }' "$gradle_properties"
}

if [[ -z "${GH_TOKEN:-}" ]]; then
  GH_TOKEN="$(gradle_property githubPackagesToken)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="$(gradle_property githubToken)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="$(gradle_property ghToken)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="$(gradle_property gh_token)"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="${GH_PACKAGES_TOKEN:-}"
  [[ -n "$GH_TOKEN" ]] || GH_TOKEN="${GITHUB_TOKEN:-}"
  export GH_TOKEN
fi

if [[ -z "${GITHUB_TOKEN:-}" && -n "${GH_TOKEN:-}" ]]; then
  export GITHUB_TOKEN="$GH_TOKEN"
fi

if [[ -z "${GITHUB_ACTOR:-}" ]]; then
  GITHUB_ACTOR="$(gradle_property githubPackagesUsername)"
  [[ -n "$GITHUB_ACTOR" ]] || GITHUB_ACTOR="$(gradle_property githubUsername)"
  [[ -n "$GITHUB_ACTOR" ]] || GITHUB_ACTOR="$(gradle_property githubActor)"
  [[ -n "$GITHUB_ACTOR" ]] || GITHUB_ACTOR="${GITHUB_USERNAME:-}"
  export GITHUB_ACTOR
fi

validate_github_packages_credentials() {
  if [[ "$ENV_NAME" != "dev" ]]; then
    return
  fi

  case "$ACTION" in
    up|build|restart) ;;
    *) return ;;
  esac

  if [[ -n "${GH_TOKEN:-}${GITHUB_TOKEN:-}" && -n "${GITHUB_ACTOR:-}" ]]; then
    return
  fi

  cat >&2 <<'EOF'
GitHub Packages 인증 정보가 없어 dev Docker 빌드를 시작할 수 없습니다.

공유 토큰을 쓰지 말고 개인 GitHub PAT(read:packages)를 설정하세요.

권장 위치:
  ~/.gradle/gradle.properties

필수 값:
  githubPackagesUsername=<your-github-id>
  githubPackagesToken=<your-read-packages-token>

또는 현재 셸/ignored .env.dev에 GITHUB_ACTOR, GH_TOKEN을 설정할 수 있습니다.
EOF
  exit 1
}

validate_github_packages_credentials

SHARED_NETWORK="${SERVICE_SHARED_NETWORK:-service-backbone-shared}"
if ! docker network inspect "$SHARED_NETWORK" >/dev/null 2>&1; then
  echo "Creating external network: $SHARED_NETWORK"
  docker network create "$SHARED_NETWORK" >/dev/null
fi

compose() {
  local env_file
  env_file="$(resolve_env_file)"
  if [[ -n "$env_file" ]]; then
    SERVICE_SHARED_NETWORK="$SHARED_NETWORK" EDITOR_ENV_FILE="$env_file" \
      docker compose --env-file "$env_file" -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" "$@"
    return
  fi

  SERVICE_SHARED_NETWORK="$SHARED_NETWORK" docker compose -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" "$@"
}

compose_dev_build() {
  local env_file
  env_file="$(resolve_env_file)"
  if [[ -n "$env_file" ]]; then
    SERVICE_SHARED_NETWORK="$SHARED_NETWORK" EDITOR_ENV_FILE="$env_file" \
      docker compose --env-file "$env_file" -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" -f "$BUILD_COMPOSE_FILE" "$@"
    return
  fi

  SERVICE_SHARED_NETWORK="$SHARED_NETWORK" docker compose -p "$COMPOSE_PROJECT_NAME" -f "$COMPOSE_FILE" -f "$BUILD_COMPOSE_FILE" "$@"
}

case "$ACTION" in
  up)
    if [[ "$ENV_NAME" == "prod" ]]; then
      compose pull "$@"
      compose up -d "$@"
    else
      compose_dev_build up -d --build "$@"
    fi
    ;;
  down) compose down --remove-orphans "$@" ;;
  build)
    if [[ "$ENV_NAME" == "prod" ]]; then
      echo "prod profile is image-only; build is only supported for dev." >&2
      exit 1
    fi
    compose_dev_build build "$@"
    ;;
  logs) compose logs -f "$@" ;;
  ps) compose ps "$@" ;;
  restart)
    compose down --remove-orphans
    if [[ "$ENV_NAME" == "prod" ]]; then
      compose pull "$@"
      compose up -d "$@"
    else
      compose_dev_build up -d --build "$@"
    fi
    ;;
  nuke) compose down --remove-orphans -v "$@" ;;
esac
