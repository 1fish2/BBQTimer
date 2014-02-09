/*
 * Copyright (c) 2014 Jerry Morrison.
 */

package com.onefishtwo.bbqtimer;

/**
 * A stopwatch time counter.
 */
public class TimeCounter {
    private static final String SHORT_FORMAT =      "%d:%02d.%01d"; //   mm:ss.f
    private static final String LONG_FORMAT  = "%d:%02d:%02d.%01d"; // h:mm:ss.f

    private long time;

    public long getTime() {
        return time;
    }

    public void setTime(long newTime) {
        if (newTime < 0) {
            newTime = 0;
        }
        time = newTime;
    }

    public long addMilliseconds(long ms) {
        setTime(time + ms);
        return time;
    }

    @Override
    public String toString() {
        long fx       = time / 100; // time in tenths of a second
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
