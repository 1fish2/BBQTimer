/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2016 Jerry Morrison
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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;

@RunWith(Parameterized.class)
public class PickerChoiceTest {

    @Parameterized.Parameters(name = "picker choice {0} <-> {1} secs")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { // {choice, seconds} pairs
                // 0.5, 1, 1.5, ... 4.5 min
                {0, 30}, {1, 60}, {2, 90}, {3, 120}, {4, 2 * 60 + 30}, {5, 3 * 60},
                {6, 3 * 60 + 30}, {7, 4 * 60}, {8, 4 * 60 + 30},
                // 5, 6, 7, ... 98, 99 min
                {9, 5 * 60}, {10, 6 * 60}, {11, 7 * 60}, {102, 98 * 60}, {103, 99 * 60},
        });
    }

    @Parameterized.Parameter
    public int choice;

    @Parameterized.Parameter(value = 1)
    public int seconds;

    private MinutesChoices mc;

    @Before
    public void before() {
        mc = new MinutesChoices();
    }

    @Test
    public void choiceToFromSeconds() {
        assertThat("picker choice", mc.pickerChoiceToSeconds(choice), is(seconds));
        assertThat("back to seconds", mc.secondsToPickerChoice(seconds), is(choice));

        if (seconds == 30) { // hack in some non-parameterized boundary test cases
            assertThat("10 seconds", mc.secondsToPickerChoice(10), is(0));
            assertThat("0 seconds", mc.secondsToPickerChoice(0), is(0));
        }
    }

    @Test
    public void secondsToMinuteString() {
        assumeThat(Locale.getDefault(), is(Locale.US));

        int minutes = seconds / 60;
        int remainder = seconds % 60;
        String minuteString = Integer.toString(minutes);
        String expected = remainder == 0 ? minuteString : (minuteString + ".5");

        assertThat("choice string", mc.secondsToMinuteChoiceString(seconds), is(expected));
    }
}
