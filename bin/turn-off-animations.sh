#!/bin/sh
# Turn off animations in the currently connected Android emulator, as recommended
# for Espresso UI tests. It'll make them a little faster and maybe more stable.
# https://medium.com/stepstone-tech/taming-the-ui-test-monster-26c017848ae0

adb -e shell settings put global window_animation_scale 0.0
adb -e shell settings put global transition_animation_scale 0.0
adb -e shell settings put global animator_duration_scale 0.0
