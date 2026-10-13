#!/usr/bin/env bash
#
# Smart Island (2026)
# © Animesh Gupta — github.com/agupta07505
# Licensed under the GNU GPL v3 License
#
# Forwards build artifacts (APKs) to a Telegram channel or forum topic community.
#

set -euo pipefail

APK_PATH="${1:-}"
BUILD_TYPE="${2:-Release}"
CUSTOM_TOPIC="${3:-}"

if [[ -z "${TELEGRAM_BOT_TOKEN:-}" || -z "${TELEGRAM_CHAT_ID:-}" ]]; then
  echo "::notice ::Telegram secrets (TELEGRAM_BOT_TOKEN / TELEGRAM_CHAT_ID) are not configured. Skipping artifact forward."
  exit 0
fi

if [[ -z "$APK_PATH" || ! -f "$APK_PATH" ]]; then
  echo "::error ::Artifact file not found: '$APK_PATH'"
  exit 1
fi

FILE_NAME="$(basename "$APK_PATH")"
FILE_SIZE="$(du -h "$APK_PATH" | cut -f1)"
REPO="${GITHUB_REPOSITORY:-agupta07505/SmartIsland}"
RUN_ID="${GITHUB_RUN_ID:-}"
RUN_URL="https://github.com/${REPO}/actions/runs/${RUN_ID}"
COMMIT_SHA="${GITHUB_SHA:-}"
SHORT_SHA="${COMMIT_SHA:0:7}"
BRANCH_OR_TAG="${GITHUB_REF_NAME:-}"
VERSION="${VERSION_TAG:-${BRANCH_OR_TAG}}"

# Extract and escape last commit message
RAW_COMMIT_MSG="$(git log -1 --pretty=format:'%s' 2>/dev/null || echo '')"
if [[ ${#RAW_COMMIT_MSG} -gt 120 ]]; then
  RAW_COMMIT_MSG="${RAW_COMMIT_MSG:0:117}..."
fi

escape_html() {
  local str="$1"
  str="${str//&/&amp;}"
  str="${str//</&lt;}"
  str="${str//>/&gt;}"
  echo "$str"
}

COMMIT_MSG="$(escape_html "$RAW_COMMIT_MSG")"

# Determine target topic (if any)
TARGET_TOPIC="$CUSTOM_TOPIC"
if [[ -z "$TARGET_TOPIC" ]]; then
  if [[ "$BUILD_TYPE" =~ [Dd]ebug ]]; then
    TARGET_TOPIC="${TELEGRAM_DEBUG_TOPIC_ID:-${TELEGRAM_TOPIC_ID:-}}"
  else
    TARGET_TOPIC="${TELEGRAM_RELEASE_TOPIC_ID:-${TELEGRAM_TOPIC_ID:-}}"
  fi
fi

# Build HTML caption
if [[ "$BUILD_TYPE" =~ [Rr]elease ]]; then
  CAPTION="🚀 <b>Smart Island ${VERSION}</b> (Release)

📦 <b>File:</b> <code>${FILE_NAME}</code> (${FILE_SIZE})
🏷 <b>Version:</b> <code>${VERSION}</code>
📝 <b>Commit:</b> <a href=\"https://github.com/${REPO}/commit/${COMMIT_SHA}\">${SHORT_SHA}</a> ${COMMIT_MSG}
🔗 <a href=\"https://github.com/${REPO}/releases/tag/${VERSION}\">GitHub Release</a> • <a href=\"${RUN_URL}\">Workflow Run</a>"
else
  CAPTION="🛠 <b>Smart Island Build</b> (${BUILD_TYPE})

📦 <b>File:</b> <code>${FILE_NAME}</code> (${FILE_SIZE})
🌿 <b>Branch:</b> <code>${BRANCH_OR_TAG}</code>
📝 <b>Commit:</b> <a href=\"https://github.com/${REPO}/commit/${COMMIT_SHA}\">${SHORT_SHA}</a> ${COMMIT_MSG}
🔗 <a href=\"${RUN_URL}\">GitHub Actions Run</a>"
fi

# Normalize and trim secrets
TELEGRAM_BOT_TOKEN="$(echo "${TELEGRAM_BOT_TOKEN}" | tr -d '[:space:]')"
CHAT_ID="$(echo "${TELEGRAM_CHAT_ID}" | tr -d '[:space:]')"
TARGET_TOPIC="$(echo "${TARGET_TOPIC:-}" | tr -d '[:space:]')"

# Normalize CHAT_ID: if numeric and missing leading '-' (e.g. copied from web URL https://t.me/c/<id>/<topic>),
# automatically prefix -100 to target the Telegram supergroup properly.
if [[ "$CHAT_ID" =~ ^[0-9]{9,}$ ]]; then
  CHAT_ID="-100${CHAT_ID}"
fi

REAL_APK_PATH="$(realpath "$APK_PATH" 2>/dev/null || echo "$APK_PATH")"

echo "Uploading $FILE_NAME ($FILE_SIZE) to Telegram chat: $CHAT_ID (Topic: ${TARGET_TOPIC:-General})..."

# Prepare curl arguments
# Use --form-string for text fields so curl never misinterprets values starting with @ or < as local files.
CURL_ARGS=(
  -s -S
  --connect-timeout 30
  --max-time 300
  -X POST "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/sendDocument"
  --form-string "chat_id=${CHAT_ID}"
  -F "document=@${REAL_APK_PATH}"
  --form-string "caption=${CAPTION}"
  --form-string "parse_mode=HTML"
)

if [[ -n "${TARGET_TOPIC:-}" ]]; then
  CURL_ARGS+=(--form-string "message_thread_id=${TARGET_TOPIC}")
fi

RESPONSE=$(curl "${CURL_ARGS[@]}")

if echo "$RESPONSE" | grep -q '"ok":true'; then
  echo "::notice ::Artifact successfully forwarded to Telegram!"
else
  echo "::error ::Telegram API error: $RESPONSE"
  exit 1
fi
