#!/usr/bin/env bash
# Set the app-specific locale; default: reset the app to follow the system locale.
#
# Arguments:
#   $1 -- locale, e.g. "en", "en-US", "de", "de-DE", or "" (reset the app to the system locale)
#   $2 -- optional device_serial, e.g. emulator-5554

set -euo pipefail

# Ensure adb is installed
if ! command -v adb &>/dev/null; then
  echo "Error: 'adb' command not found. Ensure adb is in your PATH." >&2
  exit 1
fi

PACKAGE="${PACKAGE:-com.onefishtwo.bbqtimer}"
LOCALE="${1:-}"
SERIAL="${2:-${ANDROID_SERIAL:-}}"

if [ -n "$SERIAL" ]; then
  ADB="adb -s $SERIAL"
  ID=" ($SERIAL)"
else
  ADB="adb"
  ID=""
fi

SDK_INT=$($ADB shell getprop ro.build.version.sdk | tr -d '\r')

if [ "$SDK_INT" -lt 33 ]; then
  echo "Error: Per-app locale ('cmd locale') requires Android 13+ (API 33). Target device is API ${SDK_INT}." >&2
  exit 1
fi

if [ -z "$LOCALE" ]; then
  DISPLAY_LOCALE="[system locale]"
else
  DISPLAY_LOCALE="'${LOCALE}'"
fi

echo "==> Setting '${PACKAGE}' locale on Android Emulator${ID} to ${DISPLAY_LOCALE} ..."

$ADB shell cmd locale set-app-locales "${PACKAGE}" --locales "${LOCALE}"
