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
import android.os.SystemClock;
import android.text.Html;
import android.text.Spanned;
import android.text.format.DateUtils;

import java.math.RoundingMode;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.util.Formatter;

/**
 * A stopwatch time counter (data model).
 * <p/>
 * The run states are {Running, Paused, Stopped}, where Paused is like Stopped plus an ongoing
 * Notification so it can be viewed and resumed on the Android Lollipop lock screen.
 */
public class TimeCounter {
    /** PERSISTENT STATE identifiers. */
    static final String PREF_IS_RUNNING = "Timer_isRunning";
    static final String PREF_IS_PAUSED  = "Timer_isPaused";  // new in app versionCode 10
    static final String PREF_START_TIME = "Timer_startTime";
    static final String PREF_PAUSE_TIME = "Timer_pauseTime";

    /**
     * The default format string for assembling and HTML-styling a timer duration.</p>
     *
     * Format arg %1$s is a placeholder for the already-localized HH:MM:SS string from
     * DateUtils.formatElapsedTime(), e.g. "00:00" or "0:00:00".</p>
     *
     * Format arg %2$s is a placeholder for the already-localized fractional seconds string, e.g.
     * ".0". The code uses a NumberFormat to format that string instead of an inline format %.1d to
     * suppress the integer part and disable rounding. It's wrapped in @{code <small>} HTML tags to
     * make the rapidly changing fractional part less distracting and to make it fit better on
     * screen. This way, "12:34:56.7" fits on a Galaxy Nexus screen.
     */
    public static final String DEFAULT_TIME_STYLE = "%1$s<small><small>%2$s</small></small>";

    // Synchronized on recycledStringBuilder.
    // The buffer is big enough for hhh:mm:ss.f + HTML markup = 11 + 30, and rounded up.
    private static final StringBuilder recycledStringBuilder = new StringBuilder(44);
    private static final Formatter recycledFormatter = new Formatter(recycledStringBuilder);

    // Synchronized on fractionFormatter.
    private static final NumberFormat fractionFormatter = NumberFormat.getNumberInstance();
    private static final StringBuffer recycledStringBuffer = new StringBuffer(2);
    private static final FieldPosition fractionFieldPosition =
            new FieldPosition(NumberFormat.FRACTION_FIELD);

    static {
        fractionFormatter.setMinimumIntegerDigits(0);
        fractionFormatter.setMaximumIntegerDigits(0);
        fractionFormatter.setMinimumFractionDigits(1);
        fractionFormatter.setMaximumFractionDigits(1);
        fractionFormatter.setRoundingMode(RoundingMode.DOWN);
    }

    private boolean isRunning;
    private boolean isPaused;  // distinguishes Paused from Stopped (if !isRunning)
    private long startTime; // elapsedRealtimeClock() when the timer was started
    private long pauseTime; // elapsedRealtimeClock() when the timer was paused

    public TimeCounter() {
    }

    /** Saves state to a preferences editor. */
    public void save(SharedPreferences.Editor prefsEditor) {
        prefsEditor.putBoolean(PREF_IS_RUNNING, isRunning);
        prefsEditor.putBoolean(PREF_IS_PAUSED, isPaused);
        prefsEditor.putLong(PREF_START_TIME, startTime);
        prefsEditor.putLong(PREF_PAUSE_TIME, pauseTime);
    }

    /**
     * Loads state from a preferences object. Enforces invariants and normalizes the state.
     *
     * @return true if the caller should {@link #save} the normalized state to ensure consistent
     * results. That happens when the state was running/paused with future startTime, indicating a
     * reboot. That check only helps when {@link #load} runs within startTime after reboot so it's
     * important to save the normalized state.
     */
    public boolean load(SharedPreferences prefs) {
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

    /** Returns the underlying clock time. */
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
     * Returns the Timer's Running/Paused/Stopped state for debugging. Not localized.
     *
     * @see com.onefishtwo.bbqtimer.Notifier#timerRunState(TimeCounter)
     */
    public String runState() {
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

    /** Stops and resets the timer to 0:00. */
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
     * Toggles the state to Running or Paused.
     *
     * @return true if the timer is now running.
     */
    public boolean toggleRunPause() {
        if (isRunning) {
            pause();
        } else {
            start();
        }
        return isRunning;
    }

    /** Cycles the state: Paused at 0:00 or Stopped -> Running -> Paused -> Stopped. */
    public void cycle() {
        if (isRunning()) {
            pause();
        } else if (isStopped() || isReset()) {
            start();
        } else {
            stop();
        }
    }

    /** Resets the timer to 0:00; pausing if it was Running or letting it remain Stopped. */
    public void reset() {
        startTime = pauseTime = 0;
        if (isRunning) {
            isRunning = false;
            isPaused  = true;
        } // else remain Paused or Stopped
    }

    /** Returns true if the TimeCounter is Stopped/Paused at 0:00. */
    public boolean isReset() {
        return !isRunning && getElapsedTime() == 0;
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

        synchronized (recycledStringBuilder) {
            return DateUtils.formatElapsedTime(recycledStringBuilder, elapsedSeconds);
        }
    }

    /**
     * Formats a millisecond duration in localized [hh:]mm:ss.f format <em>with attached
     * styles</em>.</p>
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

        synchronized (fractionFormatter) {
            recycledStringBuffer.setLength(0);
            f = fractionFormatter.format(seconds, recycledStringBuffer, fractionFieldPosition)
                    .toString();
        }

        synchronized (recycledStringBuilder) {
            recycledStringBuilder.setLength(0);
            html = recycledFormatter.format(DEFAULT_TIME_STYLE, hhmmss, f).toString();
        }

        return Html.fromHtml(html);
    }

    @Override
    public String toString() {
        return "TimeCounter " + runState() + " @ " + formatHhMmSs(getElapsedTime());
    }
}
