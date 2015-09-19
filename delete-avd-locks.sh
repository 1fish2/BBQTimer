#!/bin/sh
# Deletes all the .lock files left over from crashed Android emulators.
# This enables you to start them up again.

cd ~/.android/avd/
rm -rf */*.lock
