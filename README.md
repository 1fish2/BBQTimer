# BBQTimer Android app

It's a stopwatch with a twist (or two):
1. It has a lock screen widget so you can check the timer without unlocking your phone.
2. [TODO] It can set alarm times (like a count-down timer) while still counting up the time that the
food has been cooking, however long that takes.

## Status
In development. Not ready for alpha test.

## Build
Using Android Studio.

## TODO
* Make the main Activity's text a little smaller?
* Make the widget prettier: margins, translucent frame, background image?
* Show the date and time in the widget. (Only on the lock screen? Vertically resizable?)
* When stopped at 0:00, show just the date, time, and start button in the widget?
* Make the text a little smaller to fit durations > 9 hours? Only when the widget is small?
* Add app icons.
* Add a widget preview image.
* In the activity, add images to the Start/Stop/Reset labels?
* Set other AndroidManifest.xml values. (Search info?)
* Implement Activity#onCreateThumbnail().
* Provide color feedback when tapping on the time display text view, like the system stopwatch app.
* In the widget, tap the time display to open the activity? (... to start/stop?)
* Follow system theme colors?
* Add the alarm feature. On the main activity? Use, replace, or remove the Settings menu.
* Display a notification for the alarms so people can tell why it beeped.

## Bugs
* In the emulator (Nexus 5 KitKat), the Activity's timer sometimes stops.

## Implementation notes
* The lock screen widget pretty much needs to use a Chronometer view for the ticking time display.
Using a Text view would show clock skew between multiple widgets and would use more CPU time and
battery power.
* When paused, a Chronometer view can't show a well-determined time value because its API doesn't
accommodate that case. You could fake it by sending it a start time that's about the right amount of
time ago but you can't control how long until it reads the system clock. Consequently, multiple
widgets display different paused time values. The workaround is to switch the display when paused
from a Chronometer view to a Text view.
* Formatting the elapsed time like 0:12 would look nicer than 00:12 but Android's Chronometer view
only does the latter. The app's main activity screen and the paused Text view could deviate from
that but the inconsistency would be jarring.
* The widget's ViewFlipper must explicitly set android:measureAllChildren="false", otherwise
flipping its subviews will resize the adjacent ImageButton on Galaxy Nexus Jelly Bean. (Why?)
