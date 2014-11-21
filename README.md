# BBQTimer Android app

[See it in the Google Play store](https://play.google.com/store/apps/details?id=com.onefishtwo.bbqtimer)

A stopwatch with a twist (or two):

1. **Lock screen notifications** (on Android Lollipop) and a **lock screen widget** (on Jelly Bean and
   KitKat) for quick access without unlocking your phone or tablet.
2. **Periodic alarms** remind you to check the food -- rather than a one-shot alarm that assumes the
   food will be done. It’s a count-up stopwatch with timer alarms.

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
* On Android Lollipop, just start or pause BBQ Timer from the application or its home screen widget.
  You can operate it from the notification drawer and the lock screen as long as it's running or
  paused.
* On Jelly Bean and KitKat, place the BBQ Timer widget on your lock screen for quick access.
* The widget also works on the home screen.
* To resize the home screen widget, long-press on it then drag its resize handles.
  Stretched wider, it can show longer durations.
* To remove the widget, long-press it then drag it onto “X Remove”.
* Tap the stopwatch time display to cycle between *stopped* → *running* → *paused* → *stopped.*
* On the app’s main screen, tap the checkbox to turn on periodic reminder alarms.
* On the app’s main screen, set the interval between periodic reminder alarms by
  dragging the number widget, or tap the number then type in a new number, or tap and hold on the
  number above or below the current setting.

## How to Use Android Lock Screen Widgets
* This feature requires Android 4.2 Jelly Bean MR1 through Android 4.4 KitKat. Not Lollipop.
* On KitKat, you must turn on Settings → Security → Enable Widgets to enable this feature.
* For step-by-step instructions, see [Getting started with lock screen widgets on Android Jelly
  Bean](http://howto.cnet.com/8301-11310_39-57549747-285/getting-started-with-lock-screen-widgets-on-android-jelly-bean/
  “CNET How To”)
  or [How to Add Lockscreen Widgets to Android 4.4
  KitKat](http://www.gottabemobile.com/2013/11/11/add-lockscreen-widgets-android-4-4-kitkat-nexus-5/
  “GottaBe Mobile”).
* In short, to add a lock screen widget on Jelly Bean or KitKat:
    * On Android 4.4 KitKat, turn on Settings → Security → Enable Widgets.
    * Wake the screen.
    * Swipe from the left edge of the screen to the right until you see the “+” screen.
    * Tap “+”.
    * Unlock the phone.
    * Tap the BBQ Timer widget.
    * (Optional) To move the BBQ Timer widget to the *main* (rightmost) lock screen position, swipe
      down over it to select it, then long-press it and drag it to the right, past the other
      widgets.
* When you wake the Android screen, it shows the main (rightmost) lock screen widget.
  To access your other lock screen widgets, swipe rightwards.
* To rearrange your lock screen widgets, long-press one and drag it left or right.
* You can rearrange lock screen widgets when you cook (moving BBQ Timer to the rightmost
  position) and afterwards, move the Digital Clock there.

## Known Issues
* Due to [Android OS bug 2880](https://code.google.com/p/android/issues/detail?id=2880), if the
  clock gets set backwards, the widget’s date display might not update until the clock catches up to
  what was going to be the next day.
* Not yet tested on Android API level 13 (HONEYCOMB_MR2) or level 14 (ICE_CREAM_SANDWICH) where the
  emulator takes most of an hour to launch, then croaks. If you have those versions of
  Android, let me know how it goes.
* There’s a fraction of a second clock skew between the stopwatch time displayed in widgets vs. the
  notification area. To see precise times, open the application’s main screen.
* Android emulator bugs: The app’s timer display sometimes stops changing. In emulator API level 12,
  the notification area shows black text on a black background.

## TODO
* Use available space on tablets. Add screen shots for 10" tablets.
* Display the time larger in the activity when the screen is large enough.
* Add a menu of recipes to set the reminder alarm time and suggest the cooking temp.
* Add a Setting to pick {sound, vibrate, both, disable} for reminders.
* Add a Setting to pick the reminder alarm sound.
* Localize into several languages.
* Accessibility.
* Use a font-resizing text view fit long durations in the Activity. See
  [Stack Overflow auto-scale-textview-text-to-fit-within-bounds](http://stackoverflow.com/questions/5033012/auto-scale-textview-text-to-fit-within-bounds/),
  [Stack Overflow how-to-adjust-text-font-size-to-fit-textview](http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview/),
  [Stack Overflow how-to-catch-widget-size-changes-on-devices-where-onappwidgetoptionschanged-not](http://stackoverflow.com/questions/17396045/how-to-catch-widget-size-changes-on-devices-where-onappwidgetoptionschanged-not)
* Use a font-resizing heuristic to fit hours in a minimum width home screen widget without eliding
  the seconds.
* Unit tests.

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
