#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT_DIR"

GRADLE_ARGS=()
if [[ -n "${MANUAL_SIGNER_SHA256:-}" ]]; then
  GRADLE_ARGS+=("-PmanualSignerSha256=${MANUAL_SIGNER_SHA256}")
fi

./gradlew "${GRADLE_ARGS[@]}" assembleManualRelease
