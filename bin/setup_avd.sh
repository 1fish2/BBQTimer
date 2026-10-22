#!/usr/bin/env bash
#
# setup_avd.sh
# Automates post-wipe setup for Android Virtual Devices (AVDs).
# Configures system settings, disables touch/tap sounds, sets default volumes,
# and sets screen lock/timeout preferences.
#
# Usage:
#   ./setup_avd.sh [device_serial]
#
# To open Settings quickly without putting an icon on the home screen:
#    adb shell am start -a android.settings.SETTINGS
# To clear the PIN lock:
#    adb shell locksettings clear --old 1111
#
set -euo pipefail

# Ensure adb is installed
if ! command -v adb &>/dev/null; then
  echo "Error: 'adb' command not found. Ensure adb is in your PATH." >&2
  exit 1
fi

# Support optional serial parameter or ANDROID_SERIAL environment variable, e.g. emulator-5554
SERIAL="${1:-${ANDROID_SERIAL:-}}"
if [ -n "$SERIAL" ]; then
  ADB="adb -s $SERIAL"
  echo "==> Configuring Android Emulator ($SERIAL) via ADB..."
else
  ADB="adb"
  echo "==> Configuring Android Emulator via ADB..."
fi

# Ensure device is connected and ready
$ADB wait-for-device

# Wait for full Android system boot completion
echo "==> Waiting for device boot to complete..."
until [ "$($ADB shell getprop sys.boot_completed 2>/dev/null | tr -d '\r')" = "1" ]; do
  sleep 1
done

echo "Disabling touch, tap, and lockscreen sounds,"
echo "Clearing default notification, ringtone, and alarm sound URIs,"
echo "Muting system/notification streams, resettng font size,"
echo "Setting screen timeout & stay awake while charging..." # 7 = AC, USB, & wireless charging
$ADB shell << 'EOF'
  settings put system sound_effects_enabled 0
  settings put system lockscreen_sounds_enabled 0
  settings put system dtmf_tone_when_dialing 0

  settings delete system notification_sound || true
  settings delete system ringtone || true
  settings delete system alarm_alert || true

  settings put system volume_music_speaker 0 || true
  settings put system volume_notification_speaker 0 || true
  settings put system volume_ring_speaker 0 || true
  settings put system volume_system_speaker 0 || true
  settings put system volume_alarm_speaker 0 || true

  cmd audio set-stream-volume 2 0 || true
  cmd audio set-stream-volume 4 0 || true
  cmd audio set-stream-volume 6 0 || true

  settings put system font_scale 1.0

  settings put global stay_on_while_plugged_in 7
  settings put system screen_off_timeout 1800000
EOF

echo "Resetting display density..."
$ADB shell wm density reset

echo "Set a PIN lock: 1111"
$ADB shell locksettings clear --old 1111 || true
$ADB shell locksettings set-pin 1111

echo "==> AVD setup complete!"
