# BBQTimer Android app

[See it in the Google Play store](https://play.google.com/store/apps/details?id=com.onefishtwo.bbqtimer)

A timer with:

1. **Lock screen operation** for quick access via *lock screen notifications* (Android 5.0+) or *lock screen widgets* (Android 4.2 to 4.4).
2. **Periodic alarms** remind you to check/turn the food while it counts up the total cooking time.

Of course it can time more things than cooking.

Requires few permissions.  
No network access. No ads. No data gathering.  
Simple and focused.  
Free.

## System Requirements

BBQ Timer is for phones and tablets running Android 4.0+ (Ice Cream Sandwich and later).

If it encounters problems on your Android device, email me the details (device model, Android
version, screen size, problem symptom, Android "bugreport" file).

## Usage Tips
* Android 5.0+: While BBQ Timer is running or paused, you can operate it from the lock screen and the pull-down notification.
* On Android 4.2 to 4.4, place the BBQ Timer widget on your lock screen for quick access.
* The widget also works on the home screen.
* To stretch out the widget to show longer durations, long-press it then drag its resize handles.
* To remove the widget, long-press it then drag it onto “X Remove”.
* Tap the stopwatch time display to cycle between *stopped* → *running* → *paused* → *stopped.*
* On the app’s main screen, tap the checkbox to turn periodic reminder alarms on/off.

## To add a lock screen widget (Android 4.2 to 4.4)
* On Android 4.4, first enable Settings → Security → Enable Widgets.
* Wake the screen.
* Swipe from the left edge of the lock screen to the right until you get the “+” screen.
* Tap “+”.
* Unlock the phone.
* Tap the BBQ Timer widget.
* (Optional) Drag lock screen widgets left/right to rearrange them.
* See [Getting started with lock screen widgets on Android Jelly Bean](http://howto.cnet.com/8301-11310_39-57549747-285/getting-started-with-lock-screen-widgets-on-android-jelly-bean/)
  or [How to Add Lockscreen Widgets to Android 4.4 KitKat](http://www.gottabemobile.com/2013/11/11/add-lockscreen-widgets-android-4-4-kitkat-nexus-5/).

## License

[MIT License](https://github.com/1fish2/BBQTimer/blob/master/LICENSE.md).

## Known Issues
* There’s a fraction of a second clock skew between the stopwatch time displayed in widgets vs. the
  notification area. For precise times, open the application’s main screen.
* On Android N, at Font Size Largest and Display Size Default, the text gets clipped off the bottom of the home screen widget.
* The app can’t install in external storage (SD card) because Android deletes
external storage app widgets when you use USB to share files with a computer.

## TODO
* Warn when alarms are silenced by device settings.
* A voice action to start a timer.
* A menu of recipes to set the reminder alarm time and suggest the cooking temp.
* Localize into more languages.
* Accessibility.
* A font-resizing text view to fit long durations in the Activity. See
  [Stack Overflow: Auto Scale TextView Text to Fit within Bounds](http://stackoverflow.com/questions/5033012/auto-scale-textview-text-to-fit-within-bounds/),
  [How to adjust text font size to fit textview](http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview/),
  [How to catch widget size changes](http://stackoverflow.com/questions/17396045/how-to-catch-widget-size-changes-on-devices-where-onappwidgetoptionschanged-not).
* Use a font-resizing heuristic to fit hours in a minimum width home screen widget without eliding
  the seconds.
* Unit tests.
* Material Design shadows.

## Building from Source Code
Use [Android Studio](http://developer.android.com/sdk/installing/studio.html) (an excellent tool!).

## Dedication
Dedicated to open source software developers.

Open source software used for this project includes: Android, Android Studio, Audacity, Chrome,
Gimp, git, git gui, Inkscape, Java, MinGW, Proguard, and much more that we use without thinking
about, like zlib.

Don’t get me wrong -- commercial software is also great!

## Media Asset Sources
Notification sound composed from sampled cowbell sounds which are used by permission from Phil Burk,
Copyright (c) 2014 Mobileer Inc.

Launcher icon derived from a public domain image on [openclipart.org](http://openclipart.org).

## Keywords
Cooking, interval timer, lock screen widget, reminder alarm, stopwatch.

## Implementation notes
* The lock screen widget and notification need to use a Chronometer view to show the ticking time
display while using little battery power.
* When paused, a Chronometer view can’t show the right time value because its API doesn’t
accommodate that case. You could fake it by sending it a start time that’s about the right amount of
time ago but you can’t control how long until it reads the system clock. Multiple widgets would
display different paused time values. The workaround is to switch to a Text view when paused.
* Formatting the elapsed time like 0:12 would look nicer than 00:12 but the app's activity matches Android’s Chronometer view for consistency.
* The widget’s ViewFlipper must explicitly set android:measureAllChildren="false", otherwise
flipping its subviews will resize the adjacent ImageButton on Galaxy Nexus Jelly Bean.
