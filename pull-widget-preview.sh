#!/bin/sh
# Pulls the BBQ Timer's widget preview image (snapshotted by the Widget Preview app) from the
# Android emulator.

adb -e pull sdcard/Download/BBQ_Timer_ori_portrait.png BBQ\ Timer/src/main/res/drawable/widget_preview.png
