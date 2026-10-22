#!/usr/bin/env bash
# Broadcasts a standard Intent to the app to test its response, using root access to work for
# protected Intents.
#
# Requires: An AOSP or Google APIs System Image. Does not support a Google Play System Image.
#
# Ways to send a protected Intent:
#   adb root; adb shell am broadcast -a ACTION PACKAGE; adb unroot
# or
#   adb shell su root am broadcast -a ACTION PACKAGE
# or
#   echo "am broadcast -a ACTION PACKAGE" | adb shell su
#
# Cf. http://developer.android.com/tools/help/shell.html#IntentSpec
#
# TIME_SET can be tested without root via: `cmd alarm set-time`.
#
# TIMEZONE_CHANGED can be tested without root via: `cmd alarm set-timezone`, or
# `setprop persist.sys.timezone`, or `settings put global time_zone` depending on OS version.
#
# Arguments:
#   $1 -- android.intent.action.* action like TIME_SET, TIMEZONE_CHANGED, or BOOT_COMPLETED.
#         Default = MY_PACKAGE_REPLACED.
#   $2 -- optional device_serial, e.g. emulator-5554

set -euo pipefail

# Ensure adb is installed
if ! command -v adb &>/dev/null; then
  echo "Error: 'adb' command not found. Ensure adb is in your PATH." >&2
  exit 1
fi

ACTION="android.intent.action.${1:-MY_PACKAGE_REPLACED}"
PACKAGE="${PACKAGE:-com.onefishtwo.bbqtimer}"
SERIAL="${2:-${ANDROID_SERIAL:-}}"

if [ -n "$SERIAL" ]; then
  ADB="adb -s $SERIAL"
  ID=" ($SERIAL)"
else
  ADB="adb"
  ID=""
fi

echo "Broadcasting on Android Emulator${ID}: am broadcast -a" "${ACTION}" "${PACKAGE}"

$ADB shell su root am broadcast -a "${ACTION}" "${PACKAGE}"
