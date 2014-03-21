// The MIT License (MIT)
//
// Copyright (c) 2014 Jerry Morrison
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
 */
public class TimeCounter {
    /** PERSISTENT STATE identifiers. */
    static final String PREF_IS_RUNNING = "Timer_isRunning";
    static final String PREF_START_TIME = "Timer_startTime";
    static final String PREF_PAUSE_TIME = "Timer_pauseTime";

    /**
     * The default format string for combining and styling the hh:mm:ss + .f fractional seconds.</p>
     *
     * Format arg 1$ is the localized HH:MM:SS string.</p>
     *
     * Format arg 2$ is the fractional seconds string. This uses a NumberFormat instead of a string
     * format %.1d to suppress the integer part and to disable rounding.
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
    private long startTime; // when started, in system elapsed milliseconds
    private long pauseTime; // if !isRunning, when paused, in system elapsed milliseconds

    public TimeCounter() {
    }

    /** Saves state to a preferences editor. */
    public void save(SharedPreferences.Editor prefsEditor) {
        prefsEditor.putBoolean(PREF_IS_RUNNING, isRunning);
        prefsEditor.putLong(PREF_START_TIME, startTime);
        prefsEditor.putLong(PREF_PAUSE_TIME, pauseTime);
    }

    /** Loads state from a preferences object. */
    public void load(SharedPreferences prefs) {
        isRunning = prefs.getBoolean(PREF_IS_RUNNING, false);
        startTime = prefs.getLong(PREF_START_TIME, 0);
        pauseTime = prefs.getLong(PREF_PAUSE_TIME, 0);

        // Enforce invariants.
        if (isRunning) {
            if (startTime > elapsedRealtimeClock()) {
                // Must've rebooted.
                reset();
            }
        } else if (pauseTime < startTime) {
            reset();
        }
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

    /** Returns true if the timer is running (not paused/stopped). */
    public boolean isRunning() {
        return isRunning;
    }

    /** Returns the timer's [running or paused] elapsed time, in milliseconds. */
    public long getElapsedTime() {
        return (isRunning ? elapsedRealtimeClock() : pauseTime) - startTime;
    }

    /** Starts or resumes the timer. */
    public void start() {
        if (!isRunning) {
            startTime = elapsedRealtimeClock() - (pauseTime - startTime);
            isRunning = true;
        }
    }

    /** Pauses the timer. */
    public void pause() {
        if (isRunning) {
            pauseTime = elapsedRealtimeClock();
            isRunning = false;
        }
    }

    /**
     * Starts or pauses, i.e. toggles the running state.
     *
     * @return true if the timer is now running.
     */
    public boolean toggleRunning() {
        if (isRunning) {
            pause();
        } else {
            start();
        }
        return isRunning;
    }

    /** Cycles the state: Reset -> Running -> Paused -> Reset. */
    public void cycle() {
        if (isRunning()) {
            pause();
        } else if (getElapsedTime() == 0) {
            start();
        } else {
            reset();
        }
    }

    /** Stops and resets the timer. */
    public void reset() {
        startTime = pauseTime = 0;
        isRunning = false;
    }

    /** Returns true if the TimeCounter is in the Reset state, i.e. Paused at 0:00. */
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
     * QUESTION: Does DEFAULT_TIME_STYLE need to be localized for any locale? I.e. do RTL locales
     * need to put the fractional part before the HHMMSS part? If so, make the caller get it from
     * string resource R.string.time_format .
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
        return (isRunning ? "TimeCounter running @ " : "TimeCounter paused @ ")
                + formatHhMmSs(getElapsedTime());
    }
}
