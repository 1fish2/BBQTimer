/*
 * Copyright (c) 2014 Jerry Morrison.
 */

package com.onefishtwo.bbqtimer;

import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.format.DateUtils;

/**
 * A stopwatch time counter (data model).
 */
public class TimeCounter {
    private static final String SHORT_FORMAT =      "%02d:%02d.%01d"; //    mm:ss.f
    private static final String LONG_FORMAT  = "%02d:%02d:%02d.%01d"; // hh:mm:ss.f

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
            if (startTime > elapsedRealtime()) {
                // Must've rebooted.
                // TODO: How to detect reboot when startTime <= elapsedRealtime()?
                reset();
            }
        } else if (pauseTime < startTime) {
            reset();
        }
    }

    /** Returns the timer's start time, in elapsedRealtime() milliseconds. */
    public long getStartTime() {
        return startTime;
    }

    public long getPauseTime() {
        return pauseTime;
    }

    /** Returns the underlying clock time. */
    // TODO: Inject the clock for testability.
    long elapsedRealtime() {
        return SystemClock.elapsedRealtime();
    }

    /** Returns true if the timer is running (not paused/stopped). */
    public boolean isRunning() {
        return isRunning;
    }

    /** Returns the running or paused elapsed time, in milliseconds. */
    public long getElapsedTime() {
        return (isRunning ? elapsedRealtime() : pauseTime) - startTime;
    }

    /** Returns the running or stopped elapsed time, in [h:]mm:ss.f format. */
    public String getFormattedElapsedTime() {
        return formatTime(getElapsedTime());
    }

    /** Starts or resumes the timer. */
    public void start() {
        if (!isRunning) {
            startTime = elapsedRealtime() - (pauseTime - startTime);
            isRunning = true;
        }
    }

    /** Pauses the timer. */
    public void pause() {
        if (isRunning) {
            pauseTime = elapsedRealtime();
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

    /** Formats a millisecond duration in [hh:]mm:ss format like Chronometer does. */
    public static String formatHhMmSs(long elapsedMilliseconds) {
        long elapsedSeconds = elapsedMilliseconds / 1000;
        return DateUtils.formatElapsedTime(elapsedSeconds);
    }

    /** Formats a decimal fraction of a second of a millisecond duration, in .f format. */
    public static String formatFractionalSeconds(long elapsedMilliseconds) {
        long elapsedTenths = elapsedMilliseconds / 100;
        int tenths = (int)(elapsedTenths % 10);

        return String.format(".%1d", tenths);
    }

    /**
     * Formats a millisecond duration in [hh:]mm:ss.f format.
     * ([[h]h:][m]m:ss.f would look nicer but it wouldn't match Chronometer's formatting.
     */
    public static String formatTime(long elapsedMilliseconds) {
        // TODO: Use android.text.format.DateUtils.formatElapsedTime() and append the fractional part?
        long fx       = elapsedMilliseconds / 100; // time in tenths of a second
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
