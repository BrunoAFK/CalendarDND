#!/usr/bin/env bash
set -euo pipefail

if [[ -f .env ]]; then
  set -a
  # shellcheck disable=SC1091
  source .env
  set +a
fi

TITLE=""
MESSAGE=""
ACTION=""
TOPIC="${FCM_TOPIC:-updates}"

usage() {
  echo "Usage: $0 -t=\"Title\" -m=\"Message\" [-a=\"https://...\"]" >&2
  echo "Env: FCM_PROJECT_ID, FCM_ACCESS_TOKEN (optional if gcloud is available), FCM_TOPIC (optional)" >&2
  echo "Env: GOOGLE_APPLICATION_CREDENTIALS (optional, defaults to ./firebase-admin.json if present)" >&2
  echo "Tip: create .env in repo root with those values." >&2
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    -t=*)
      TITLE="${1#*=}"
      ;;
    -t)
      TITLE="${2:-}"
      shift
      ;;
    -m=*)
      MESSAGE="${1#*=}"
      ;;
    -m)
      MESSAGE="${2:-}"
      shift
      ;;
    -a=*)
      ACTION="${1#*=}"
      ;;
    -a)
      ACTION="${2:-}"
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown arg: $1" >&2
      usage
      exit 1
      ;;
  esac
  shift
done

if [[ -z "$TITLE" || -z "$MESSAGE" ]]; then
  usage
  exit 1
fi

if [[ -z "${FCM_PROJECT_ID:-}" ]]; then
  echo "Missing FCM_PROJECT_ID." >&2
  usage
  exit 1
fi

if [[ -n "${FCM_ACCESS_TOKEN:-}" ]]; then
  ACCESS_TOKEN="$FCM_ACCESS_TOKEN"
elif command -v gcloud >/dev/null 2>&1; then
  ACCESS_TOKEN="$(gcloud auth print-access-token)"
else
  if [[ -z "${GOOGLE_APPLICATION_CREDENTIALS:-}" && -f "firebase-admin.json" ]]; then
    GOOGLE_APPLICATION_CREDENTIALS="firebase-admin.json"
  fi
  if [[ -z "${GOOGLE_APPLICATION_CREDENTIALS:-}" && -f "firebase-admin.json" ]]; then
    GOOGLE_APPLICATION_CREDENTIALS="firebase-admin.json"
  fi
  if [[ -n "${GOOGLE_APPLICATION_CREDENTIALS:-}" ]]; then
    if [[ ! -f "$GOOGLE_APPLICATION_CREDENTIALS" ]]; then
      echo "Service account JSON not found at: $GOOGLE_APPLICATION_CREDENTIALS" >&2
      exit 1
    fi
    if [[ ! -s "$GOOGLE_APPLICATION_CREDENTIALS" ]]; then
      echo "Service account JSON is empty: $GOOGLE_APPLICATION_CREDENTIALS" >&2
      exit 1
    fi
    if [[ "${DEBUG:-0}" == "1" ]]; then
      FILE_SIZE="$(wc -c < "$GOOGLE_APPLICATION_CREDENTIALS" | tr -d ' ')"
      echo "Using service account JSON: $GOOGLE_APPLICATION_CREDENTIALS (${FILE_SIZE} bytes)" >&2
      echo -n "First bytes: " >&2
      head -c 64 "$GOOGLE_APPLICATION_CREDENTIALS" | sed $'s/\\r/\\\\r/g' | tr '\n' '\\n' >&2
      echo >&2
      echo -n "BOM check (hex): " >&2
      head -c 3 "$GOOGLE_APPLICATION_CREDENTIALS" | od -An -t x1 | tr -d ' \n' >&2
      echo >&2
    fi
    if ! command -v jq >/dev/null 2>&1; then
      echo "jq is required to mint an access token from service account JSON." >&2
      exit 1
    fi
    if ! command -v openssl >/dev/null 2>&1; then
      echo "openssl is required to mint an access token from service account JSON." >&2
      exit 1
    fi
    CLIENT_EMAIL="$(jq -r '.client_email' "$GOOGLE_APPLICATION_CREDENTIALS")"
    PRIVATE_KEY="$(jq -r '.private_key' "$GOOGLE_APPLICATION_CREDENTIALS")"
    if [[ -z "$CLIENT_EMAIL" || -z "$PRIVATE_KEY" || "$CLIENT_EMAIL" == "null" || "$PRIVATE_KEY" == "null" ]]; then
      echo "Service account JSON is missing client_email or private_key." >&2
      exit 1
    fi
    IAT="$(date +%s)"
    EXP="$((IAT + 3600))"
    HEADER_B64="$(printf '{"alg":"RS256","typ":"JWT"}' | openssl base64 -A | tr '+/' '-_' | tr -d '=')"
    CLAIMS_B64="$(
      printf '{"iss":"%s","scope":"https://www.googleapis.com/auth/firebase.messaging","aud":"https://oauth2.googleapis.com/token","iat":%s,"exp":%s}' \
        "$CLIENT_EMAIL" "$IAT" "$EXP" \
        | openssl base64 -A | tr '+/' '-_' | tr -d '='
    )"
    UNSIGNED_JWT="${HEADER_B64}.${CLAIMS_B64}"
    TMP_KEY="$(mktemp)"
    printf '%b' "$PRIVATE_KEY" > "$TMP_KEY"
    SIGNATURE="$(printf '%s' "$UNSIGNED_JWT" | openssl dgst -sha256 -sign "$TMP_KEY" | openssl base64 -A | tr '+/' '-_' | tr -d '=')"
    JWT_ASSERTION="${UNSIGNED_JWT}.${SIGNATURE}"
    ACCESS_TOKEN="$(
      curl -sS -X POST https://oauth2.googleapis.com/token \
        -H "Content-Type: application/x-www-form-urlencoded" \
        -d "grant_type=urn:ietf:params:oauth:grant-type:jwt-bearer&assertion=${JWT_ASSERTION}" \
        | jq -r '.access_token'
    )"
    rm -f "$TMP_KEY"
    if [[ -z "$ACCESS_TOKEN" || "$ACCESS_TOKEN" == "null" ]]; then
      echo "Failed to fetch access token." >&2
      exit 1
    fi
  else
    echo "Missing FCM_ACCESS_TOKEN, gcloud, and GOOGLE_APPLICATION_CREDENTIALS." >&2
    usage
    exit 1
  fi
fi

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required to build the FCM payload." >&2
  exit 1
fi

PAYLOAD="$(
  jq -n --arg topic "$TOPIC" --arg title "$TITLE" --arg body "$MESSAGE" --arg action "$ACTION" '
    {
      message: {
        topic: $topic,
        data: ({title: $title, body: $body} + (if $action != "" then {action: $action} else {} end)),
        android: {priority: "HIGH"}
      }
    }'
)"

curl -sS -X POST \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json; UTF-8" \
  "https://fcm.googleapis.com/v1/projects/${FCM_PROJECT_ID}/messages:send" \
  -d "${PAYLOAD}"
