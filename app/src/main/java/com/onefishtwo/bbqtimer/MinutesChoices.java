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

import java.util.Locale;

/**
 * Constructs locale-dependent UI strings for the "minutes picker" (to pick the number of minutes
 * between periodic alarms).
 */
public class MinutesChoices {

    /**
     * The number of "x.5" minute entries near the start of the array, counting by 0.5; all
     * following entries are whole numbers.
     */
    private static final int NUM_HALF_MINUTE_ENTRIES = 8;
    private static final int BEGIN_INDEX_WHOLE_MINUTES_ONLY = NUM_HALF_MINUTE_ENTRIES * 2 - 1;
    static final int MAX_MINUTES = 99;

    /**
     * The names of the periodic alarm minutesPicker choices:<br/>
     * {@code Index:   0, 1,   2, 3,   4, 5, ... 11,   12, ... 106} <br/>
     * {@code Name:  0.5, 1, 1.5, 2, 2.5, 3, ... 7.5,   8, ...  99}
     */
    final String[] choices = new String[MAX_MINUTES + NUM_HALF_MINUTE_ENTRIES];

    /**
     * Synthesizes a "quantity" to pass to Resources#getQuantityString() to get an integer or
     * decimal number of minutes pluralized correctly in the default locale. getQuantityString()
     * handles integers; hack it for decimals like "1.5".<p/>
     *
     * TODO: This handles English and German. More languages will require more cases.<p/>
     *
     * In English and German, "1" is in category "one" while all other cardinals (even "1.0") are in
     * category "other". [In some languages and English ordinals, 21, 31, ... are in category "one"
     * ("21st"), while 22, 32, ... are in category "two" ("32nd"), not to mention categories "few"
     * ("3rd") and "other" ("5th").] So for decimals, return an integer quantity that will select
     * the "other" category.<p/>
     *
     * https://code.google.com/p/android/issues/detail?id=63144 <br/>
     * http://cldr.unicode.org/index/charts > Supplemental > Language Plural Rules
     */
    static int secondsToMinutesPluralizationQuantity(int seconds) {
        return seconds % 60 == 0 ? seconds / 60 : 5;
    }

    /** Constructs a MinutesChoices for the default locale. */
    MinutesChoices() {
        updateLocale();

        // The whole-numbers-only part of the array is locale-independent.
        for (int i = BEGIN_INDEX_WHOLE_MINUTES_ONLY; i < choices.length; ++i) {
            int minutes = i - (NUM_HALF_MINUTE_ENTRIES - 1);
            choices[i] = Integer.toString(minutes);
        }
    }

    /** (Re)makes locale-dependent data. */
    final void updateLocale() {
        Locale locale = Locale.getDefault();

        for (int i = 0; i <= BEGIN_INDEX_WHOLE_MINUTES_ONLY; ++i) {
            int halfMinutes = i + 1;
            choices[i] = (halfMinutes % 2) == 0 ? Integer.toString(halfMinutes / 2)
                    : String.format(locale, "%,.1f", halfMinutes / 2.0F);
        }
    }

    /** Converts a minutesPicker choice index to a count of seconds/alarm. */
    static int pickerChoiceToSeconds(int choice) {
        return choice < BEGIN_INDEX_WHOLE_MINUTES_ONLY
                ? (choice + 1) * 30
                : (choice + 1 - NUM_HALF_MINUTE_ENTRIES) * 60;
    }

    /** Converts seconds/alarm to a valid minutesPicker choice index. */
    static int secondsToPickerChoice(int secondsPerAlarm) {
        if (secondsPerAlarm < 30) {
            secondsPerAlarm = 30;
        }

        int minutes = Math.min(secondsPerAlarm / 60, MAX_MINUTES);
        int seconds = secondsPerAlarm - minutes * 60;

        return minutes < NUM_HALF_MINUTE_ENTRIES
                ? minutes * 2 - 1 + (seconds < 30 ? 0 : 1)
                : minutes + (NUM_HALF_MINUTE_ENTRIES - 1);
    }

    /** Normalizes the input to a valid value. */
    public static int normalizeSeconds(int secondsPerAlarm) {
        return pickerChoiceToSeconds(secondsToPickerChoice(secondsPerAlarm));
    }

    /** Converts seconds/alarm to a minutesPicker choice string. */
    String secondsToMinuteChoiceString(int seconds) {
        return choices[secondsToPickerChoice(seconds)];
    }
}
