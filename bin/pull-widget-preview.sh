#!/bin/sh
# Pulls the BBQ Timer's widget preview image (snapshotted by the Widget Preview app) from the
# Android emulator.

adb -e pull sdcard/Download/BBQ_Timer_ori_portrait.png widget_preview.png
adb -e shell rm sdcard/Download/BBQ_Timer_ori_portrait.png
echo 'Now move widget_preview.png to app/src/main/res/drawable-???/'
