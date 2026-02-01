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
CONDITION=""
FLAVOR=""
LANGUAGE=""
OPEN_URL=""
ACTION_LABEL=""
ACTION_URL=""
OPEN_URL=""
ACTION_LABEL=""
ACTION_URL=""
MIN_VERSION=""
BELOW_VERSION=""
EXACT_VERSION=""
DEBUG_ONLY=0
RELEASE_ONLY=0
ALL_TARGET=0

usage() {
  cat >&2 <<'EOF'
Usage:
  scripts/message.sh -t="Title" -m="Message" [-a="https://..."]

Targeting flags (combine as needed):
  --all                       Send to "all" or "updates" topics
  --topic=updates             Send to a specific topic
  --flavor=play|manual         Target build flavor topic
  --language=en|en-US         Target language topic (lang_en, lang_en_us)
  --min-version=11200         Target version and above (v_ge_11200)
  --below-version=11200       Target versions below (NOT v_ge_11200)
  --exact-version=11600       Target a single version (v_11600)
  --debug-only                Target debug builds (build_debug)
  --release-only              Target release builds (build_release)
  --condition="..."           Raw FCM condition (overrides flags)

Notification options:
  -t, -m                      Title and message body (required)
  -a, --action                Legacy action URL (opens when user taps action button)
  --open-url=URL              Override tap action for the notification
  --action-label=TEXT         Action button label
  --action-url=URL            Action button URL (preferred over -a)

Environment:
  FCM_PROJECT_ID              Firebase project ID
  FCM_ACCESS_TOKEN            OAuth token (optional if gcloud is available)
  FCM_TOPIC                   Default topic if --topic not provided
  GOOGLE_APPLICATION_CREDENTIALS  Service account JSON (optional, defaults to ./firebase-admin.json)

Default topics (subscribed in app):
  all
  updates
  flavor_play | flavor_manual
  build_debug | build_release
  v_<versionCode>             e.g., v_11600
  v_ge_<versionGate>          e.g., v_ge_11200 (gates configured in app)
  lang_<tag>                  e.g., lang_en, lang_en_us

Examples:
  # Send to everyone
  scripts/message.sh -t="Hi" -m="Hello" --all

  # Send to play users on 1.12 or higher
  scripts/message.sh -t="Update" -m="..." --flavor=play --min-version=11200

  # Send to users below 1.12 (all flavors)
  scripts/message.sh -t="Important" -m="..." --below-version=11200

  # Send to debug builds only
  scripts/message.sh -t="Debug" -m="..." --debug-only

  # Send to Croatian play users
  scripts/message.sh -t="Bok" -m="..." --flavor=play --language=hr

  # Open a URL when tapping, with an action button
  scripts/message.sh -t="Read" -m="Open the article" \
    --open-url="https://example.com" \
    --action-label="View" --action-url="https://example.com"

  # Use a custom condition
  scripts/message.sh -t="Promo" -m="..." \
    --condition="('flavor_play' in topics) && ('lang_en' in topics) && !('v_ge_11500' in topics)"
EOF
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
    --open-url=*)
      OPEN_URL="${1#*=}"
      ;;
    --open-url)
      OPEN_URL="${2:-}"
      shift
      ;;
    --action-label=*)
      ACTION_LABEL="${1#*=}"
      ;;
    --action-label)
      ACTION_LABEL="${2:-}"
      shift
      ;;
    --action-url=*)
      ACTION_URL="${1#*=}"
      ;;
    --action-url)
      ACTION_URL="${2:-}"
      shift
      ;;
    --topic=*)
      TOPIC="${1#*=}"
      ;;
    --topic)
      TOPIC="${2:-}"
      shift
      ;;
    --all)
      ALL_TARGET=1
      ;;
    --flavor=*)
      FLAVOR="${1#*=}"
      ;;
    --flavor)
      FLAVOR="${2:-}"
      shift
      ;;
    --language=*)
      LANGUAGE="${1#*=}"
      ;;
    --language)
      LANGUAGE="${2:-}"
      shift
      ;;
    --min-version=*)
      MIN_VERSION="${1#*=}"
      ;;
    --min-version)
      MIN_VERSION="${2:-}"
      shift
      ;;
    --below-version=*)
      BELOW_VERSION="${1#*=}"
      ;;
    --below-version)
      BELOW_VERSION="${2:-}"
      shift
      ;;
    --exact-version=*)
      EXACT_VERSION="${1#*=}"
      ;;
    --exact-version)
      EXACT_VERSION="${2:-}"
      shift
      ;;
    --debug-only)
      DEBUG_ONLY=1
      ;;
    --release-only)
      RELEASE_ONLY=1
      ;;
    --condition=*)
      CONDITION="${1#*=}"
      ;;
    --condition)
      CONDITION="${2:-}"
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

BASE_TARGET=""
if [[ "$ALL_TARGET" == "1" ]]; then
  BASE_TARGET="('all' in topics) || ('updates' in topics)"
elif [[ -n "$TOPIC" ]]; then
  BASE_TARGET="'${TOPIC}' in topics"
fi

if [[ -z "$CONDITION" ]]; then
  CONDITIONS=()
  if [[ -n "$BASE_TARGET" ]]; then
    CONDITIONS+=("$BASE_TARGET")
  fi
  if [[ -n "$FLAVOR" ]]; then
    CONDITIONS+=("'flavor_${FLAVOR}' in topics")
  fi
  if [[ -n "$LANGUAGE" ]]; then
    LANG_TOPIC="${LANGUAGE,,}"
    LANG_TOPIC="${LANG_TOPIC//-/_}"
    CONDITIONS+=("'lang_${LANG_TOPIC}' in topics")
  fi
  if [[ -n "$MIN_VERSION" ]]; then
    CONDITIONS+=("'v_ge_${MIN_VERSION}' in topics")
  fi
  if [[ -n "$EXACT_VERSION" ]]; then
    CONDITIONS+=("'v_${EXACT_VERSION}' in topics")
  fi
  if [[ -n "$BELOW_VERSION" ]]; then
    CONDITIONS+=("!('v_ge_${BELOW_VERSION}' in topics)")
  fi
  if [[ "$DEBUG_ONLY" == "1" ]]; then
    CONDITIONS+=("'build_debug' in topics")
  fi
  if [[ "$RELEASE_ONLY" == "1" ]]; then
    CONDITIONS+=("'build_release' in topics")
  fi
  if [[ "${#CONDITIONS[@]}" -gt 0 ]]; then
    CONDITION="$(printf "%s" "${CONDITIONS[0]}")"
    for ((i=1; i<${#CONDITIONS[@]}; i++)); do
      CONDITION="${CONDITION} && ${CONDITIONS[$i]}"
    done
  fi
fi

build_data() {
  jq -n \
    --arg title "$TITLE" \
    --arg body "$MESSAGE" \
    --arg action "$ACTION" \
    --arg open_url "$OPEN_URL" \
    --arg action_label "$ACTION_LABEL" \
    --arg action_url "$ACTION_URL" \
    '
    ({title: $title, body: $body}
      + (if $action != "" then {action: $action} else {} end)
      + (if $open_url != "" then {open_url: $open_url} else {} end)
      + (if $action_label != "" then {action_label: $action_label} else {} end)
      + (if $action_url != "" then {action_url: $action_url} else {} end)
    )
    '
}

if [[ -z "$CONDITION" && "$BASE_TARGET" =~ ^\'[^\']+\'\ in\ topics$ ]]; then
  PAYLOAD="$(
    jq -n --arg topic "$TOPIC" --argjson data "$(build_data)" '
      {
        message: {
          topic: $topic,
          data: $data,
          android: {priority: "HIGH"}
        }
      }'
  )"
else
  PAYLOAD="$(
    jq -n --arg condition "$CONDITION" --argjson data "$(build_data)" '
      {
        message: {
          condition: $condition,
          data: $data,
          android: {priority: "HIGH"}
        }
      }'
  )"
fi

curl -sS -X POST \
  -H "Authorization: Bearer ${ACCESS_TOKEN}" \
  -H "Content-Type: application/json; UTF-8" \
  "https://fcm.googleapis.com/v1/projects/${FCM_PROJECT_ID}/messages:send" \
  -d "${PAYLOAD}"
