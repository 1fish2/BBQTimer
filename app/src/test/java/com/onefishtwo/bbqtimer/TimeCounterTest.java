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
 *
 */

package com.onefishtwo.bbqtimer;

import android.text.Spanned;

import org.junit.Test;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static android.text.format.DateUtils.HOUR_IN_MILLIS;
import static android.text.format.DateUtils.MINUTE_IN_MILLIS;
import static android.text.format.DateUtils.SECOND_IN_MILLIS;
import static com.onefishtwo.bbqtimer.TimeCounter.lengthOfLeadingIntervalTime;
import static com.onefishtwo.bbqtimer.TimeCounter.parseHhMmSs;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class TimeCounterTest {
    static class MockSpanned implements Spanned {
        @Nullable
        @Override
        public <T> T[] getSpans(int start, int end, Class<T> type) { return null; }

        @Override
        public int getSpanStart(Object tag) { return 0; }

        @Override
        public int getSpanEnd(Object tag) { return 0; }

        @Override
        public int getSpanFlags(Object tag) { return 0; }

        @Override
        public int nextSpanTransition(int start, int limit, Class type) { return 0; }

        @Override
        public int length() { return 0; }

        @Override
        public char charAt(int index) { return 0; }

        @NonNull
        @Override
        public CharSequence subSequence(int start, int end) { return ""; }

        @SuppressWarnings("EmptyMethod")  // an "empty" method just for the @NonNull annotation
        @NonNull
        @Override
        public String toString() {
            return super.toString();
        }
    }

    static class MockFormatHook extends TimeCounter.InjectForTesting {
        long inputMsec =
                987 * HOUR_IN_MILLIS + 45 * MINUTE_IN_MILLIS + 23 * SECOND_IN_MILLIS + 600;
        String hhmmssText = "987:45:23";
        String htmlText = hhmmssText + "<small>.6</small>";
        final MockSpanned mockSpanned = new MockSpanned();

        @NonNull
        @Override
        String formatElapsedTime(StringBuilder recycle, long elapsedSeconds) {
            assertNotNull(recycle);
            assertEquals(inputMsec / 1000, elapsedSeconds);
            return hhmmssText;
        }

        @NonNull
        @Override
        Spanned fromHtml(String source) {
            assertEquals(htmlText, source);
            return mockSpanned;
        }
    }

    /**
     * Unit test for formatHhMmSsFraction(); injects mocks of DateUtils.formatElapsedTime() and
     * Html.fromHtml() SINCE THE UNIT TEST VERSION OF android.jar DOESN'T IMPLEMENT THOSE METHODS OR
     * SPANNABLE STRINGS.
     */
    @Test
    public void testFormatHhMmSsFraction() {
        MockFormatHook hook = new MockFormatHook();

        TimeCounter.injected = hook;

        assertSame(hook.mockSpanned, TimeCounter.formatHhMmSsFraction(hook.inputMsec));
    }

    @Test
    public void testParseHhMmSs() {
        // seconds
        assertEquals(12, parseHhMmSs(":12"));
        assertEquals(99, parseHhMmSs(":99"));
        assertEquals(12, parseHhMmSs("0:12"));
        assertEquals(12, parseHhMmSs("00:000:012"));
        assertEquals(34, parseHhMmSs("  : 34  "));
        assertEquals(34, parseHhMmSs("  0  : 34  "));
        assertEquals(34, parseHhMmSs("  0 : 0  : 34  "));
        assertEquals(34, parseHhMmSs("  :  : 34  "));
        assertEquals(0, parseHhMmSs("  :: "));

        // minutes
        assertEquals(12 * 60, parseHhMmSs("12"));
        assertEquals(12 * 60, parseHhMmSs("12:0"));
        assertEquals(12 * 60, parseHhMmSs("12:000"));
        assertEquals(12 * 60, parseHhMmSs("12: 0 "));
        assertEquals(12 * 60, parseHhMmSs("012:"));
        assertEquals(12 * 60, parseHhMmSs(":12:"));
        assertEquals(34 * 60, parseHhMmSs("  34  "));
        assertEquals(34 * 60, parseHhMmSs("  34 : "));
        assertEquals(34 * 60, parseHhMmSs("  : 34 : "));
        assertEquals(34 * 60, parseHhMmSs(" 0 3 4 : "));
        assertEquals(34 * 60, parseHhMmSs(" 0 3 4 : 0 0 "));
        assertEquals(134 * 60, parseHhMmSs(" 1 3 4 : "));

        // minutes:seconds
        assertEquals(60 + 59, parseHhMmSs(" 1 : 59 "));
        assertEquals(6000 + 99, parseHhMmSs(" 100 : 99 "));
        assertEquals(3 * 60 + 12, parseHhMmSs("3:12"));

        // hours:minutes:seconds
        assertEquals(123 * 3600 + 4 * 60 + 5,
                parseHhMmSs("123:0004:0005"));

        // empty or just white space: no fields but split() returns [""]
        assertEquals(-1, parseHhMmSs(""));
        assertEquals(-1, parseHhMmSs("   "));

        // too many fields
        assertEquals(-1, parseHhMmSs(" ::: "));
        assertEquals(-1, parseHhMmSs(" : :  :  "));
        assertEquals(-1, parseHhMmSs("1:2:3:4"));
        assertEquals(-1, parseHhMmSs(" :1:: "));

        // bad fields
        assertEquals(-1, parseHhMmSs("2pm"));
        assertEquals(-1, parseHhMmSs("10:15 am"));
        assertEquals(-1, parseHhMmSs("-15"));
        assertEquals(-1, parseHhMmSs("-15"));
        assertEquals(-1, parseHhMmSs("-15:"));
        assertEquals(-1, parseHhMmSs(": -15 :"));
        assertEquals(-1, parseHhMmSs("10.20.2000"));
        assertEquals(-1, parseHhMmSs(" ten "));
    }

    /**
     * Make a test call to formatHhMmSsCompact(). When hours > 0, that calls
     * DateUtils.formatElapsedTime(), in which case this must mock out the underlying Formatter.
     * Otherwise it doesn't, so the mock returns the wrong string result there, thus testing that
     * formatHhMmSsCompact() doesn't use it.
     */
    private static String fc(long hours, long minutes, long seconds) {
        long totalSeconds = hours * 3600 + minutes * 60 + seconds;
        MockFormatHook hook = new MockFormatHook();

        hook.inputMsec = totalSeconds * 1000L;
        hook.hhmmssText = String.format("%d:%02d:%02d", hours, minutes, seconds);
        hook.htmlText = hook.hhmmssText;

        TimeCounter.injected = hook;

        return TimeCounter.formatHhMmSsCompact(totalSeconds * 1000L);
    }

    @Test
    public void testFormatHhMmSsCompact() {
        // No hours: formatHhMmSsCompact() does its own formatting as "m:ss" or "m".
        assertEquals("34:56", fc(0, 34, 56));
        assertEquals("4:56", fc(0, 4, 56));
        assertEquals("4", fc(0, 4, 0));
        assertEquals("24", fc(0, 24, 0));

        // With hours: formatHhMmSsCompact() calls DateUtils.formatElapsedTime(), which has to be
        // mocked for unit testing.
        assertEquals("2:34:56", fc(2, 34, 56));
        assertEquals("5:04:00", fc(5, 4, 0));
        assertEquals("15:00:00", fc(15, 0, 0));
    }

    @Test
    public void testLengthOfLeadingIntervalTime() {
        assertEquals(1, lengthOfLeadingIntervalTime("7"));
        assertEquals(4, lengthOfLeadingIntervalTime("  27"));
        assertEquals(3, lengthOfLeadingIntervalTime("127, notes go here"));
        assertEquals(5, lengthOfLeadingIntervalTime(" 3:15 "));
        assertEquals(8, lengthOfLeadingIntervalTime("\t 1:3:15 "));
        assertEquals(13, lengthOfLeadingIntervalTime(" 001:22:33:44 - notes"));
        assertEquals(0, lengthOfLeadingIntervalTime("x15"));
        assertEquals(3, lengthOfLeadingIntervalTime("\r\n7\r\n"));
        assertEquals(1, lengthOfLeadingIntervalTime("7.5"));
    }
}
