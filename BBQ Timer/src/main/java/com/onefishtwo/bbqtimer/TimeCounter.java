/*
 * Copyright (c) 2014 Jerry Morrison.
 */

package com.onefishtwo.bbqtimer;

import android.content.SharedPreferences;
import android.os.SystemClock;

/**
 * A stopwatch time counter.
 */
public class TimeCounter {
    private static final String SHORT_FORMAT =      "%d:%02d.%01d"; //   mm:ss.f
    private static final String LONG_FORMAT  = "%d:%02d:%02d.%01d"; // h:mm:ss.f

    /** PERSISTENT STATE identifiers. */
    public static final String PREF_IS_RUNNING = "isRunning";
    public static final String PREF_START_TIME = "startTime";
    public static final String PREF_PAUSE_TIME = "pauseTime";

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
            if (startTime > elapsedTime()) {
                // Must've rebooted.
                // TODO: How to detect reboot when startTime <= elapsedTime()?
                reset();
            }
        } else if (pauseTime < startTime) {
            reset();
        }
    }

    long elapsedTime() {
        // TODO: Inject the clock for testability.
        return SystemClock.elapsedRealtime();
    }

    /** Returns true if the timer is running (not paused/stopped). */
    public boolean isRunning() {
        return isRunning;
    }

    /** Returns the running or paused elapsed time, in milliseconds. */
    public long getElapsedTime() {
        return (isRunning ? elapsedTime() : pauseTime) - startTime;
    }

    /** Returns the running or stopped elapsed time, in [h:]mm:ss.f format. */
    public String getFormattedElapsedTime() {
        return formatTime(getElapsedTime());
    }

    /** Starts or resumes the timer. */
    public void start() {
        if (!isRunning) {
            startTime = elapsedTime() - (pauseTime - startTime);
            isRunning = true;
        }
    }

    /** Pauses the timer. */
    public void pause() {
        if (isRunning) {
            pauseTime = elapsedTime();
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
