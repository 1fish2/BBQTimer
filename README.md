# BBQTimer Android app

It's a stopwatch with a twist or two:
1. It has a lock screen widget so you can check the timer without unlocking your phone.
2. [TODO] It can set alarm times (like a count-down timer) while still counting up the time that the food has been cooking, however long that takes.

## Status
In development. Not ready for alpha test.

## Build
Using Android Studio.

## TODO
* Switch to a TextView when stopped (or see if there's a way to make a stopped Chronometer display a consistent value).
* Add Start/Stop and Reset buttons to the widget.
* Make the widget prettier: icon instead of "BBQ", margins, background, translucent widget background frame.
* Show the date and time in the widget. (Only on the lock screen? Vertically resizable?)
* Add a widget preview image.
* Figure out minSdkVersion. (It goes in "build.gradle".) App Widgets were introduced in level 3.
* Set other AndroidManifest.xml values. (Search info?)
* App icons.
* Use image buttons for Start/Stop & Reset.
* Add the alarm feature. Use, replace, or remove the Settings menu.
* Display a notification for the alarms so people can tell why it beeped.
* Implement app thumbnail.
* Provide color feedback when tapping on the time display text view, like the system stopwatch app.
* Follow system theme colors?
