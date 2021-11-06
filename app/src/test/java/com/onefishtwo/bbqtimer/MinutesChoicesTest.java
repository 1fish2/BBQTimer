// The MIT License (MIT)
//
// Copyright (c) 2016 Jerry Morrison
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
// associated documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or
// substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
// NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.onefishtwo.bbqtimer;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Locale;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeThat;

public class MinutesChoicesTest {
    private final int MM = MinutesChoices.MAX_MINUTES;
    private static final int NUM_MINUTE_CHOICES = 107;
    private static final String[] FIRST_EN_MINUTE_STRINGS = {
            "0.5", "1", "1.5", "2", "2.5", "3", "3.5", "4", "4.5", "5", "5.5", "6", "6.5",
            "7", "7.5", "8", "9", "10"};
    private final String MMS = Integer.toString(MM);

    private Locale savedLocale;
    private MinutesChoices mc;

    @Before
    public void before() {
        // Cf. http://stackoverflow.com/a/21810126/1682419 to also change Android resources.
        savedLocale = Locale.getDefault();
        mc = new MinutesChoices();
    }

    @After
    public void after() {
        Locale.setDefault(savedLocale);
    }

    @Test
    public void enUsStrings() {
        assumeThat(Locale.getDefault(), is(Locale.US));

        assertThat("minute choices", Arrays.asList(mc.choices),
                ListPrefixMatcher.listPrefixMatcher(FIRST_EN_MINUTE_STRINGS));
        assertThat("number of minute choices", mc.choices.length, is(NUM_MINUTE_CHOICES));
        assertThat("the last minute choice", mc.choices[mc.choices.length - 1], is(MMS));
        assertThat("the penultimate minute choice", mc.choices[mc.choices.length - 2], is("98"));
    }

    @Test
    public void deDeStrings() {
        String[] de_first_choices = new String[FIRST_EN_MINUTE_STRINGS.length];
        for (int i = 0; i < de_first_choices.length; ++i) {
            de_first_choices[i] = FIRST_EN_MINUTE_STRINGS[i].replace('.', ',');
        }

        assumeThat(Locale.getDefault(), is(Locale.US));

        Locale.setDefault(Locale.GERMANY);

        // mc has not been updated to the new locale.
        assertThat("minute choices", Arrays.asList(mc.choices),
                ListPrefixMatcher.listPrefixMatcher(FIRST_EN_MINUTE_STRINGS));
        assertThat("number of minute choices", mc.choices.length, is(NUM_MINUTE_CHOICES));

        // Update mc to German.
        mc.updateLocale();
        assertThat("minute choices", Arrays.asList(mc.choices),
                ListPrefixMatcher.listPrefixMatcher(de_first_choices));
        assertThat("number of minute choices", mc.choices.length, is(NUM_MINUTE_CHOICES));
        assertThat("the last minute choice", mc.choices[mc.choices.length - 1], is(MMS));
    }

    @Test
    public void boundaryCases() {
        final int MM1 = MM + 1;

        assertThat("10 seconds", MinutesChoices.secondsToPickerChoice(10), is(0));
        assertThat("0 seconds", MinutesChoices.secondsToPickerChoice(0), is(0));

        assertThat("0 minutes", MinutesChoices.secondsToPickerChoice(0), is(0));
        assertThat("-0.5 minutes", MinutesChoices.secondsToPickerChoice(-30), is(0));
        assertThat("-1 minutes", MinutesChoices.secondsToPickerChoice(-60), is(0));

        assertThat("max minutes", MinutesChoices.secondsToPickerChoice(MM * 60), is(106));
        assertThat("1 too many minutes", MinutesChoices.secondsToPickerChoice(MM1 * 60), is(106));
        assertThat("1 too many secs", MinutesChoices.secondsToPickerChoice(MM * 60 + 1), is(106));

        assertThat("max minutes", mc.secondsToMinuteChoiceString(MM * 60), is(MMS));
        assertThat("1 too many minutes", mc.secondsToMinuteChoiceString(MM1 * 60), is(MMS));
    }

    @Test
    public void normalizeSeconds() {
        assertThat("negative seconds", MinutesChoices.normalizeSeconds(-90), is(30));
        assertThat("0 seconds", MinutesChoices.normalizeSeconds(0), is(30));
        assertThat("just over 90 seconds", MinutesChoices.normalizeSeconds(91), is(90));
        assertThat("almost 2 minutes", MinutesChoices.normalizeSeconds(119), is(90));
        assertThat("big", MinutesChoices.normalizeSeconds(2000 * 60 + 1), is(MM * 60));
    }
}
