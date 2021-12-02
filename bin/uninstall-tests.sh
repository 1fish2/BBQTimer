#!/bin/sh
# Uninstalls the app's Android Tests from a connected device or emulator.
#
# Arguments:
#   $1 -- optional flag like "-d" to force it to use an ADB USB Device.
#         Defaults to "-e".
#
# Alternative: `./gradlew connectedCheck` will run the Android Tests then
# uninstall the app and test APKs, but not the orchestrator or TestServices.

adb "${1:--e}" uninstall com.onefishtwo.bbqtimer.test
adb "${1:--e}" uninstall androidx.test.orchestrator
