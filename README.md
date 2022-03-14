# BBQTimer Android app

[See it in the Google Play store](https://play.google.com/store/apps/details?id=com.onefishtwo.bbqtimer)

BBQ Timer is a combined interval timer/stopwatch:
1. Quick access via a **lock screen notification** (no need to unlock your phone) and a home screen widget.
2. **Periodic alarms** to check/turn your food while it also tracks the total time. (A count-up “stopwatch app” won’t alert you and a countdown “timer app” won’t track the total time as you turn the food and check its temperature.) BBQ Timer displays the reminder alarm timer and the total time.
3. **Adjustable reminder alarm period** while it’s running.

Of course it can time more things than cooking, like interval exercises.

* Requires minimal app permissions.  
* No network access. No ads. No data collection.  
* Simple and focused.  
* Free.


## System Requirements

BBQ Timer is for phones and tablets running Android 6.0+ (Lollipop) and later.

If it encounters problems on your Android device, email me the details (device model, Android
version, screen size, problem symptom, Android "bugreport" file).


## Usage Tips
* While BBQ Timer is *running* or *paused*, you can operate it **from the lock screen** or the pull-down notification. To put it on the lock screen when the Timer is *stopped*, tap the **Pause** or **Play** buttons in the app, or the **Play** button in the home screen widget, or (on Android 7.1+) long-press the app’s home screen icon then tap the “Pause at 00:00” shortcut.
* Try the home screen widget.
* Resize the widget to see more or less info: long-press it then drag its resize handles.
* To remove the widget, long-press and drag it onto “× Remove”.
* Tap the time display to cycle between *stopped* → *running* → *paused* → *stopped.*
* In the app, tap the checkbox to turn on/off the periodic reminder alarms.
* Tap the widget's background or its countdown time to open the app.
* Enter a periodic alarm interval in any of these formats: *10* \[10 minutes], *7:30* \[7 minutes, 30 seconds], *3:15:00* \[3 hours, 15 minutes]. *12:00* can be shortened to *12:0* or *12:* or *12*. *0:09* can be shortened to *:9*. *2:00:00* \[2 hours] can be shortened to *2:0:0* or *2::* or *120*.
* The app, widget, and notification display the the countdown interval time as well as the total time (requires Android 7+).

**Note: These System Settings are required to hear and see BBQTimer alarms:**
* “Alarm volume” at an audible level.
* Lock screen - Show all or non-private notifications.
* BBQTimer “Show notifications”, *not* Silent. (You may also choose to “Override Do Not Disturb“.)
* BBQTimer “Alarm” notification category - “Show notifications”, *not* “Silent”, “Make sound and pop on screen”, sound choice *not* “None”, Importance “High” or higher to hear and see on the lock screen and in the notification area.
* Apps - Special app access - Alarms & reminders - Allowed.


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
BBQ, cooking, interval timer, home screen widget, reminder alarm, stopwatch.


## Implementation notes
* When paused, a Chronometer view can’t show the right time value because its API doesn’t
handle that case. A Chronometer also ignores its format string when stopped. The workaround is to switch to a Text view when paused.
* Formatting the elapsed time like 0:12 would look nicer than 00:12 but the latter matches Android’s Chronometer view for consistency.
* The widget’s ViewFlipper must explicitly set android:measureAllChildren="false", otherwise
flipping its subviews will resize the adjacent ImageButton on Galaxy Nexus Jelly Bean.


[Privacy Policy](Privacy-Policy-for-the-BBQTimer-app.md).
