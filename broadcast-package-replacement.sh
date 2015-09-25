#!/bin/sh
# Broadcasts a MY_PACKAGE_REPLACED Intent to the app to test its response.
#
# Arguments:
#   $1 -- optional flags like "-d" to force it to use an ADB USB Device.

adb shell am broadcast -a android.intent.action.MY_PACKAGE_REPLACED \
    -n com.onefishtwo.bbqtimer/.ResumeReceiver
