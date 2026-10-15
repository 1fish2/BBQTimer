#!/usr/bin/env bash
# Deletes all the .lock files left over from crashed Android emulators.
# This enables you to start them up again.

cd ~/.android/avd/ || exit
rm -rf -- */*.lock
