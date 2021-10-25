// The MIT License (MIT)
//
// Copyright (c) 2015 Jerry Morrison
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
// associated documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or
// substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
// NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.onefishtwo.bbqtimer;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;

import java.math.RoundingMode;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Formatter;
import java.util.Locale;

import androidx.annotation.NonNull;

/**
 * A stopwatch time counter (data model).
 * <p/>
 * The run states are {Running, Paused, Stopped}, where Paused is like Stopped plus an ongoing
 * Notification so it can be viewed and resumed on the Android Lollipop lock screen.
 */
@SuppressWarnings("SynchronizationOnStaticField")
public class TimeCounter {
    static class InjectForTesting {
        String formatElapsedTime(@SuppressWarnings("SameParameterValue") StringBuilder recycle,
                long elapsedSeconds) {
            return DateUtils.formatElapsedTime(recycle, elapsedSeconds);
        }

        @SuppressWarnings("deprecation")
        Spanned fromHtml(String source) {
            if (Build.VERSION.SDK_INT < 24) {
                return Html.fromHtml(source);
            } else {
                return Html.fromHtml(source, Html.FROM_HTML_MODE_COMPACT);
            }
        }
    }

    /** Inject or mock for testing. */
    @NonNull
    static InjectForTesting injected = new InjectForTesting();

    /** PERSISTENT STATE identifiers. */
    private static final String PREF_IS_RUNNING = "Timer_isRunning";
    private static final String PREF_IS_PAUSED  = "Timer_isPaused";  // new in app versionCode 10
    private static final String PREF_START_TIME = "Timer_startTime";
    private static final String PREF_PAUSE_TIME = "Timer_pauseTime";

    /**
     * The default format string for assembling and HTML-styling a timer duration.<p/>
     *
     * Format arg %1$s is a placeholder for the already-localized HH:MM:SS string from
     * DateUtils.formatElapsedTime(), e.g. "00:00" or "0:00:00".<p/>
     *
     * Format arg %2$s is a placeholder for the already-localized fractional seconds string, e.g.
     * ".0". The code uses a NumberFormat to format that string instead of an inline format %.1d to
     * suppress the integer part and disable rounding. It's wrapped in &lt;small&gt; HTML tags to
     * make the rapidly changing fractional part less distracting and to make it fit better on
     * screen. This way, "12:34:56.7" fits on a Galaxy Nexus screen.
     *<p/>
     * NOTE: This used to use &lt;small&gt;&lt;small&gt; but the fraction digit ".8" would display
     * wider than other digits when the time string got wide enough to need to auto-size down.
     */
    private static final String DEFAULT_TIME_STYLE = "%1$s<small>%2$s</small>";

    // Synchronized on recycledStringBuilder.
    // The buffer is big enough for hhh:mm:ss.f + HTML markup = 11 + 30, and rounded up.
    private static final StringBuilder recycledStringBuilder = new StringBuilder(44);
    private static final Formatter recycledFormatter = new Formatter(recycledStringBuilder);

    // Also synchronized on recycledStringBuilder.
    private static Locale lastLocale;
    private static NumberFormat fractionFormatter; // constructed on demand and on locale changes
    private static final StringBuffer recycledStringBuffer = new StringBuffer(2);
    private static final FieldPosition fractionFieldPosition =
            new FieldPosition(NumberFormat.FRACTION_FIELD);

    /** Makes locale-specific text formatters if needed, including a locale change. */
    private static void makeLocaleFormatters() {
        synchronized (recycledStringBuilder) {
            Locale locale = Locale.getDefault();

            if (!locale.equals(lastLocale)) {
                lastLocale = locale;
                fractionFormatter = NumberFormat.getNumberInstance();
                fractionFormatter.setMinimumIntegerDigits(0);
                fractionFormatter.setMaximumIntegerDigits(0);
                fractionFormatter.setMinimumFractionDigits(1);
                fractionFormatter.setMaximumFractionDigits(1);
                fractionFormatter.setRoundingMode(RoundingMode.DOWN);
            }
        }
    }

    private boolean isRunning;
    private boolean isPaused;  // distinguishes Paused from Stopped (if !isRunning)
    private long startTime; // elapsedRealtimeClock() when the timer was started
    private long pauseTime; // elapsedRealtimeClock() when the timer was paused

    public TimeCounter() {
    }

    /** Saves state to a preferences editor. */
    public void save(@NonNull SharedPreferences.Editor prefsEditor) {
        prefsEditor.putBoolean(PREF_IS_RUNNING, isRunning);
        prefsEditor.putBoolean(PREF_IS_PAUSED, isPaused);
        prefsEditor.putLong(PREF_START_TIME, startTime);
        prefsEditor.putLong(PREF_PAUSE_TIME, pauseTime);
    }

    /**
     * Loads and normalizes the state from a preferences object.
     *
     * @return true if the caller should {@link #save} the normalized state to ensure consistent
     * results. That happens when the loaded state is "running" or "paused" with a future startTime,
     * which means the device must've rebooted. load() can only detect that within startTime after
     * reboot, so it's important to save the normalized state in that "reset" case.
     */
    public boolean load(@NonNull SharedPreferences prefs) {
        isRunning = prefs.getBoolean(PREF_IS_RUNNING, false);
        isPaused  = prefs.getBoolean(PREF_IS_PAUSED, false);  // absent in older data
        startTime = prefs.getLong(PREF_START_TIME, 0);
        pauseTime = prefs.getLong(PREF_PAUSE_TIME, 0);

        boolean needToSave = false;

        // Enforce invariants and normalize the state.
        if (isRunning) {
            isPaused = false;
            if (startTime > elapsedRealtimeClock()) { // Must've rebooted.
                stop();
                needToSave = true;
            }
        } else if (isPaused) {
            if (startTime > pauseTime || startTime > elapsedRealtimeClock()) {
                stop();
                needToSave = true;
            }
        } else {
            stop();
        }

        return needToSave;
    }

    /** Returns the timer's start time, in SystemClock.elapsedRealtime() milliseconds. */
    public long getStartTime() {
        return startTime;
    }

    public long getPauseTime() {
        return pauseTime;
    }

    /** Returns the underlying clock time, in milliseconds since boot. */
    // TODO: Inject the clock for testability.
    public long elapsedRealtimeClock() {
        return SystemClock.elapsedRealtime();
    }

    /** Converts from the elapsed realtime clock (ELAPSED) to the realtime wall clock (RTC). */
    public long elapsedTimeToWallTime(long elapsed) {
        return elapsed - elapsedRealtimeClock() + System.currentTimeMillis();
    }

    /** Returns true if the timer is Running (not Stopped/Paused). */
    public boolean isRunning() {
        return isRunning;
    }

    /** Returns true if the timer is Paused (not Stopped/Running). */
    public boolean isPaused() {
        return !isRunning && isPaused;
    }

    /** Returns true if the timer is Stopped (not Running/Paused). */
    public boolean isStopped() {
        return !isRunning && !isPaused;
    }

    /**
     * Returns true if the TimeCounter is Paused at 0:00 (the result of
     * {@link #reset()}).
     */
    public boolean isPausedAt0() {
        return isPaused() && getElapsedTime() == 0;
    }

    /**
     * Returns the Timer's Running/Paused/Stopped state for debugging. Not localized.
     *
     * @see com.onefishtwo.bbqtimer.Notifier#timerRunState(TimeCounter)
     */
    @NonNull
    String runState() {
        if (isRunning) {
            return "Running";
        } else if (isPaused) {
            return "Paused";
        } else {
            return "Stopped";
        }
    }

    /** Returns the timer's (Stopped/Paused/Running) elapsed time, in milliseconds. */
    public long getElapsedTime() {
        return (isRunning ? elapsedRealtimeClock() : pauseTime) - startTime;
    }

    /** Stops and clears the timer to 0:00. */
    public void stop() {
        startTime = pauseTime = 0;
        isRunning = false;
        isPaused  = false;
    }

    /** Starts or resumes the timer. */
    public void start() {
        if (!isRunning) {
            startTime = elapsedRealtimeClock() - (pauseTime - startTime);
            isRunning = true;
            isPaused  = false;
        }
    }

    /** Pauses the timer. */
    public void pause() {
        if (isRunning) {
            pauseTime = elapsedRealtimeClock();
            isRunning = false;
        }
        isPaused = true;
    }

    /**
     * Toggles the state to Running or Paused for a Play/Pause button that goes
     * Stopped or Paused -> Running -> Paused.
     *
     * @return true if the timer is now running.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean toggleRunPause() {
        if (isRunning) {
            pause();
        } else {
            start();
        }
        return isRunning;
    }

    /**
     * Toggles the state to Paused or Running for a Play/Pause button that goes
     * Stopped or Running -> Paused -> Running.
     *
     * @return true if the timer is now running.
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean togglePauseRun() {
        if (isPaused) {
            start();
        } else {
            pause();
        }
        return isRunning;
    }

    /** Cycles the state: Paused at 0:00 or Stopped -> Running -> Paused -> Stopped. */
    public void cycle() {
        if (isRunning()) {
            pause();
        } else if (isStopped() || isPausedAt0()) {
            start();
        } else {
            stop();
        }
    }

    /** Resets the timer to Paused at 0:00. */
    public void reset() {
        startTime = pauseTime = 0;
        isRunning = false;
        isPaused  = true;
    }

    /**
     * Formats this TimeCounter's millisecond duration in localized [hh:]mm:ss.f format <em>with
     * attached styles</em>.
     */
    public Spanned formatHhMmSsFraction() {
        return formatHhMmSsFraction(getElapsedTime());
    }

    /** Formats a millisecond duration in [hh:]mm:ss format like Chronometer does. */
    public static String formatHhMmSs(long elapsedMilliseconds) {
        long elapsedSeconds = elapsedMilliseconds / 1000;
        InjectForTesting injected1 = injected;

        synchronized (recycledStringBuilder) {
            return injected1.formatElapsedTime(recycledStringBuilder, elapsedSeconds);
        }
    }

    /**
     * Formats a millisecond duration in localized [hh:]mm:ss.f format <em>with attached
     * styles</em>.<p/>
     *
     * QUESTION: Does {@link #DEFAULT_TIME_STYLE} need to be localized for any locale? Do RTL
     * locales need to put the fractional part before the HHMMSS part? If so, make the caller get it
     * from a string resource.
     */
    public static Spanned formatHhMmSsFraction(long elapsedMilliseconds) {
        String hhmmss = formatHhMmSs(elapsedMilliseconds);
        double seconds = elapsedMilliseconds / 1000.0;
        String f;
        String html;

        makeLocaleFormatters();

        synchronized (recycledStringBuilder) {
            recycledStringBuffer.setLength(0);
            f = fractionFormatter.format(seconds, recycledStringBuffer, fractionFieldPosition)
                    .toString();

            recycledStringBuilder.setLength(0);
            html = recycledFormatter.format(DEFAULT_TIME_STYLE, hhmmss, f).toString();
        }

        return injected.fromHtml(html);
    }

    @NonNull
    @Override
    public String toString() {
        return "TimeCounter " + runState() + " @ " + formatHhMmSs(getElapsedTime());
    }
}
