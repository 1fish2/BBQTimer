# BBQTimer Android app

[<img alt='Get it on Google Play' height="80" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'>](https://play.google.com/store/apps/details?id=com.onefishtwo.bbqtimer&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)
[<img alt="Get it on IzzyOnDroid" height="80" src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png">](https://apt.izzysoft.de/fdroid/index/apk/com.onefishtwo.bbqtimer)

* **Combined interval timer / stopwatch** = periodic alarms + elapsed time.  
  Reminds you periodically to turn the food while it tracks the total cooking time.
* Quick access via **lock screen notification**, **pull-down notification**, and
 **home screen widget**.
* Editable **pop-up menu of interval times**. Quickly access your favorite timers,
  each with optional notes.
* **Changeable alarms** while it’s running.
* No ads.

Type in the interval time in `minutes`, `minutes:seconds`, or `hours:minutes:seconds`.

Example intervals:
* `10` = 10 minutes
* `7:30` = 7 minutes, 30 seconds
* `3:15:00` = 3 hours, 15 minutes

Short forms:
* `12:00` = `12:0` = `12:` = `12` = 12 minutes
* `0:09` = `:9` = 9 seconds
* `2:00:00` = `2:0:0` = `2::` = `120` = 2 hours

<!--suppress CheckImageSize -->
<img alt="paused screenshot" height="540" src="Play-assets/Pixel 3 screenshots/screenshot paused.png">
<img alt="lock-screen screenshot" height="540" src="Play-assets/Pixel 3 screenshots/screenshot lock screen.png">


## Android Requirements

**BBQ Timer** is for phones and tablets running Android 6.0+ (Marshmallow and later).

If it encounters problems on your Android device, email me the details (device model, Android
version, screen size, problem symptom, Android "bugreport" file).


## Usage Tips
* Tap the checkbox to turn the periodic reminder alarms on/off.
* Tap the time display to cycle between *stopped* → *running* → *paused* → *stopped*.
* Add the BBQ Timer widget to the home screen.
  * Tap the widget’s elapsed time to start/pause/stop.
  * Tap the widget’s background or its countdown time to open the app.
  * Resize the widget (long-press it then drag its resize handles) to see more or less info.
  * To remove the widget, long-press and drag it to “× Remove”.
* While BBQ Timer is *running* or *paused*, it appears **on the lock screen** and **in the pull-down notification** so you can see and control it in those places.
  * To put it on the lock screen, put it in **Pause** or **Play** mode by tapping buttons in the app or the home screen widget.
  * You can long-press the app’s home screen icon, then tap the “Pause at 00:00” shortcut (on Android 7.1+) to make it Paused and ready on the lock screen.
* Tap ▲ in the alarm interval text field to open the pop-up menu of interval times.
  * Tap “Edit these intervals…” in the menu (or Long-press ▲) to customize the menu. Each line should start with a time interval in HH:MM:SS or MM:SS or MM, followed by a space and optional notes. E.g. “6 thin fish, cook to 145°F” is a 6 minute timer interval to flip or check the fish, with a reminder that fish is typically considered done at 145°F.
  * When editing, tap the “Reset” command to reconstruct a fresh menu of alarm intervals using the current system Language preference and (on Android 14+) the current Regional preference for Temperature units.
* The app, home screen widget, and pull-down notification show the countdown interval time as well as the total elapsed time (requires Android 7+).
* In the app, the phone’s volume keys adjust the Alarm volume.
* You can change BBQ Timer’s “Alarm” sound in Settings - Notifications. Don’t pick “None” if you want to hear the interval alarms. To restore the app’s cowbell sound, uninstall and reinstall the app.

### System Settings needed to hear and see BBQ Timer notification alarms
* “Alarm volume” at an audible level.
* Lock screen - Show all or non-private notifications.
* Settings / Apps / Special app access / Alarms & reminders / Allowed.
* Apps / BBQ Timer “Show notifications”, *not* Silent. (You may also choose to “Override Do Not Disturb“.)
* Apps / BBQ Timer “Alarm” notification category - “Show notifications”, *not* “Silent”, “Make sound and pop on screen”, sound choice *not* “None”, Importance “High” or higher to hear and see on the lock screen and in the notification area.
* Notifications / App settings / BBQ Timer / On.


## License

[MIT License](https://github.com/1fish2/BBQTimer/blob/master/LICENSE.md).

Google Play and the Google Play logo are trademarks of Google LLC.


## Notes
* There’s some clock skew between the stopwatch time as displayed in widgets vs. the
  notification area. To see more precise times, open the application.
* At Font Size **Largest** in Settings **Display Size Default**, the home screen widget’s text may get clipped at the bottom.

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


[Privacy Policy](Privacy-Policy-for-the-BBQTimer-app.md).
