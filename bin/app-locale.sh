#!/usr/bin/env bash
# Set the app-specific locale; default: reset the app to follow the system locale.
#
# Arguments:
#   $1 -- locale, e.g. "en", "en-US", "de", "de-DE", or "" (reset the app to the system locale)
#   $2 -- optional flag like "-d" to force it to use an ADB USB Device,
#         or TODO: "-s emulator-5554" to pick a specific emulator.
#         Defaults to "-e".

PACKAGE="com.onefishtwo.bbqtimer"
LOCALE="${1:-}"
TIP=""

if [ -z "$1" ]; then TIP=" (reset the app to the system locale)"; fi

echo "Running: cmd locale set-app-locales --locales \"${LOCALE}\" ${TIP}"

adb "${2:--e}" shell cmd locale set-app-locales "${PACKAGE}" --locales "${LOCALE}"
