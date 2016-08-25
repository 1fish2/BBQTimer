#!/bin/sh
# Set the current Android emulator or device into the German-Germany locale.
#
# Arguments:
#   $1 -- optional flag like "-d" to force it to use an ADB USB Device.
#         Defaults to "-e".

adb "${1:--e}" shell "setprop persist.sys.locale de-DE;stop;sleep 5;start"
