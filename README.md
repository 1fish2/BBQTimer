# BBQTimer Android app

[See it in the Google Play store](https://play.google.com/store/apps/details?id=com.onefishtwo.bbqtimer)

A timer with:

1. **Lock screen operation** for quick access via *lock screen notifications* (Android 5.0+ Lollipop+) or *lock screen widgets* (Android 4.2 Jelly Bean to 4.4 KitKat).
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
* Android 5.0+ (Lollipop+): While BBQ Timer is *running* or *paused*, you can operate it from the lock screen and the pull-down notification. If the Timer is *stopped*, just tap the **Pause** or **Play** button within the app, or the **Play** button in the home screen widget, or the timer text in either place.
* On Android 4.2 Jelly Bean to 4.4 KitKat, place the BBQ Timer widget on your lock screen for quick access.
* The widget also works on the home screen.
* To stretch out the widget to show longer durations, long-press it then drag its resize handles.
* To remove the widget, long-press it then drag it onto “X Remove”.
* Tap the stopwatch time display to cycle between *stopped* → *running* → *paused* → *stopped.*
* Within the app, tap the checkbox to turn periodic reminder alarms on/off.

## To put a widget on your lock screen (Android 4.2 to 4.4)
1. On Android 4.4 KitKat, first enable Settings → Security → Enable Widgets.
2. Wake the screen.
3. Swipe from the left edge of the lock screen to the right until you get the “+” screen.
4. Tap “+”.
5. Unlock the phone.
6. Tap the BBQ Timer widget.
7. (Optional) Drag lock screen widgets left/right to rearrange them.
8. (Further info) See [Getting started with lock screen widgets on Android Jelly Bean](http://howto.cnet.com/8301-11310_39-57549747-285/getting-started-with-lock-screen-widgets-on-android-jelly-bean/)
  or [How to Add Lockscreen Widgets to Android 4.4 KitKat](http://www.gottabemobile.com/2013/11/11/add-lockscreen-widgets-android-4-4-kitkat-nexus-5/).

## License

[MIT License](https://github.com/1fish2/BBQTimer/blob/master/LICENSE.md).

## Known Issues
* There’s a fraction of a second clock skew between the stopwatch time displayed in widgets vs. the
  notification area. For precise times, open the application’s main screen.
* At Font Size Largest in Display Size Default, the home screen widget's text gets clipped at the bottom.
* The app can’t install in external storage (SD card) because Android deletes
external storage app widgets when you use USB to share files with a computer.

## Building from Source Code
Use [Android Studio](http://developer.android.com/sdk/installing/studio.html) (an excellent tool!).

## Dedication
Dedicated to open source software developers.

Open source software used for this project includes: Android, Android Studio, ART, Audacity, Chrome,
Dalvik, Gimp, git, git gui, Gradle, Inkscape, MinGW, Proguard, and much more that we use without thinking
about, like libpng and zlib.

Don’t get me wrong -- commercial software is also great!

## Media Asset Sources
Notification sound composed from sampled cowbell sounds which are used by permission from Phil Burk,
Copyright (c) 2014 Mobileer Inc.

Launcher icon derived from a public domain image on [openclipart.org](http://openclipart.org).

## Keywords
BBQ, cooking, interval timer, lock screen widget, reminder alarm, stopwatch.

## Implementation notes
* The lock screen widget and notification need to use a Chronometer view to show the ticking time
display while using little battery power.
* When paused, a Chronometer view can’t show the right time value because its API doesn’t
handle that case. You could fudge it but it'll be off a bit and multiple widgets would
display different times. The workaround is to switch to a Text view when paused.
* Formatting the elapsed time like 0:12 would look nicer than 00:12 but the latter matches Android’s Chronometer view for consistency.
* The widget’s ViewFlipper must explicitly set android:measureAllChildren="false", otherwise
flipping its subviews will resize the adjacent ImageButton on Galaxy Nexus Jelly Bean.
