# BBQTimer Android-App

[<img alt='Jetzt bei Google Play' height="80" src='https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png'>](https://play.google.com/store/apps/details?id=com.onefishtwo.bbqtimer&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1)
[<img alt="Erhältlich bei IzzyOnDroid" height="80" src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png">](https://apt.izzysoft.de/fdroid/index/apk/com.onefishtwo.bbqtimer)

* **Erinnert dich regelmäßig ans Prüfen/Wenden des Grillguts und misst die gesamte Garzeit.**  
  Intervall-Timer + Stoppuhr = regelmäßige Alarme + verstrichene Zeit.
* **Schnellzugriff über Sperrbildschirm, Startbildschirm und Benachrichtigungen ohne Entsperren.**  
  Pulldown- und Sperrbildschirm-Benachrichtigung, Start- und Sperrbildschirm-Widget sowie App über dem Sperrbildschirm nutzen (auf unterstützten Geräten).
* Alarme bei laufendem Timer anpassen.
* Schnellzugriff-Menü mit häufigen Intervallen und eigenen Notizen erstellen.
* Werbefrei.

Beispiel-Intervalle:
* `10` = 10 Minuten
* `7:30` = 7 Minuten, 30 Sekunden
* `3:15:00` = 3 Stunden, 15 Minuten

Praktische Kurzformen:
* `12:00` = `12:0` = `12:` = `12` = 12 Minuten
* `0:09` = `:9` = 9 Sekunden
* `2:00:00` = `2:0:0` = `2::` = `120` = 2 Stunden

Gib das Alarmintervall in `Minuten`, `Minuten:Sekunden` oder `Stunden:Minuten:Sekunden` ein (MM, MM:SS, HH:MM:SS).

<!--suppress CheckImageSize -->
<img alt="Screenshot: Pausiert" height="540" src="Play-assets/Pixel 3 screenshots/screenshot paused.png">
<img alt="Screenshot: Sperrbildschirm" height="540" src="Play-assets/Pixel 3 screenshots/screenshot lock screen.png">

## Systemanforderungen

**BBQ Timer** ist für Smartphones und Tablets ab Android 7.0 (Android N und höher).

Falls auf deinem Android-Gerät Probleme auftreten, sende mir bitte die Details per E-Mail (Gerätemodell, Android-Version, Bildschirmgröße und Problembeschreibung).

## Tipps
* Kontrollkästchen antippen, um regelmäßige Erinnerungsalarme ein-/auszuschalten.
* Zeitanzeige antippen, um zwischen *gestoppt* → *läuft* → *pausiert* → *gestoppt* zu wechseln.
* App über dem Sperrbildschirm nutzen[](no-store) (Android 8.1+)[](/no-store): Bei geöffneter App die Ein-/Aus-Taste drücken, um den Bildschirm auszuschalten, und erneut drücken zum Aufwecken.
* BBQ Timer Startbildschirm-Widget:
  * Hinzufügen: App-Symbol gedrückt halten, im Menü auf Widgets tippen und das BBQ Timer-Widget antippen oder ziehen.
  * Im Widget auf ⏸️/▶️ oder die verstrichene Zeit tippen, um den Timer zu _starten_/_pausieren_/_stoppen_.
  * Auf Hintergrund oder Countdown tippen, um die App zu öffnen.
  * Gedrückt halten und Rahmen ziehen, um mehr/weniger Infos zu sehen.[](no-store)
  * Entfernen: Gedrückt halten und auf „× Entfernen“ ziehen oder tippen.[](/no-store)
* Wenn BBQ Timer *läuft* oder *pausiert* ist, ist er **auf dem Sperrbildschirm** und **in der Pulldown-Benachrichtigung**.
  * Für die Sperrbildschirm-Anzeige: In App oder Widget auf ⏸️ oder ▶️ tippen.
  * App-Symbol auf dem Startbildschirm gedrückt halten und „Pause bei 00:00“ wählen, um ihn zu *pausieren*.
* App, Start-/Sperrbildschirm-Widget und Pulldown-Benachrichtigung zeigen Countdown-Intervall und gesamte verstrichene Zeit.
* Im Alarmintervall-Feld auf ▲ tippen, um das Menü der Alarmintervalle zu öffnen.
  * Im Menü auf „Diese Intervalle bearbeiten…“ tippen (oder ▲ gedrückt halten), um es anzupassen.
  * Jede Zeile hat eine Dauer im Format _HH:MM:SS_, _MM:SS_ oder _MM_ plus optionale Notizen. Z. B. „6 dünne Fischfilets, bis 63 °C“ für 6 Min./Seite und Prüfung auf 63 °C.
  * Im Editor auf „Zurücksetzen“ tippen, um das Menü anhand der aktuellen Temperatureinheit (Android 14+) oder Sprache/Region neu zu erstellen.
* In der App regeln die Lautstärketasten des Geräts die Alarmlautstärke.[](no-store)
* Den Alarmton der App kannst du unter Einstellungen → Benachrichtigungen ändern. Bei „Keiner“ ertönt kein Alarm. Um den Kuhglocken-Ton wiederherzustellen: App deinstallieren und neu installieren.[](/no-store)
* BBQ Timer-Widget zum Sperrbildschirm hinzufügen (auf einigen Geräten verfügbar):
  1. Einstellungen → „Widgets auf dem Sperrbildschirm“ aktivieren.
  2. Bildschirm aus- und einschalten.
  3. Nach links zum Widget-Bildschirm wischen.
  4. Bildschirm gedrückt halten oder „+ Widgets hinzufügen“ tippen, dann entsperren.
  5. Durchsuchen → BBQ Timer tippen.
  6. BBQ Timer-Widget tippen.
  7. (+ Hinzufügen), (✔︎ Fertig) tippen.
  * Widget nutzen: Bildschirm einschalten und nach links zu den Widgets wischen.

### Systemeinstellungen für zuverlässige Alarme
* Alarmlautstärke auf hörbarer Stufe.
* Sperrbildschirm → Nicht-private oder alle Benachrichtigungen anzeigen.
* Einstellungen → Apps → Spezieller App-Zugriff → Wecker & Erinnerungen → Zugelassen.
* Apps → BBQ Timer „Benachrichtigungen anzeigen“ *nicht* Lautlos. (Optional: „Bitte nicht stören“ überschreiben.)
* Apps → BBQ Timer Benachrichtigungskategorie „Alarm“ → „Benachrichtigungen anzeigen“; *nicht* „Lautlos“; „Ton abspielen und auf dem Bildschirm einblenden“; Ton *nicht* „Keiner“; Wichtigkeit „Hoch“ oder höher, um ihn auf Sperrbildschirm und im Benachrichtigungsbereich zu hören und zu sehen.
* Benachrichtigungen → App-Benachrichtigungen → BBQ Timer → Ein.


## Lizenz

[MIT-Lizenz](https://github.com/1fish2/BBQTimer/blob/master/LICENSE.md).

Google Play und das Google Play-Logo sind Marken von Google LLC.


## Hinweise
* Zwischen den Widgets und dem Benachrichtigungsbereich kann es zu einem leichten Zeitversatz kommen. Öffne die App für genauere Zeiten.
* Bei Schriftgröße **Am größten** und **Anzeigegröße Standard** in den Einstellungen kann der Text im Startbildschirm-Widget unten abgeschnitten werden.

## Aus dem Quellcode erstellen
Verwende [Android Studio](http://developer.android.com/sdk/installing/studio.html) (ein hervorragendes Werkzeug!).


## Widmung
Den Entwicklern von Open-Source-Software gewidmet.

Zu der für dieses Projekt verwendeten Open-Source-Software gehören: Android, Android Studio, ART, Audacity, Chrome,
Dalvik, Gimp, git, Gradle, Inkscape, MinGW, Proguard und vieles mehr, das wir selbstverständlich nutzen,
wie libpng und zlib.

Nicht falsch verstehen – kommerzielle Software ist ebenfalls großartig!


## Medienquellen
Der Alarmton der App besteht aus aufgenommenen Kuhglocken-Klängen, verwendet mit freundlicher Genehmigung von Phil Burk,
Copyright © 2014 Mobileer Inc.


## Suchbegriffe
BBQ, Grillen, Kochen, Intervall-Timer, Startbildschirm-Widget, Erinnerungsalarm, Stoppuhr.


[Datenschutzerklärung](Privacy-Policy-for-the-BBQTimer-app.md).
