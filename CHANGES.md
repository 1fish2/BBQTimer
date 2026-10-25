# BBQTimer change history

## Beta release 5.0b1, versionCode 26, targetSdk 37 Android 17

### en-US
* Support Android versions 7 - 17+, minSdk 24.
* Control via notifications on Wear OS smartwatches: Start/stop timers directly on your wrist
* Use BBQ Timer without unlocking: in the app, turn the screen off and back on; or open it from a lock screen Notification or from a lock screen Widget (on supported devices; turn on Settings/Display/Lock screen/Widgets on lock screen)
* Modernize the UI to Material Design 3 with color token palettes, color contrast levels, scalable vector drawables, tool tips, better layout on large screens, and a little animation. Add a backward-compatible SplashScreen.
* Edge-to-edge compatibility, accommodating display cutouts and gesture navigation areas.
* Support notifications bridged to Wear OS.
  * Wear OS notification bridging does not support live count-up or count-down chronometers. But the time value in the top right corner (e.g. "10m") will update at 1 minute resolution when it’s offscreen. It shows the elapsed time running or paused.
* Support lock screen widgets on relevant devices.
* Support running the Activity over the lock screen.
  * To use BBQ Timer w/o unlocking: in the app, turn the screen off and back on; or open it from a lock screen notification or from a lock screen Widget (on supported devices; turn on Settings/Display/Lock screen/Widgets on lock screen).
  * Add a lock state indicator in the action bar to indicate that the device remains securely locked while the BBQ Timer Activity is accessible.
* Bundle a fixed-width font and use fontFeatureSettings="tnum" in time views to fix width-jitter during ticks on most devices.
* Improve the Recipe Editor dialog's handling of screen rotation.
* More test coverage.
* Code refactorings.
* Add android:intentMatchingFlags for Android 16+ increased Intent security.
* Workaround more Android bugs.

### de-DE
* Unterstützung für Android-Versionen 7 bis 17+ (minSdk 24).
* Steuerung über Benachrichtigungen auf Wear OS-Smartwatches: Timer direkt am Handgelenk starten/stoppen
* BBQ Timer ohne Entsperren nutzen: Bildschirm in der App aus- und wieder einschalten oder über eine Sperrbildschirm-Benachrichtigung bzw. ein Sperrbildschirm-Widget öffnen (auf unterstützten Geräten; aktivieren unter Einstellungen/Display (Anzeige)/Sperrbildschirm/Widgets auf dem Sperrbildschirm)
* Verbesserte Übersetzungen
* Modernisierung der Benutzeroberfläche auf Material Design 3 mit Farb-Token-Paletten, Kontraststufen, skalierbaren Vektorgrafiken (Vector
Drawables), Tooltips, optimiertem Layout für große Bildschirme und dezenten Animationen. Abwärtskompatibler SplashScreen hinzugefügt.
* Edge-to-Edge-Kompatibilität unter Berücksichtigung von Display-Ausschnitten (Cutouts) und Gesten-Navigationsbereichen.
* Unterstützung für an Wear OS weitergeleitete Benachrichtigungen (Notification Bridging).
  * Die Wear OS-Benachrichtigungsweiterleitung unterstützt keine Live-Chronometer zum Hoch- oder Herunterzählen. Die Zeitangabe in der
oberen rechten Ecke (z. B. „10m“) wird bei ausgeschaltetem Bildschirm jedoch im 1-Minuten-Takt aktualisiert. Sie zeigt die abgelaufene Zeit
(laufend oder pausiert) an.
* Unterstützung für Sperrbildschirm-Widgets auf entsprechenden Geräten.
* Unterstützung für das Ausführen der Activity über dem Sperrbildschirm.
  * BBQ Timer ohne Entsperren nutzen: In der App den Bildschirm aus- und wieder einschalten oder die App über eine Sperrbildschirm-
Benachrichtigung bzw. ein Sperrbildschirm-Widget öffnen (auf unterstützten Geräten; aktivieren unter Einstellungen > Display >
Sperrbildschirm > Widgets auf dem Sperrbildschirm).
  * Sperrstatus-Anzeige in der Action Bar hinzugefügt, um zu signalisieren, dass das Gerät weiterhin sicher gesperrt bleibt, während die
BBQ-Timer-Activity zugänglich ist.
* Nicht-proportionale Schriftart gebündelt und fontFeatureSettings="tnum" in Zeitanzeigen verwendet, um Breitenflackern beim Weiterschalten
der Ziffern auf den meisten Geräten zu beheben.
* Verhalten des Rezept-Editor-Dialogs bei Bildschirmdrehung verbessert.
* Höhere Testabdeckung.
* Code-Refactorings.
* android:intentMatchingFlags für erhöhte Intent-Sicherheit ab Android 16+ hinzugefügt.
* Workarounds für weitere Android-Bugs implementiert.


## Beta release 4.1b1, versionCode 24, targetSdk 34 Android 14

### en-US
* Work around Moto G's variable width digits by embedding a Roboto font.
* Support °C/°F Regional preferences in Android 14.
* Support Android 14, API 34.
* Start on Android 15, API 35, e.g. Intents must have actions.
* Update the dev tools & libraries.
* Fix inspection warnings.
* Set a monochrome adaptive app icon.
* Log early & late setAlarmClock() alarms.
* Add StrictMode configuration controlled by a static final boolean.
* Tweak the popup menu code in an attempt to fix the exception WindowLeaked PopupWindow$PopupDecorView.
* Stop tracking MainActivityIsVisible.*
* Load prefs during app creation. Hopefully the I/O time is covered by the splash screen.
* On Android 15+, Force Stop disables the widget. Reset state when the user taps it or opens the app, also after device reboot then login.
* Speed up the playPauseStopUITest by checking saved res IDs instead of button icon pixels.

### de-DE
* Umgehen Sie die Ziffern mit variabler Breite des Moto G.*
* Unterstützen Sie regionale °C/°F-Präferenzen in Android 14.
* Unterstützung für Android 14, API-Level 34.
* Erste Anpassungen an Android 15, API-Level 35 (z. B. müssen Intents nun zwingend Aktionen besitzen).
* Entwickler-Werkzeuge und Bibliotheken aktualisiert.
* Code-Inspektions-Warnungen behoben.
* Monochromes adaptives Anwendungssymbol eingerichtet.
* Protokollierung von zu frühen und verspäteten setAlarmClock()-Alarmen integriert.
* StrictMode-Konfiguration hinzugefügt, gesteuert über einen statischen booleschen Wert (static final boolean).
* Code des Popup-Menüs angepasst, um die Ausnahme WindowLeaked PopupWindow$PopupDecorView abzufangen.
* Verfolgung von MainActivityIsVisible.* eingestellt.
* Einstellungen (SharedPreferences) während der App-Initialisierung laden; I/O-Zeit wird voraussichtlich durch den Startbildschirm
abgedeckt.
* Ab Android 15+ deaktiviert das Erzwingen des Beendens das Widget. Status-Reset erfolgt bei Antippen, beim App-Start sowie nach dem
Neustart und Benutzer-Login.
* Laufzeit von playPauseStopUITest verkürzt, indem gespeicherte Ressourcen-IDs anstelle von Icon-Pixeln abgeglichen werden.

## Release 4.0, versionCode 23

### en-US
* ▲ Pop-up menu of interval times. Quickly access your favorite timers with optional notes, e.g. "6 minutes for thin fish, cook to 145°F". Long-press ▲ to customize the menu.
* Concise interval time display (04:00 ➜ 4) to suggest short form entry.
* Android 13.

### de-DE
* ▲ Popup-Menü der Intervallzeiten. Greifen Sie schnell auf Ihre bevorzugten Timer mit optionalen Notizen zu, z. "6 Minuten für dünnen Fisch, bis 63°C". Drücken Sie lange auf ▲, um das Menü anzupassen.
* Prägnante Intervallzeitanzeige (04:00 ➜ 4), um eine Eingabe in Kurzform vorzuschlagen.
* Android 13.

## beta release-4.0b1, versionCode 23 Pre-release

### en-US
* ▲ Pop-up menu of interval times. Quickly access your favorite timers with optional notes, e.g. "6 minutes for thin fish, cook to 145°F". Long-press ▲ to customize the menu.
* Concise interval time display (04:00 ➜ 4) to suggest short form entry.
* Android 13: UI to request permission to Notify the user, USE_EXACT_ALARM, and locals_config.

### de-DE
* ▲ Popup-Menü der Intervallzeiten. Greifen Sie schnell auf Ihre bevorzugten Timer mit optionalen Notizen zu, z. "6 Minuten für dünnen Fisch, bis 63°C". Drücken Sie lange auf ▲, um das Menü anzupassen.
* Prägnante Intervallzeitanzeige (04:00 ➜ 4), um eine Eingabe in Kurzform vorzuschlagen.
* Android 13: Benutzeroberfläche zum Anfordern der Berechtigung zum Benachrichtigen des Benutzers, "USE_EXACT_ALARM" und locals_config.

## Release 3.0, versionCode 21, targetSdk 30, minSdk 21, versionCode 21 (same build as 3.0b1, 3.0a6)

### en-US
* Type in an alarm time as m, m:s, or h:m:s.
* See the countdown interval timer and the total elapsed time in the app, home screen widget, and notification (Android 7+).
* Resize the home screen widget to see more/less info. Tap its background to open the app. Its pause/play button begins ready to Pause (to open the lock screen notification).
* Updates for Android 12.
* Light/Dark styles. New icons.

### de-DE
* Geben Sie eine Weckzeit im Format M, M:S oder H:M:S ein.
* Siehe Countdown- und Countup-Zeiten.

## beta release 3.0b1, versionCode 21, targetSdk 30, minSdk 21

### en-US
* Type in an alarm time as m, m:s, or h:m:s.
* See the countdown interval timer and the total elapsed time in the app, home screen widget, and notification (Android 7+).
* Resize the home screen widget to see more/less info. Tap its background to open the app. Its pause/play button begins ready to Pause (to open the lock screen notification).
* Updates for Android 12.
* Light/Dark styles. New icons.

## Release versionCode 15, versionName "2.5"

### en-US
* ½ minute alarms up to 4.5. Whole minutes up to 99.
* Larger timer text in the app (auto-size).
* Long-press the launch screen icon for Android 7.1 (N MR1+) app shortcuts.
* You can change the alarm sound on Android O via notification channel settings. Be careful not to disable the sound or lock screen visibility.
* Fire red notification LED.
* Fix: Home screen widget text is fine with system font size changes.
* Warns the user about some cases of disabled app notifications.
* Makes the home/lock screen widget text size fixed to fit the vertical space and not vary with the system font size.

## Release 2.4: Android N, Alarm volume

### en-US
* Tune for Android N.
* Use the system Alarm (not Ring) volume, have the volume keys adjust it, and warn if it's muted. “Do not disturb - Alarms only” won’t silence alarms.
* In the app, the hardware volume buttons adjust the Alarm volume.
* Warn you if the Alarm volume is muted.
* On Android L+, Stop -> Pause will pause at 0:00 to readily open the lockscreen notification. Older Androids get Stop instead of Reset.
* The cycle is now Run -> Pause -> Stop instead of Run -> Pause -> Reset.
* Add 1/2-minute period.
* Pulse the paused timer for visibility.
* Work around low-contrast Android M notifications.
* Desupport Android 3.x H (SDK 12-13).
* Drop the alarm count from notifications. Android N doesn't support it, and it's maybe not worth the visual complexity.
* Ensure set-clock adjustments don't cause early/late alarms.
* Use Material Design colors. Color the status bar.
* Ensure some Font/Display Size combos don't show the date below the home screen widget on Android L+.

## Release 2.3, Add "de", Android M

### en-US
* Now in German (Deutsche) and English.
* Android 6.0 Marshmallow compatibility and system "alarm clock" display.
* Heads-up notification alarm in-app on Android 5+.
* Pre-check the "Periodic alarms" checkbox for newcomers.

## Release 2.2, "en"

### en-US
* Removed excess library resources so Google Play won't think the app is localized into many languages.

### Release 2.1, Tablet release

### en-US
* Tablets: Big numbers and buttons in the app.
* App updates don't stop the timer.

### Release 2.0, Lollipop release

### en-US
* Android Lollipop-savvy. Lollipop doesn’t support lock-screen widgets so now you can operate BBQ Timer from its lock-screen notification as long as the timer is running or paused.
* “Material design” style.
* Bug fixes for older versions of Android.
