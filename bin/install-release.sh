#!/bin/sh
# Installs the release APK on a connected device or emulator.
#
# Arguments:
#   $1 -- optional flags like "-r" to reinstall.

adb install ${1-} BBQ\ Timer/build/outputs/apk/BBQ\ Timer-release.apk
