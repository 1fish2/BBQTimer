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

import android.support.annotation.NonNull;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;

import java.util.regex.Pattern;

/**
 * A Matcher that matches a time string in the format "MM:SS.F" within the interval [min .. max]
 * milliseconds. It stores the time value parsed from each match attempt in the instance variable
 * {@link #time} so the caller can use a relative time in the next match.
 */
public class TimeIntervalMatcher extends CustomTypeSafeMatcher<String> {
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d\\d):(\\d\\d)[.,](\\d)");

    private final long min, max;

    /** The time value parsed from the last match attempt. */
    public long time;

    public TimeIntervalMatcher(long min, long max) {
        super("a time string in [" + TimeCounter.formatHhMmSsFraction(min)
                + " .. " + TimeCounter.formatHhMmSsFraction(max) + "]");
        this.min = min;
        this.max = max;
    }

    /** Matches a time string "MM:SS.F" in the interval [min .. max] milliseconds. */
    @NonNull
    public static TimeIntervalMatcher inTimeInterval(long min, long max) {
        return new TimeIntervalMatcher(min, max);
    }

    @Override
    protected boolean matchesSafely(@NonNull String item) {
        java.util.regex.Matcher m = TIME_PATTERN.matcher(item);

        if (!m.matches()) {
            return false;
        }

        time = (val(m, 1) * 60 + val(m, 2)) * 1000 + val(m, 3) * 100;
        return min <= time && time <= max;
    }

    @Override
    protected void describeMismatchSafely(String item, @NonNull Description mismatchDescription) {
        mismatchDescription.appendText("expected ");
        super.describeTo(mismatchDescription);
        mismatchDescription.appendText("; was ").appendText(item);
    }

    /** Gets the integer value of a capturing group from the Matcher. */
    private int val(@NonNull java.util.regex.Matcher matcher, int group) {
        final String s = matcher.group(group);
        return s == null ? 0 : Integer.parseInt(s);
    }
}
