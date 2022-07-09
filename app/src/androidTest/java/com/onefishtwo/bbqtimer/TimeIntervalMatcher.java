/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Jerry Morrison
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.onefishtwo.bbqtimer;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;

import java.util.regex.Pattern;

import androidx.annotation.NonNull;

/**
 * A Matcher that matches a time string in the format "MM:SS.F" within the interval [min .. max]
 * milliseconds. It stores the time value parsed from each match attempt in the instance variable
 * {@link #time} so the caller can use a relative time in the next match.
 */
public class TimeIntervalMatcher extends CustomTypeSafeMatcher<String> {
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d\\d):(\\d\\d)[.,](\\d)");
    private static final Pattern SHORT_PATTERN = Pattern.compile("(\\d\\d):(\\d\\d)");

    private final long min, max;
    private final boolean expectFraction;

    /** The time value parsed from the last match attempt. */
    public long time;

    public TimeIntervalMatcher(long _min, long _max, boolean _expectFraction) {
        super("a time string in [" + TimeCounter.formatHhMmSsFraction(_min)
                + " .. " + TimeCounter.formatHhMmSsFraction(_max) + "]");
        this.min = _min;
        this.max = _max;
        this.expectFraction = _expectFraction;
    }

    public TimeIntervalMatcher(long _min, long _max) {
        this(_min, _max, true);
    }

    /** Matches a time string "MM:SS.F" in the interval [min .. max] milliseconds. */
    @NonNull
    public static TimeIntervalMatcher inTimeInterval(long min, long max) {
        return new TimeIntervalMatcher(min, max);
    }

    /** Matches a time string "MM:SS" in the interval [min .. max] milliseconds. */
    @NonNull
    public static TimeIntervalMatcher inWholeTimeInterval(long min, long max) {
        return new TimeIntervalMatcher(min, max, false);
    }

    @Override
    protected boolean matchesSafely(@NonNull String item) {
        java.util.regex.Matcher m = (expectFraction ? TIME_PATTERN : SHORT_PATTERN).matcher(item);

        if (!m.matches()) {
            return false;
        }

        time = (val(m, 1) * 60L + val(m, 2)) * 1000L + val(m, 3) * 100L;
        return min <= time && time <= max;
    }

    @Override
    protected void describeMismatchSafely(String item, @NonNull Description mismatchDescription) {
        mismatchDescription.appendText("expected ");
        super.describeTo(mismatchDescription);
        mismatchDescription.appendText("; was ").appendText(item);
    }

    /**
     * Gets the integer value of a capturing group from the Matcher; or 0 if the group isn't in the
     * matcher.
     *
     * @throws NumberFormatException if the group is "" or otherwise not an integer.
     */
    private int val(@NonNull java.util.regex.Matcher matcher, int group) {
        if (group > matcher.groupCount()) {
            return 0;
        }

        final String s = matcher.group(group);
        return s == null ? 0 : Integer.parseInt(s);
    }
}
