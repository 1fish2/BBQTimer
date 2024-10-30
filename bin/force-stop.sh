#!/bin/sh
# Force Stop the app on a connected device or emulator.
#
# Arguments:
#   $1 -- optional flag like "-d" to force it to use an ADB USB Device.
#         Defaults to "-e".

adb "${1:--e}" shell am force-stop com.onefishtwo.bbqtimer
