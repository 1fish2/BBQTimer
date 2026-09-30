#!/bin/sh
# Open an SSH tunnel to the phone emulator `emulator-5554`.
adb -s emulator-5554 forward tcp:5601 tcp:5601
