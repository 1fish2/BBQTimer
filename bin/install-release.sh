#!/bin/sh
# (Re)Installs the release APK on a connected device or emulator.
#
# Arguments:
#   $1 -- optional flag like "-d" to force it to use an ADB USB Device.
#         Defaults to "-e".

adb "${1:--e}" install -r app/build/outputs/apk/BBQTimer-release.apk
