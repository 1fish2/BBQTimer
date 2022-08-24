#!/bin/sh
# Set Notification permissions per a newly installed app on Android 13+.

adb shell pm revoke com.onefishtwo.bbqtimer android.permission.POST_NOTIFICATIONS
adb shell pm clear-permission-flags com.onefishtwo.bbqtimer android.permission.POST_NOTIFICATIONS user-set
adb shell pm clear-permission-flags com.onefishtwo.bbqtimer android.permission.POST_NOTIFICATIONS user-fixed
