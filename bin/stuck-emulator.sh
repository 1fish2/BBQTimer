#!/bin/sh
# Send KEYCODE_MENU to the emulator to (hopefully) get it to respond to the
# physical keyboard and mouse again.

adb -e shell input keyevent 82
