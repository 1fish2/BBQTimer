# BBQTimer Android app

A stopwatch with a twist (or two):

1. A lock screen widget lets you quickly operate the timer without unlocking your phone.
2. Periodic reminder alarms (like count-down timers) while counting up the elapsed time.
   Remind you to check the food, however long it actually takes to cook.

Requires few permissions. No network access. No ads.

## Status
Not yet tested on various OS releases and screen sizes.
See the **TODO** list, below.

BBQ Timer is for Android 3.1 (HONEYCOMB_MR1) and later. If it has problems on your phone or tablet
running Android 3.1 or later, send me the details (device type, Android version, screen size,
problem symptom, and an Android "Bug Report" file if the app crashed).

## Usage Tips
* Put the BBQ Timer widget on the Android lock screen for quick access (if you have Android 4.2
  Jelly Bean or later).
* The BBQ Timer widget also works on the home screen (even in earlier versions of Android).
* You can resize the home screen widget by long-pressing it, then dragging its resize handles.
  The initial size fits minutes and seconds (e.g. "18:12"). Stretched wider, it can fit hours,
  minutes, and seconds (e.g. "1:15:09"). This may vary with your screen size and font size settings.
* Tap the stopwatch time display to cycle between Reset -> Running -> Paused -> Reset.
* Tap the "Periodic alarms this many minutes apart" number if you want to set it by typing a number.

## How to Use Android Lock Screen Widgets
* Android 4.2 Jelly Bean and later versions support lock screen widgets.
* In Android 4.4 KitKat, you must turn on Settings > Security > Enable Widgets to use that feature.
* For step-by-step instructions, see [Getting started with lock screen widgets on Android Jelly
  Bean](http://howto.cnet.com/8301-11310_39-57549747-285/getting-started-with-lock-screen-widgets-on-android-jelly-bean/
  "CNET How To")
  and [How to Add Lockscreen Widgets to Android 4.4
  KitKat](http://www.gottabemobile.com/2013/11/11/add-lockscreen-widgets-android-4-4-kitkat-nexus-5/
  "GottaBe Mobile").
* In short: To add a lock screen widget: wake the screen, then swipe rightwards until you see the
  "+" screen, tap the "+", unlock the phone, pick a widget, then (optionally) drag it further to the
  right.
* To rearrange your lock screen widgets, long-press on one and drag it left or right.
* The rightmost widget appears as soon as you wake up the screen.
* You can rearrange your lock screen widgets any time you want. Example: Move BBQ Timer to the
  rightmost position when you're cooking, then move the Digital Clock widget to the rightmost
  position until your next BBQ.

## TODO
* Set other AndroidManifest.xml values? (Search info?)
* Deploy to the Play store.

* Show a tip about adding the widget to the lock screen when first running the app.
* Show tips via a menu command: How to install as home & lock screen widgets. What tapping the
  widget text does.
* Add a large notification icon: mdpi 64x64 px, hdpi 96x96 px, xhdpi 128x128 px,
  xxhpdi 192x192 px.
* Add a Settings dialog to pick {sound, vibrate, both, disable} for reminder alarms.
* Add a Settings dialog to pick the reminder alarm sound.
* Add button actions (pause/resume/reset) to the notification area?
* Adapt the activity layout to tablet screens (esp. landscape). Subclass Checkbox so it doesn't grow
  wider than needed for the box + label?

* Support Android Wear.

* When stopped at 00:00, add the time of day to the widget? ... after a few minutes?
  (To track the time: Upon ACTION_SCREEN_ON when there are lock screen widgets, register a Service to
  listen for ACTION_TIME_TICK. Unregister upon ACTION_SCREEN_OFF or when the last widget is removed.)
* L10N.
* Accessibility.
* Use a font-resizing text view fit long durations into the Activity. See
  [Stack Overflow auto-scale-textview-text-to-fit-within-bounds](http://stackoverflow.com/questions/5033012/auto-scale-textview-text-to-fit-within-bounds/),
  [Stack Overflow how-to-adjust-text-font-size-to-fit-textview](http://stackoverflow.com/questions/2617266/how-to-adjust-text-font-size-to-fit-textview/),
  [Stack Overflow how-to-catch-widget-size-changes-on-devices-where-onappwidgetoptionschanged-not](http://stackoverflow.com/questions/17396045/how-to-catch-widget-size-changes-on-devices-where-onappwidgetoptionschanged-not)
* Use a font-resizing heuristic to fit hours into a minimum width home screen widget without eliding
  the seconds.

* Unit tests.

## Known Issues
* Due to Android OS bug https://code.google.com/p/android/issues/detail?id=2880 if the clock gets
  set backwards, the widget's date might not update until the clock catches up to when it was going
  to be the next day.
* In the emulator (Nexus 5 KitKat), the Activity's timer sometimes stops.
* On Android emulator v12 (HONEYCOMB_MR1), trying to show the alarm count in the notification area
  crashes. This is a bug in the NotificationCompat library. To work around this, I made the app not
  try to display the count on builds older than v15 (ICE_CREAM_SANDWICH_MR1), where it works fine.
* On Android emulator builds v12 to v16 (JELLY_BEAN), the checkbox overlaps its label
  "Periodic alarms this many minutes apart:". Just an emulator bug?
* On Android emulator v12, the notification area tries to show black text on a black background.
  Just an emulator bug?
* On Android emulator v15 (ICE_CREAM_SANDWICH_MR1), the notification area displays the stopwatch's
  start time instead of its elapsed time.
* Not yet tested on Android v13 (HONEYCOMB_MR2).
* Not yet tested on Android v14 (ICE_CREAM_SANDWICH). The emulator takes most of an hour to launch,
  then croaks. Every time.
* Tests fine on Android v17 ()JELLY_BEAN_MR1, v18 (JELLY_BEAN_MR2), v19 (KITKAT).

## To Build from Source Code
Using [Android Studio](http://developer.android.com/sdk/installing/studio.html) (an awesome tool!).

## Dedication
Dedicated to open source software developers.

Open source software used for this project includes: Android, Android Studio, Audacity, Chrome,
Gimp, git, git gui, Inkscape, Java, MinGW, Proguard, and much more that we use without thinking
about, like zlib.

Don't get me wrong -- commercial software is also great!

## Sources
Notification sound composed from sampled cowbell sounds, used by permission from Phil Burk,
Copyright (c) 2014 Mobileer Inc.

The launcher icon is derived from a public domain image on [openclipart.org](openclipart.org).

## Keywords
Alarm, cook, cooking, home screen widget, interval timer, lock screen widget, stopwatch.

## Implementation notes
* The lock screen widget pretty much needs to use a Chronometer view for the ticking time display.
Using a Text view would show clock skew between multiple widgets and would use more CPU time and
battery power.
* When paused, a Chronometer view can't show a well-determined time value because its API doesn't
accommodate that case. You could fake it by sending it a start time that's about the right amount of
time ago but you can't control how long until it reads the system clock. Consequently, multiple
widgets would display different paused time values. The workaround is to switch the display from a
Chronometer view to a Text view when paused. A simpler workaround would pause the Chronometer then
use it as the paused Text view, but that relies on the Chronometer's implementation details.
* Formatting the elapsed time like 0:12 would look nicer than 00:12 but Android's Chronometer view
only does the latter. The app's main activity screen and the paused Text view don't have to match it
but the inconsistency would be jarring.
* The widget's ViewFlipper must explicitly set android:measureAllChildren="false", otherwise
flipping its subviews will resize the adjacent ImageButton on Galaxy Nexus Jelly Bean. (Why?)
