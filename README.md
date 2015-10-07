# BBQTimer Android app

[See it in the Google Play store](https://play.google.com/store/apps/details?id=com.onefishtwo.bbqtimer)

A stopwatch with:

1. **Lock screen operation** for quick access via a *lock screen notification* (Android
   Lollipop 5 on) or a *lock screen widget* (Jelly Bean 4.2 to KitKat 4.4).
2. **Periodic alarms** to remind you to check the food (instead of a one-shot alarm that assumes
   the food will be done). It’s a count-up stopwatch with alarms.

Of course it can time more things than cooking.

Requires few permissions.  
No network access. No ads. No data gathering.  
Simple and focused.  
Free.

## System Requirements

BBQ Timer is for phones and tablets running Android 3.1 (Honeycomb MR1) and later.

If it encounters problems on your Android device, email me the details (device model, Android
version, screen size, problem symptom, Android "bugreport" file).

[Note: The app can’t install in external storage (SD card) because Android deletes
external storage app widgets every time you use USB to share files with a computer.]

## Status

Released.

## License

[MIT License](https://github.com/1fish2/BBQTimer/blob/master/LICENSE.md).

## Usage Tips
* Android Lollipop on: Start BBQ Timer from the application or its home screen widget.
  You can operate it from the lock screen and the pull-down notification while it's running or
  paused.
* Jelly Bean and KitKat: Put the BBQ Timer widget on your lock screen for quick access.
  (Instructions below.)
* The widget also works on the home screen.
* Tap the timer display to cycle between *stopped* → *running* → *paused* → *stopped.*
* If you long-press the home screen widget, you can stretch it to fit longer durations.
* On the app’s main screen, tap the checkbox to turn on periodic reminder alarms. Set the
  interval between reminders by dragging the number widget, or tap the number then
  type in a new number, or press and hold the number above or below the current value.

## How to Use Android Lock Screen Widgets
* Only Android 4.2 Jelly Bean MR1 through Android 4.4 KitKat can do this.
* On KitKat, first enable Settings → Security → Enable Widgets.
* For step-by-step instructions, see [Getting started with lock screen widgets on Android Jelly
  Bean](http://howto.cnet.com/8301-11310_39-57549747-285/getting-started-with-lock-screen-widgets-on-android-jelly-bean/
  “CNET How To”)
  or [How to Add Lockscreen Widgets to Android 4.4
  KitKat](http://www.gottabemobile.com/2013/11/11/add-lockscreen-widgets-android-4-4-kitkat-nexus-5/
  “GottaBe Mobile”).
* In short, to add a lock screen widget on Jelly Bean or KitKat:
    * On Android 4.4 KitKat, turn on Settings → Security → Enable Widgets.
    * Wake the screen.
    * Swipe from the left edge of the screen towards the right until you see the “+” screen.
    * Tap “+”.
    * Unlock the phone.
    * Tap the BBQ Timer widget.
    * (Optional) To move the BBQ Timer widget to the *main* (rightmost) lock screen position, swipe
      down over it to select it, then long-press it and drag it to the right, past the other
      widgets.
* When you wake the Android screen, it shows the main (rightmost) lock screen widget.
  To access your other lock screen widgets, swipe rightwards.
* To rearrange your lock screen widgets, long-press one and drag it left or right. E.g. when you
  cook, move BBQ Timer to the rightmost position. Afterwards, move the Digital Clock there.

## Known Issues
* Due to [Android OS bug 2880](https://code.google.com/p/android/issues/detail?id=2880), if the
  clock gets set backwards, the widget’s date display might not update until the clock catches up to
  what was going to be the next day.
* Not yet tested on Android API level 13 (HONEYCOMB_MR2) or level 14 (ICE_CREAM_SANDWICH) where the
  emulator takes most of an hour to launch, then croaks.
* There’s a fraction of a second clock skew between the stopwatch time displayed in widgets vs. the
  notification area. To see precise times, open the application’s main screen.
* Android emulator bugs: The app’s timer display sometimes stops changing. In emulator API level 12,
  the notification area shows black text on a black background.

## TODO
* Display something in the activity when the alarm rings. Flash a background color or image? Show a notification despite being in the Activity?
* Add a voice action to start a timer.
* Add a menu of recipes to set the reminder alarm time and suggest the cooking temp.
* Localize into several languages.
* Accessibility.
* Use a font-resizing text view fit long durations in the Activity. See
  [Stack Overflow auto-scale-textview-text-to-fit-within-bounds](http://stackoverflow.com/questions/5033012/auto-scale-textview-text-to-fit-within-bounds/),
  [Stack Overflow how-to-adjust-text-font-size-to-fit-textview](http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview/),
  [Stack Overflow how-to-catch-widget-size-changes-on-devices-where-onappwidgetoptionschanged-not](http://stackoverflow.com/questions/17396045/how-to-catch-widget-size-changes-on-devices-where-onappwidgetoptionschanged-not)
* Use a font-resizing heuristic to fit hours in a minimum width home screen widget without eliding
  the seconds.
* Unit tests.
* Add Material Design shadows.

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
display without using a lot of battery power.
* When paused, a Chronometer view can’t show the right time value because its API doesn’t
accommodate that case. You could fake it by sending it a start time that’s about the right amount of
time ago but you can’t control how long until it reads the system clock. Multiple widgets would
display different paused time values. The workaround is to switch to a Text view when paused.
* Formatting the elapsed time like 0:12 would look nicer than 00:12 but Android’s Chronometer view
always does the latter. The app’s main screen and its paused text widget match it for consistency.
* The widget’s ViewFlipper must explicitly set android:measureAllChildren="false", otherwise
flipping its subviews will resize the adjacent ImageButton on Galaxy Nexus Jelly Bean. (Why?)
