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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;

@RunWith(Parameterized.class)
public class PickerChoiceTest {

    @Parameterized.Parameters(name = "picker choice {0} <-> {1} secs")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { // {choice, seconds} pairs
                // 0.5, 1, 1.5, ... 7.5 minutes
                {0, 30}, {1, 60}, {2, 90}, {3, 120}, {4, 2 * 60 + 30}, {5, 3 * 60},
                {6, 3 * 60 + 30}, {7, 4 * 60}, {8, 4 * 60 + 30}, {9, 5 * 60}, {10, 5 * 60 + 30},
                {11, 6 * 60}, {12, 6 * 60 + 30}, {13, 7 * 60}, {14, 7 * 60 + 30},
                // 8, 9, 10, ... 98, 99 minutes
                {15, 8 * 60}, {16, 9 * 60}, {17, 10 * 60}, {105, 98 * 60}, {106, 99 * 60},

                // See (unparameterized) MinutesChoicesTest for boundary cases.
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
        assertThat("picker choice", MinutesChoices.pickerChoiceToSeconds(choice), is(seconds));
        assertThat("back to seconds", MinutesChoices.secondsToPickerChoice(seconds), is(choice));
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
