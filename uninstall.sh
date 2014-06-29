#!/bin/sh
# Uninstalls the APK from a connected device or emulator.
#
# Arguments:
#   $1 -- optional flags like "-d" to force it to use an ADB USB Device.

adb "${1-}" uninstall com.onefishtwo.bbqtimer
