#!/usr/bin/env bash
# Open the Settings app

set -euo pipefail

adb shell am start -a android.settings.SETTINGS
