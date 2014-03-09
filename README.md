# BBQTimer Android app

It's a stopwatch with a twist (or two):
1. It has a lock screen widget so you can quickly check the timer without unlocking your phone.
2. [TODO] It can set alarm times (like a count-down timer) while still counting up the time that the
food has been cooking, however long that takes.

## Status
It works. Development is ongoing.

## Usage tips
* Tap the stopwatch time to cycle between Reset -> Running -> Paused -> Reset.
* Put the BBQ Timer widget on the Android lock screen for quick access. (This requires Android 4.2
  Jelly Bean or later.)
* The BBQ Timer widget also works on the home screen, even in earlier versions of Android.
* You can resize the home screen widget by long-pressing it, then dragging its resize handles.
  The initial size fits minutes and seconds (e.g. "18:12"). Stretched wider, it can fit hours,
  minutes, and seconds (e.g. "1:15:09").

## How to Use Android Lock Screen Widgets
* Android lock screen widgets work in Android 4.2 Jelly Bean and later versions.
* In Android 4.4 KitKat, you'll need to turn on Settings > Security > Enable Widgets.
* For instructions, see [Getting started with lock screen widgets on Android Jelly
  Bean](http://howto.cnet.com/8301-11310_39-57549747-285/getting-started-with-lock-screen-widgets-on-android-jelly-bean/
  "CNET How To")
  and [How to Add Lockscreen Widgets to Android 4.4
  KitKat](http://www.gottabemobile.com/2013/11/11/add-lockscreen-widgets-android-4-4-kitkat-nexus-5/
  "GottaBe Mobile").
* You can rearrange your lock screen widgets by dragging them left and right.
* Drag a widget to the rightmost position to see it as soon as you power on.
* You can rearrange your lock screen widgets any time you want, e.g. move BBQ Timer to the
  right-most position when you're cooking, then move the Digital Clock widget to the right-most
  position until your next BBQ.
* To add a lock screen widget: power on, then swipe to the right until you see the "+" screen, tap
  the "+", unlock the phone, pick a widget, then (optionally) drag it further to the right.

## Build
Using Android Studio.

## TODO
* Add the alarm feature: a chime while running, with a settable period or settable times. Set it in
  the main activity or via the Settings menu?
* Display a notification for the alarms so people can tell why it chimed.

* Set up signed, optimized, deployable release builds.
* Set other AndroidManifest.xml values. (Search info?)
* Use a font-resizing text view fit long durations into the Activity.
* Use a font-resizing heuristic to fit hours into a minimum width home screen widget without eliding
  the seconds.
* Implement Activity#onCreateThumbnail()?
* Follow system theme colors?
* Test on various OS versions, screen sizes, and pixel densities. (Is the widget preview image
  needed at multiple resolutions?)
* Add unit tests.
* L10N.
* Accessibility.
* Show a tip about adding the widget to the lock screen when first running the app.
* Show tips via a menu command: How to install as home & lock screen widgets. What tapping the
  widget text does.
* Deploy to the Play store.

* Reset the timer on boot-up?
* When stopped at 00:00, add the time of day to the widget? Make the stopwatch text 00:00 small?
  Switch to the time of day after a few minutes?
  To track the time: Upon ACTION_SCREEN_ON when there are lock screen widgets, register a Service to
  listen for ACTION_TIME_TICK. Unregister upon ACTION_SCREEN_OFF or when the last widget is removed.
* Make the text smaller to fit smaller widgets and longer duration strings? See bug workaround
  http://stackoverflow.com/questions/17396045/how-to-catch-widget-size-changes-on-devices-where-onappwidgetoptionschanged-not

## Bugs
* In the emulator (Nexus 5 KitKat), the Activity's timer sometimes stops.
* Due to Android OS bug https://code.google.com/p/android/issues/detail?id=2880 if the clock gets
set backwards, the widget's date won't update until the clock catches up to what was going to be the
next day.

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
