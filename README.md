# BBQTimer Android app

[<img alt='Get it on Google Play' height="80" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'>](https://play.google.com/store/apps/details?id=com.onefishtwo.bbqtimer&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)
[<img alt="Get it on IzzyOnDroid" height="80" src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png">](https://apt.izzysoft.de/fdroid/index/apk/com.onefishtwo.bbqtimer)

* **Combined interval timer/stopwatch** = periodic alarms + elapsed time.  
  Reminds you periodically to turn the food while it tracks the total cooking time.
* Quick access via **lock screen notification** and **home screen widget**.
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

<img alt="paused screenshot" height="540" src="Play-assets/Pixel 3 screenshots/screenshot paused.png">
<img alt="lock-screen screenshot" height="540" src="Play-assets/Pixel 3 screenshots/screenshot lock screen.png">


## Android Requirements

**BBQ Timer** is for phones and tablets running Android 5.0+ (Lollipop and later).

If it encounters problems on your Android device, email me the details (device model, Android
version, screen size, problem symptom, Android "bugreport" file).


## Usage Tips
* Tap the checkbox to turn the periodic reminder alarms on/off.
* Tap the time display to cycle between *stopped* → *running* → *paused* → *stopped*.
* Add the BBQ Timer widget to the home screen.
  * Tap the widget's elapsed time to start/pause/stop.
  * Tap the widget's background or its countdown time to open the app.
  * Resize the widget (long-press it then drag its resize handles) to see more or less info.
  * To remove the widget, long-press and drag it to “× Remove”.
* While BBQ Timer is *running* or *paused*, it appears **on the lock screen** and **in the pull-down notification** so you can see and control it in those places.
  * To put it on the lock screen, put it in **Pause** or **Play** mode by tapping buttons in the app or the home screen widget.
  * You can long-press the app’s home screen icon, then tap the “Pause at 00:00” shortcut (on Android 7.1+) to make it Paused and ready on the lock screen.
* The app, home screen widget, and pull-down notification show the countdown interval time as well as the total elapsed time (requires Android 7+).
* You can change BBQ Timer’s “Alarm” sound in Settings - Notifications. Don’t pick “None” if you want to hear the interval alarms. To restore the app’s cowbell sound, uninstall and reinstall the app.

### System Settings needed to hear and see BBQTimer notification alarms
* “Alarm volume” at an audible level.
* Lock screen - Show all or non-private notifications.
* BBQTimer “Show notifications”, *not* Silent. (You may also choose to “Override Do Not Disturb“.)
* BBQTimer “Alarm” notification category - “Show notifications”, *not* “Silent”, “Make sound and pop on screen”, sound choice *not* “None”, Importance “High” or higher to hear and see on the lock screen and in the notification area.
* Settings / Apps / Special app access / Alarms & reminders / Allowed.


## License

[MIT License](https://github.com/1fish2/BBQTimer/blob/master/LICENSE.md).

Google Play and the Google Play logo are trademarks of Google LLC.


## Notes
* There’s some clock skew between the stopwatch time as displayed in widgets vs. the
  notification area. To see more precise times, open the application.
* At Font Size **Largest** in Settings **Display Size Default**, the home screen widget's text may get clipped at the bottom.

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


## Android programming notes
* The auto-size countdown text can jump upwards when the text changes or the soft keyboard opens on Android SDK 30 - 32. On earlier versions of Android, auto-text resizing doesn't always grow back when the text shrinks. The workaround is to set the height to `0dp` (called MATCH_CONSTRAINT in the docs) instead of `wrap_content` + `app:layout_constrainedHeight="true"`.
* Android 12 on Pixel 3 (at least) drops the initial "off" period from the notification vibration pattern, which makes it impossible to sync the notification's vibration pattern to its sound. Work around that by adding a tiny pulse at the start.
* To avoid flakiness, Espresso Instrumentation tests sometimes need delays or additional actions, in addition to turning off Android animations. Is there a better way?
* To get good contrast in notification text on Android SDK 24 - 26, use different day and night color resources. On Android SDK 23, color the mid-gray background area to get contrast with both the medium gray heads-up notification text and the light white pull-down notification text. The color setting carries over from a notification to its replacement so there's no way to consistently set different background colors for the two cases.
* The `EditText` and Material Design `TextInputEditText` widgets don't call their `OnFocusChangeListener`. Work around that with a subclass of `TextInputEditText` that overrides `onFocusChanged()` to call its own listener.
* Automatic focus in the text field on Activity start, app switch, or rotation is distracting and annoying, esp. with its CLEAR_TEXT (X) endIcon. So force it to defocus and reset its contents. Hiding the soft keyboard seems to be an unsolved problem in general.
* On SDK 27, double-clicking the EditText field would cause log errors `FileNotFoundException: No file for null locale` and `TextClassifierImpl: Error getting assist info`. On SDK > 27, something calls the TextClassifier on the UI thread, logging `TextClassifier called on main thread` because it can be slow. To avoid the delay and potential ANR, switch to the no-op TextClassifier.
* Making the home screen widget responsive to its size and screen orientation varies by Android SDK version. Android 12+ supports view mappings to switch between layouts without waking the app. View mappings and layout managers react to **actual sizes**. For Android < 12, use the `onAppWidgetOptionsChanged()` hook to respond when the user resizes a widget. It only receives the **size range** minWidth x maxHeight on portrait to maxWidth x minHeight on landscape, and it rarely gets called when the screen rotates, so it can only set a layout for the size range. Any finer responsiveness must be implemented by the layout managers and density/size/orientation-specific resources.
* Android's Split Screen feature can crash or refuse to split the screen, depending on the app's `minHeight` value, at least on SDK 29+. Tune the `minHeight` value experimentally.
* On SDK < 23, an icon in the home screen widget's background gets stretched horizontally. The workaround is to skip the icon on those Android versions.
* Show a large notification icon in API < 24 so Android won't stretch the small icon to a fuzzy large icon. Otherwise large icons are supposed to be reserved for cases like a user photo.
* When paused, a Chronometer view can’t show the right time value because its API doesn’t
handle that case. A Chronometer also ignores its format string when stopped. The workaround is to switch to a Text view when paused. This app uses Chronometers in home screen widgets and notifications.
* Formatting the elapsed time like 0:12 would look nicer than 00:12 but the latter matches Android’s Chronometer view for consistency.
* The widget’s ViewFlipper must explicitly set android:measureAllChildren="false", otherwise
flipping its subviews will resize the adjacent ImageButton on Galaxy Nexus Jelly Bean.


[Privacy Policy](Privacy-Policy-for-the-BBQTimer-app.md).
