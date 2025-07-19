#!/bin/sh
# Set animations OFF or to the optional speed scale ("1.0" is normal) in the
# connected or identified (by serial) Android emulator.
# Turning animations OFF makes Espresso tests a little faster and more stable.
# https://medium.com/stepstone-tech/taming-the-ui-test-monster-26c017848ae0
#
# Args:
# * $1: speed scale, defaults to "0.0" meaning OFF, "1.0" is normal
# * $2: the device serial number, e.g. "emulator-5564"

SCALE="${1:-0.0}"
SN="${2:-}"

echo window_animation_scale=`adb -e -s "$SN" shell settings get global window_animation_scale`
echo transition_animation_scale=`adb -e -s "$SN" shell settings get global transition_animation_scale`
echo animator_duration_scale=`adb -e -s "$SN" shell settings get global animator_duration_scale`

echo
echo Setting Android animation time scale to $SCALE
echo

adb -e -s "$SN" shell settings put global window_animation_scale $SCALE
adb -e -s "$SN" shell settings put global transition_animation_scale $SCALE
adb -e -s "$SN" shell settings put global animator_duration_scale $SCALE

echo window_animation_scale=`adb -e -s "$SN" shell settings get global window_animation_scale`
echo transition_animation_scale=`adb -e -s "$SN" shell settings get global transition_animation_scale`
echo animator_duration_scale=`adb -e -s "$SN" shell settings get global animator_duration_scale`
