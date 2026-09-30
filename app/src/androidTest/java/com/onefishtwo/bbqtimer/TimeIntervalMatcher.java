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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Description;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A Hamcrest Matcher that matches a time string in the format "MM:SS" or "MM:SS.F" within the
 * interval [min, max] milliseconds. It stores the time value parsed from each match attempt in its
 * instance variable {@link #time} so the caller can compute a relative time for the next test.
 * <p>
 * This is for testing the app's count-up (00:00.0) and count-down (00:00) Views. Test values should
 * never be larger than a minute or two, so there's no reason to support hours (0:00:00.0).
 */
public class TimeIntervalMatcher extends CustomTypeSafeMatcher<String> {
    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d\\d):(\\d\\d)[.,](\\d)");
    private static final Pattern SHORT_PATTERN = Pattern.compile("(\\d\\d):(\\d\\d)");

    private final long min, max;
    private final boolean expectFraction;

    /** The time value parsed from the last match attempt (in milliseconds). */
    public long time;

    public TimeIntervalMatcher(long minMs, long maxMs, boolean expectTenths) {
        super("a time string in [" + TimeCounter.formatHhMmSsFraction(minMs)
                + ", " + TimeCounter.formatHhMmSsFraction(maxMs) + "]");
        min = minMs;
        max = maxMs;
        expectFraction = expectTenths;
    }

    public TimeIntervalMatcher(long minMs, long maxMs) {
        this(minMs, maxMs, true);
    }

    /** Matches a time string "MM:SS.F" in the interval [min, max] milliseconds. */
    @NonNull
    public static TimeIntervalMatcher inTimeInterval(long min, long max) {
        return new TimeIntervalMatcher(min, max);
    }

    /** Matches a time string "MM:SS" in the interval [min, max] milliseconds. */
    @NonNull
    public static TimeIntervalMatcher inWholeTimeInterval(long min, long max) {
        return new TimeIntervalMatcher(min, max, false);
    }

    @Override
    protected boolean matchesSafely(@NonNull String item) {
        return describeMismatch(item) == null;
    }

    /**
     * Construct a helpful description of the mismatch, BUT currently this doesn't get called.
     * E.g. for `matches(withText(timeMatcher))` in an Espresso test, the `withText()` matcher
     * constructs the mismatch description without calling this method.
     */
    @Override
    protected void describeMismatchSafely(@NonNull String item,
                                          @NonNull Description mismatchDescription) {
        mismatchDescription.appendValue(item)
                .appendText(" ")
                .appendText(describeMismatch(item));
    }

    /** Returns null if `item` matches or a description of why it doesn't. */
    @Nullable
    private String describeMismatch(@NonNull CharSequence item) {
        Matcher m = (expectFraction ? TIME_PATTERN : SHORT_PATTERN).matcher(item);

        if (!m.matches()) {
            return "doesn't match the pattern " + m.pattern();
        }

        time = (val(m, 1) * 60L + val(m, 2)) * 1000L + val(m, 3) * 100L;
        return min <= time && time <= max ? null : "is outside the interval";
    }

    /**
     * Gets the integer value of a capturing group from the Matcher; or 0 if the group isn't in the
     * matcher.
     *
     * @throws NumberFormatException if the group is "" or otherwise not an integer.
     */
    private int val(@NonNull MatchResult matcher, int group) {
        if (group > matcher.groupCount()) {
            return 0;
        }

        final String s = matcher.group(group);
        return s == null ? 0 : Integer.parseInt(s);
    }
}
