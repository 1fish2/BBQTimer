# BBQTimer Android app

[<img alt='Get it on Google Play' height="80" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'>](https://play.google.com/store/apps/details?id=com.onefishtwo.bbqtimer&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)
[<img alt="Get it on IzzyOnDroid" height="80" src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png">](https://apt.izzysoft.de/fdroid/index/apk/com.onefishtwo.bbqtimer)

* **Reminds you periodically to check/flip the food while it tracks the total cooking time.**  
  Interval timer + stopwatch = periodic alarms + elapsed time.
* **Quick access on the lock screen, home screen, and notifications without unlocking your phone.**  
  Pull-down notification, lock screen notification, home screen widget, lock screen widget, and use the app over the lock screen (on supported devices).
* Adjust alarms while the timer is running.
* Create a quick-access menu with frequent interval times and your own notes.
* No ads.

Example intervals:
* `10` = 10 minutes
* `7:30` = 7 minutes, 30 seconds
* `3:15:00` = 3 hours, 15 minutes

Short forms:
* `12:00` = `12:0` = `12:` = `12` = 12 minutes
* `0:09` = `:9` = 9 seconds
* `2:00:00` = `2:0:0` = `2::` = `120` = 2 hours

Enter the interval time in `minutes`, `minutes:seconds`, or `hours:minutes:seconds` (MM, MM:SS, HH:MM:SS).

<!--suppress CheckImageSize -->
<img alt="paused screenshot" height="540" src="Play-assets/Pixel 3 screenshots/screenshot paused.png">
<img alt="lock-screen screenshot" height="540" src="Play-assets/Pixel 3 screenshots/screenshot lock screen.png">

## Android Requirements

**BBQ Timer** is for phones and tablets running Android 7.0+ (Android N and later).

If it encounters problems on your Android device, email me the details (device model, Android
version, screen size, and problem details).


## Tips
* Tap the checkbox to turn periodic reminder alarms on/off.
* Tap the time display to cycle between *stopped* → *running* → *paused* → *stopped*.
* To use the app over the lock screen[](no-store) (Android 8.1+)[](/no-store): While viewing the app’s activity, tap the power button to turn off the screen, then tap the power button to wake the screen.
* BBQ Timer home screen widget:
  * Add it: Long-press the Home screen app icon, tap Widgets in the menu, then tap or drag the BBQ Timer widget.
  * Tap the widget’s ⏸️/▶️ button or elapsed time to _run_/_pause_/_stop_ the timer.
  * Tap its background or countdown to open the app.
  * Long-press it then drag its handles to see more/less info.[](no-store)
  * To remove it: Long-press then drag it to or tap “× Remove”.[](/no-store)
* While BBQ Timer is *running* or *paused*, it's **on the lock screen** and **in the pull-down notification**.
  * To get the notification onto the lock screen: tap ⏸️or ▶️in the app or widget.
  * Long-press the app’s home screen icon, then tap the “Pause at 00:00” shortcut to *pause* it.
* The app, home/lock screen widget, and pull-down notification show the countdown interval time and the total elapsed time.
* Tap ▲ in the alarm interval field to open the menu of Alarm intervals.
  * Tap “Edit these intervals…” in the menu (or long-press ▲) to edit the menu.
  * Each line should have a duration in the form _HH:MM:SS_ or _MM:SS_ or _MM_, followed by optional notes. E.g. “6 thin fish, cook to 145°F” to cook 6 minutes/side and check it for 145°F.
  * In the editor, tap the “Reset” command to build a fresh menu of intervals using the current Temperature units (Android 14+) or language/country.
* In the app, the phone’s volume keys adjust the Alarm volume.[](no-store)
* You can change the app’s Alarm sound in Settings → Notifications. If you pick “None” it won’t sound an alarm. To restore the app’s cowbell sound: Uninstall then reinstall the app.[](/no-store)
* To add the BBQ Timer widget to the lock screen (available on some devices):
  1. Settings → enable “Widgets on lock screen.”
  2. Turn the screen off and on.
  3. Swipe left to reach the widget screen.
  4. Long-press the screen or tap “+Add widgets,” then unlock.
  5. Tap Browse → BBQ Timer.
  6. Tap BBQ Timer widget.
  7. Tap (+ Add), (✔︎ Done).
  * To use the widget: Turn the screen on then swipe left to the widgets.

### System settings for reliable alarms
* Alarm volume at an audible level.
* Lock screen → Show non-private or all notifications.
* Settings → Apps → Special app access → Alarms & reminders → Allowed.
* Apps → BBQ Timer “Show notifications” *not* Silent. (Optional: Override Do Not Disturb.)
* Apps → BBQ Timer “Alarm” notification category → “Show notifications”; *not* “Silent”; “Make sound and pop on screen”; sound choice *not* “None”; Importance “High” or higher to hear and see it on the lock screen and in the notification area.
* Notifications → App notifications → BBQ Timer → On.


## License

[MIT License](https://github.com/1fish2/BBQTimer/blob/master/LICENSE.md).

Google Play and the Google Play logo are trademarks of Google LLC.


## Notes
* There can be a little clock skew between the time in widgets and the
  notification area. To see more precise times, open the application.
* At Font Size **Largest** in Settings **Display Size Default**, the home screen widget’s text may get clipped at the bottom.

## Building from Source Code
Use [Android Studio](http://developer.android.com/sdk/installing/studio.html) (an excellent tool!).


## Dedication
Dedicated to open source software developers.

Open source software used for this project includes: Android, Android Studio, ART, Audacity, Chrome,
Dalvik, Gimp, git, Gradle, Inkscape, MinGW, Proguard, and much more that we use without thinking
about, like libpng and zlib.

Don’t get me wrong -- commercial software is also great!


## Media Asset Sources
The app’s alarm sound is composed of recorded cowbell sounds which are used by permission from Phil Burk,
Copyright © 2014 Mobileer Inc.


## Keywords
BBQ, cooking, interval timer, home screen widget, reminder alarm, stopwatch.


[Privacy Policy](Privacy-Policy-for-the-BBQTimer-app.md).
