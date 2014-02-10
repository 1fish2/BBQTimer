/*
 * Copyright (c) 2014 Jerry Morrison.
 */

package com.onefishtwo.bbqtimer;

import android.os.SystemClock;

/**
 * A stopwatch time counter.
 */
public class TimeCounter {
    private static final String SHORT_FORMAT =      "%d:%02d.%01d"; //   mm:ss.f
    private static final String LONG_FORMAT  = "%d:%02d:%02d.%01d"; // h:mm:ss.f

    private boolean isRunning;
    private long startTime, pauseTime; // in milliseconds

    public TimeCounter() {
    }

    /** Returns true if the timer is running (not paused/stopped). */
    public boolean isRunning() {
        return isRunning;
    }

    /** Returns the running or paused elapsed time, in milliseconds. */
    public long getElapsedTime() {
        // TODO: Inject the clock for testability.
        return (isRunning ? SystemClock.elapsedRealtime() : pauseTime) - startTime;
    }

    /** Returns the running or stopped elapsed time, in [h:]mm:ss.f format. */
    public String getFormattedElapsedTime() {
        return formatTime(getElapsedTime());
    }

    /** Starts or resumes the timer. */
    public void start() {
        if (!isRunning) {
            startTime = SystemClock.elapsedRealtime() - (pauseTime - startTime);
            isRunning = true;
        }
    }

    /** Pauses the timer. */
    public void pause() {
        if (isRunning) {
            pauseTime = SystemClock.elapsedRealtime();
            isRunning = false;
        }
    }

    /** Stops and resets the timer. */
    public void reset() {
        startTime = pauseTime = 0;
        isRunning = false;
    }

    /** Formats a millisecond value in [h:]mm:ss.f format. */
    public static String formatTime(long milliseconds) {
        // TODO: Use android.text.format.DateUtils.formatElapsedTime() and append the fractional part?
        long fx       = milliseconds / 100; // time in tenths of a second
        long sx       = fx / 10;  // time in seconds, extended with minutes and hours
        long mx       = sx / 60;
        long hours    = mx / 60;
        long fraction = fx - sx * 10;
        long seconds  = sx - mx * 60;
        long minutes  = mx - hours * 60;

        if (hours > 0) {
            return String.format(LONG_FORMAT, hours, minutes, seconds, fraction);
        } else {
            return String.format(SHORT_FORMAT, minutes, seconds, fraction);
        }
    }
}
