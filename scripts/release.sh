#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/.." && pwd)"

print_usage() {
  cat <<'EOF'
Usage: scripts/release.sh

This script:
- Bumps versionName/versionCode
- Runs ./gradlew assembleDebug, test, lint, assembleManualRelease
- Ensures release-notes/<version>.md exists (must be ready)
- Creates a git tag v<version>
- Generates update.json
- Pushes commits + tags
- Creates a GitHub release and uploads the APK + update.json
EOF
}

if [[ $# -ne 0 ]]; then
  print_usage
  exit 1
fi

generated_dir="${repo_root}/release-notes/generated"
update_json="${generated_dir}/update.json"

mkdir -p "${generated_dir}"

ensure_jq() {
  if command -v jq >/dev/null 2>&1; then
    return
  fi
  echo "jq not found. Installing..."
  install_package jq
}

ensure_gh() {
  if command -v gh >/dev/null 2>&1; then
    return
  fi
  echo "gh not found. Installing..."
  install_package gh
}

install_package() {
  local pkg="$1"
  local os=""
  if [[ -f /etc/os-release ]]; then
    os="$(. /etc/os-release && echo "${ID}")"
  else
    os="$(uname -s)"
  fi

  case "${os}" in
    ubuntu|debian)
      sudo apt-get update
      sudo apt-get install -y "${pkg}"
      ;;
    fedora)
      sudo dnf install -y "${pkg}"
      ;;
    darwin|Darwin)
      if ! command -v brew >/dev/null 2>&1; then
        echo "Homebrew is required. Install it from https://brew.sh/ and rerun."
        exit 1
      fi
      brew install "${pkg}"
      ;;
    *)
      echo "Unsupported OS for auto-install. Please install ${pkg} manually."
      exit 1
      ;;
  esac
}

ensure_jq
ensure_gh

cd "${repo_root}"

ensure_python() {
  if command -v python3 >/dev/null 2>&1; then
    return
  fi
  echo "python3 not found. Please install Python 3 and rerun."
  exit 1
}

ensure_python

compute_sha256() {
  local file="$1"
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "${file}" | awk '{print $1}'
    return
  fi
  if command -v shasum >/dev/null 2>&1; then
    shasum -a 256 "${file}" | awk '{print $1}'
    return
  fi
  echo "No sha256 tool found. Install sha256sum or shasum."
  exit 1
}

require_signing_props() {
  local props_file="${repo_root}/local.properties"
  local store_file=""
  local store_password=""
  local key_alias=""
  local key_password=""

  if [[ -f "${props_file}" ]]; then
    store_file="$(grep -E '^RELEASE_STORE_FILE=' "${props_file}" | cut -d'=' -f2- || true)"
    store_password="$(grep -E '^RELEASE_STORE_PASSWORD=' "${props_file}" | cut -d'=' -f2- || true)"
    key_alias="$(grep -E '^RELEASE_KEY_ALIAS=' "${props_file}" | cut -d'=' -f2- || true)"
    key_password="$(grep -E '^RELEASE_KEY_PASSWORD=' "${props_file}" | cut -d'=' -f2- || true)"
  fi

  store_file="${store_file:-${RELEASE_STORE_FILE:-}}"
  store_password="${store_password:-${RELEASE_STORE_PASSWORD:-}}"
  key_alias="${key_alias:-${RELEASE_KEY_ALIAS:-}}"
  key_password="${key_password:-${RELEASE_KEY_PASSWORD:-}}"

  if [[ -z "${store_file}" || -z "${store_password}" || -z "${key_alias}" || -z "${key_password}" ]]; then
    echo "Missing release signing properties. Add to local.properties:"
    echo "RELEASE_STORE_FILE=/full/path/to/keystore.jks"
    echo "RELEASE_STORE_PASSWORD=..."
    echo "RELEASE_KEY_ALIAS=..."
    echo "RELEASE_KEY_PASSWORD=..."
    exit 1
  fi

  if [[ ! -f "${store_file}" ]]; then
    echo "Release keystore not found at: ${store_file}"
    exit 1
  fi
}

if ! gh auth status >/dev/null 2>&1; then
  echo "GitHub CLI is not authenticated. Run: gh auth login"
  exit 1
fi

get_latest_version() {
  local tag=""
  tag="$(gh release view --json tagName -q .tagName 2>/dev/null || true)"
  if [[ -z "${tag}" ]]; then
    tag="$(git tag --sort=-v:refname | head -n 1)"
  fi
  tag="${tag#v}"
  if [[ -z "${tag}" ]]; then
    echo ""
  else
    echo "${tag}"
  fi
}

prompt_bump_type() {
  local choice=""
  while true; do
    read -r -p "Bump type (major/minor/patch): " choice
    case "${choice}" in
      major|minor|patch) echo "${choice}"; return ;;
      *) echo "Please enter major, minor, or patch." ;;
    esac
  done
}

prompt_first_version() {
  local choice=""
  while true; do
    read -r -p "No releases found. Enter first version (e.g., 1.0 or 1.0.0): " choice
    if [[ "${choice}" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]]; then
      echo "${choice}"
      return
    fi
    echo "Please enter a valid version like 1.0 or 1.0.0."
  done
}

assert_clean_worktree() {
  local allowed_notes="$1"
  local status
  status="$(git status --porcelain)"
  if [[ -z "${status}" ]]; then
    return
  fi

  local unexpected
  unexpected="$(echo "${status}" | grep -v -E "^[ ?MADRCU]{2} (${allowed_notes})$" || true)"
  if [[ -n "${unexpected}" ]]; then
    echo "Working tree has unrelated changes:"
    echo "${unexpected}"
    exit 1
  fi
}

semver_to_code() {
  local ver="$1"
  IFS='.' read -r major minor patch <<<"${ver}"
  if [[ -z "${major:-}" || -z "${minor:-}" ]]; then
    echo "Invalid version: ${ver}"
    exit 1
  fi
  patch="${patch:-0}"
  echo $((major * 10000 + minor * 100 + patch))
}

bump_version() {
  local ver="$1"
  local bump="$2"
  local parts=2
  IFS='.' read -r major minor patch <<<"${ver}"
  if [[ -n "${patch:-}" ]]; then
    parts=3
  fi
  case "${bump}" in
    major)
      major=$((major + 1))
      minor=0
      patch=0
      ;;
    minor|patch)
      minor=$((minor + 1))
      patch=0
      ;;
  esac
  if [[ "${parts}" -eq 2 ]]; then
    echo "${major}.${minor}"
  else
    echo "${major}.${minor}.${patch}"
  fi
}

update_gradle_versions() {
  local ver="$1"
  local code
  code="$(semver_to_code "${ver}")"
  CODE="${code}" VERSION="${ver}" REPO_ROOT="${repo_root}" python3 - <<'PY'
import os
import re
from pathlib import Path

path = Path(os.environ["REPO_ROOT"]) / "app" / "build.gradle.kts"
text = path.read_text()
code = os.environ["CODE"]
ver = os.environ["VERSION"]
existing_code = re.search(r'versionCode\s*=\s*(\d+)', text)
existing_name = re.search(r'versionName\s*=\s*\"([^\"]+)\"', text)

if existing_code and existing_name:
    current_code = existing_code.group(1)
    current_name = existing_name.group(1)
    if current_code == code and current_name == ver:
        print(f"app/build.gradle.kts already at versionName {ver}, versionCode {code}")
        raise SystemExit(0)

new = text
new = re.sub(r'versionCode\s*=\s*\d+', f'versionCode = {code}', new, count=1)
new = re.sub(r'versionName\s*=\s*\"[^\"]+\"', f'versionName = \"{ver}\"', new, count=1)
if new == text:
    print("Failed to update versionName/versionCode in app/build.gradle.kts")
    print("Found versionCode:", existing_code.group(1) if existing_code else "none")
    print("Found versionName:", existing_name.group(1) if existing_name else "none")
    raise SystemExit(1)
path.write_text(new)
print(f"Updated app/build.gradle.kts to versionName {ver}, versionCode {code}")
PY
}

current_version="$(get_latest_version)"
if [[ -z "${current_version}" ]]; then
  version="$(prompt_first_version)"
else
  bump_type="$(prompt_bump_type)"
  version="$(bump_version "${current_version}" "${bump_type}")"
  echo "Latest release: ${current_version}"
fi

notes_file="${repo_root}/release-notes/${version}.md"
draft_notes="${repo_root}/release-notes/new.md"

if [[ -f "${draft_notes}" && ! -f "${notes_file}" ]]; then
  mv "${draft_notes}" "${notes_file}"
fi

echo "Next version: ${version}"

if [[ ! -s "${notes_file}" ]]; then
  echo "Missing or empty release notes: ${notes_file}"
  echo "Create release-notes/new.md and rerun, or create ${notes_file}."
  exit 1
fi

assert_clean_worktree "release-notes/(new|${version})\\.md|scripts/release.sh"

update_gradle_versions "${version}"
require_signing_props

repo_url="$(git remote get-url origin 2>/dev/null || true)"
if [[ -z "${repo_url}" ]]; then
  echo "No git remote 'origin' found."
  exit 1
fi

owner_repo="$(python3 - <<PY
import re
import sys

url = "${repo_url}"
m = re.match(r'(?:git@|https://)github.com[:/](.+?)(?:\\.git)?$', url)
if not m:
    sys.exit(1)
print(m.group(1))
PY
)"
if [[ -z "${owner_repo}" ]]; then
  echo "Failed to parse GitHub repo from origin: ${repo_url}"
  exit 1
fi

tag="v${version}"
if git rev-parse "${tag}" >/dev/null 2>&1; then
  echo "Tag already exists: ${tag}"
  exit 1
fi

apk_name="CalendarDND-${version}-manual.apk"

./gradlew assembleDebug test lint assembleManualRelease

apk_path="$(ls "${repo_root}"/app/build/outputs/apk/manual/release/app-manual-release*.apk 2>/dev/null | head -n 1 || true)"

if [[ ! -f "${apk_path}" ]]; then
  echo "APK not found after build in app/build/outputs/apk/manual/release"
  exit 1
fi

final_apk_path="${generated_dir}/${apk_name}"
cp "${apk_path}" "${final_apk_path}"
apk_path="${final_apk_path}"
current_sha="$(compute_sha256 "${apk_path}")"

mapfile -t release_files < <(ls "${repo_root}/release-notes/"*.md 2>/dev/null | grep -E '/[0-9]+\.[0-9]+(\.[0-9]+)?\.md$' | sort -V -r)
if [[ ${#release_files[@]} -eq 0 ]]; then
  echo "No release notes found in release-notes/*.md"
  exit 1
fi

release_entries=()
for file in "${release_files[@]}"; do
  file_version="$(basename "${file}" .md)"
  file_notes="$(cat "${file}")"
  file_apk_name="CalendarDND-${file_version}-manual.apk"
  file_apk_url="https://github.com/${owner_repo}/releases/download/v${file_version}/${file_apk_name}"
  file_version_code="$(semver_to_code "${file_version}")"
  sha_value=""
  if [[ "${file_version}" == "${version}" ]]; then
    sha_value="${current_sha}"
  fi
  if [[ -n "${sha_value}" ]]; then
    release_entries+=("$(jq -n --arg versionName "${file_version}" --arg apkUrl "${file_apk_url}" --arg releaseNotes "${file_notes}" --argjson versionCode "${file_version_code}" --arg sha256 "${sha_value}" '{versionName: $versionName, versionCode: $versionCode, apkUrl: $apkUrl, releaseNotes: $releaseNotes, sha256: $sha256}')")
  else
    release_entries+=("$(jq -n --arg versionName "${file_version}" --arg apkUrl "${file_apk_url}" --arg releaseNotes "${file_notes}" --argjson versionCode "${file_version_code}" '{versionName: $versionName, versionCode: $versionCode, apkUrl: $apkUrl, releaseNotes: $releaseNotes}')")
  fi
done

printf '%s\n' "${release_entries[@]}" | jq -s '{releases: .}' > "${update_json}"

git add -A
if git diff --cached --quiet; then
  echo "No changes to commit for version bump or release notes."
else
  git commit -m "Release ${version}"
fi
git tag -a "${tag}" -m "Release ${version}"
git push origin HEAD
git push origin "${tag}"

gh release create "${tag}" \
  --title "${tag}" \
  --notes-file "${notes_file}" \
  "${apk_path}#${apk_name}" \
  "${update_json}#update.json"

echo "Release created: ${tag}"
