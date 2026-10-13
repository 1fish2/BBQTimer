#!/usr/bin/env bash
#
# setup_avd.sh
# Automates post-wipe setup for Android Virtual Devices (AVDs).
# Configures system settings, disables touch/tap sounds, sets default volumes,
# disables test-flaking animations, and sets screen lock/timeout preferences.
#
# To open Settings quickly without putting an icon on the home screen:
#    adb shell am start -a android.settings.SETTINGS
# To clear the PIN lock:
#    adb shell locksettings clear --old 1111
#
set -euo pipefail

echo "==> Configuring Android Emulator via ADB..."

# Ensure device is connected and ready
adb wait-for-device

echo "1. Disabling touch, tap, and lockscreen sounds..."
adb shell settings put system sound_effects_enabled 0
adb shell settings put system lockscreen_sounds_enabled 0
adb shell settings put system dtmf_tone_when_dialing 0

echo "2. Clearing default notification, ringtone, and alarm sound URIs..."
adb shell settings delete system notification_sound || true
adb shell settings delete system ringtone || true
adb shell settings delete system alarm_alert || true

echo "3. Muting system/notification streams..."
adb shell settings put system volume_music_speaker 0 || true
adb shell settings put system volume_notification_speaker 0 || true
adb shell settings put system volume_ring_speaker 0 || true
adb shell settings put system volume_system_speaker 0 || true
adb shell settings put system volume_alarm_speaker 0 || true
adb shell cmd audio set-stream-volume 2 0 || true
adb shell cmd audio set-stream-volume 4 0 || true
adb shell cmd audio set-stream-volume 6 0 || true

# echo "4. Disabling window, transition, and animator scales for fast UI testing..."
# adb shell settings put global window_animation_scale 0.0
# adb shell settings put global transition_animation_scale 0.0
# adb shell settings put global animator_duration_scale 0.0

echo "5. Setting screen timeout & stay awake while plugged in..."
adb shell settings put global stay_on_while_plugged_in 3
adb shell settings put system screen_off_timeout 1800000

echo "6. Ensuring default font scale and resetting display density..."
adb shell settings put system font_scale 1.0
adb shell wm density reset

echo "7. Set a PIN lock: 1111"
adb shell locksettings clear --old 1111 || true
adb shell locksettings set-pin 1111

echo "==> AVD setup complete!"
