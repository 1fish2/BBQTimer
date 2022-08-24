#!/bin/sh
# Set Notification permissions as when the user enabled the app's Notifications on Android 12L- then
# upgraded to Android 13+.

adb shell pm grant com.onefishtwo.bbqtimer android.permission.POST_NOTIFICATIONS
adb shell pm clear-permission-flags com.onefishtwo.bbqtimer android.permission.POST_NOTIFICATIONS user-set
adb shell pm clear-permission-flags com.onefishtwo.bbqtimer android.permission.POST_NOTIFICATIONS user-fixed
