#!/bin/sh
# Set animations OFF or to the optional speed scale ("1.0" is normal) in the
# currently connected Android emulator.
# Turning animations OFF makes Espresso tests a little faster and more stable.
# https://medium.com/stepstone-tech/taming-the-ui-test-monster-26c017848ae0

SCALE="${1:-0.0}"

echo window_animation_scale=`adb -e shell settings get global window_animation_scale`
echo transition_animation_scale=`adb -e shell settings get global transition_animation_scale`
echo animator_duration_scale=`adb -e shell settings get global animator_duration_scale`

echo
echo Setting Android animation time scale to $SCALE
echo

adb -e shell settings put global window_animation_scale $SCALE
adb -e shell settings put global transition_animation_scale $SCALE
adb -e shell settings put global animator_duration_scale $SCALE

echo window_animation_scale=`adb -e shell settings get global window_animation_scale`
echo transition_animation_scale=`adb -e shell settings get global transition_animation_scale`
echo animator_duration_scale=`adb -e shell settings get global animator_duration_scale`
