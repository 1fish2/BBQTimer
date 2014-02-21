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

    @Override
    public String toString() {
        return (isRunning ? "TimeCounter running @ " : "TimeCounter paused @ ")
                + formatHhMmSs(getElapsedTime());
    }
}
