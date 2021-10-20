# BBQTimer Android app

[See it in the Google Play store](https://play.google.com/store/apps/details?id=com.onefishtwo.bbqtimer)

An interval timer with a twist:

1. **Lock screen operation** for quick access via *lock screen notifications* (Android 5.0 Lollipop+) or a *lock screen widget* (Android 4.2 Jelly Bean to 4.4 KitKat) without unlocking your phone.
2. **Periodic alarms** to check/turn your food while it tracks the total cooking time. (A “stopwatch” won’t alert you while a “timer” won’t track the total time while you turn the burgers, check their temperature, and give them more time as needed.)
3. **Adjustable alarm interval** while it’s running.

Of course it can time more things than cooking.

Requires no special permissions.
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
* Within the app, tap the checkbox to turn the periodic reminder alarms on/off.

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

## Notification Channel Settings (Android 8.0+)

In Android 8.0 (Oreo), you can use the system Settings to adjust notification sounds. Beware that
some changes will disable the app's periodic alarm sounds or lock screen features. Settings:

* Apps &amp; notifications / notifications / On the lock screen: **Show all notification content**
* Apps &amp; notifications / App info / BBQ Timer / App notifications / **On**
* Apps &amp; notifications / App info / BBQ Timer / App notifications / **Alarm** - **On**
  * This notification channel is used to play the alarm sound, and it remains to display the timer
    and the pause/reset/stop controls in the notification area and on the lock screen.
  * **Importance** - must be **Urgent** to enable the “heads up” display from the pull-down notification bar;
    **High** or higher to enable alarm sounds;
    **Medium** or higher to appear on the lock screen and in the notification area.
  * **Sound** - you can pick any sound, but don't pick **None** if you want to hear the app’s alarms.
  * **Vibrate** and **Blink light** - you can change these alarm properties.
  * **On the lock screen** - **Show all notification content** to operate the timer from the lock screen.
* Apps &amp; notifications / App info / BBQ Timer / App notifications / **Controls** - **On**
  * This notification channel is used to display the timer and the pause/reset/stop controls in the
    notification area and on the lock screen when the app doesn’t need to play the alarm sound.
  * **Importance** - must be **Medium** or higher to appear on the lock screen and in the notification area.
  * **On the lock screen** - **Show all notification content** to operate the timer from the lock screen.

The system alarm volume controls the volume of the app’s alarms. If you turn it all the way down,
the app won’t sound audible alarms. It will still vibrate and blink the notification LED, assuming
those settings are enabled.

## License

[MIT License](https://github.com/1fish2/BBQTimer/blob/master/LICENSE.md).

## Notes
* There’s a small clock skew between the stopwatch time displayed in widgets vs. the
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

## Keywords
BBQ, cooking, interval timer, lock screen widget, reminder alarm, stopwatch.

## Implementation notes
* The lock screen widget and notification use a Chronometer view to show the ticking time
display while using little battery power.
* When paused, a Chronometer view can’t show the right time value because its API doesn’t
handle that case. You could fudge it but it'll be off a bit and multiple widgets would
display different times. The workaround is to switch to a Text view when paused.
* Formatting the elapsed time like 0:12 would look nicer than 00:12 but the latter matches Android’s Chronometer view for consistency.
* The widget’s ViewFlipper must explicitly set android:measureAllChildren="false", otherwise
flipping its subviews will resize the adjacent ImageButton on Galaxy Nexus Jelly Bean.
