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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import static com.onefishtwo.bbqtimer.TimeCounter.parseHhMmSs;

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

    /**
     * Unit test for formatHhMmSsFraction(); injects mocks of DateUtils.formatElapsedTime() and
     * Html.fromHtml() since the unit test Android.jar doesn't implement them or even spannable
     * strings.
     */
    @Test
    public void testFormatHhMmSsFraction() {
        final long inputMsec =
                987 * HOUR_IN_MILLIS + 45 * MINUTE_IN_MILLIS + 23 * SECOND_IN_MILLIS + 600;
        final String hhmmssText = "987:45:23";
        final String htmlText = hhmmssText + "<small>.6</small>";
        final MockSpanned mockSpanned = new MockSpanned();

        TimeCounter.injected = new TimeCounter.InjectForTesting() {
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
        };

        assertSame(mockSpanned, TimeCounter.formatHhMmSsFraction(inputMsec));
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
        assertEquals(0, parseHhMmSs("  ::: "));

        // minutes
        assertEquals(12 * 60, parseHhMmSs("12"));
        assertEquals(12 * 60, parseHhMmSs("12:0"));
        assertEquals(12 * 60, parseHhMmSs("012:"));
        assertEquals(12 * 60, parseHhMmSs(":12:"));
        assertEquals(34 * 60, parseHhMmSs("  34  "));
        assertEquals(34 * 60, parseHhMmSs("  34 : "));
        assertEquals(34 * 60, parseHhMmSs(" :: : 34 : "));
        assertEquals(34 * 60, parseHhMmSs(" 0 34 : "));

        // minutes:seconds
        assertEquals(60 + 59, parseHhMmSs(" 1   59 "));
        assertEquals(60 + 59, parseHhMmSs(" 1 : 59 "));
        assertEquals(6000 + 59, parseHhMmSs(" 100 ::: 59 "));
        assertEquals(3 * 60 + 12, parseHhMmSs("3:12"));

        // hours:minutes:seconds
        assertEquals(123 * 3600 + 4 * 60 + 5,
                parseHhMmSs("123:0004:0005"));

        // empty or just white space: no fields but split() returns [""]
        assertEquals(-1, parseHhMmSs(""));
        assertEquals(-1, parseHhMmSs("   "));

        // too many fields
        assertEquals(-1, parseHhMmSs(" 0 0 34 : "));
        assertEquals(-1, parseHhMmSs(" 0 0 34 : 0 "));

        // bad fields
        assertEquals(-1, parseHhMmSs("-15"));
        assertEquals(-1, parseHhMmSs("-15:"));
        assertEquals(-1, parseHhMmSs(": -15 :"));
        assertEquals(-1, parseHhMmSs("10.20.2000"));
        assertEquals(-1, parseHhMmSs(" ten "));
    }
}
