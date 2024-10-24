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


import static com.onefishtwo.bbqtimer.LocaleUtils.getDefaultFormatLocale;
import static com.onefishtwo.bbqtimer.LocaleUtils.useFahrenheit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.os.Build;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Locale;

@RunWith(AndroidJUnit4.class)
public class LocaleUtilsAndroidTest {
    private static final String TAG = "LocaleUtilsAndroidTest";

    private static final Locale BAHAMAS = new Locale("en", "BS");
    private static final Locale SPAIN = new Locale("es", "ES");

    private Locale initialLocale, initialFormatLocale;

    @Before
    public void setUp() {
        initialLocale = Locale.getDefault();

        if (Build.VERSION.SDK_INT >= 24) {
            initialFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
            Log.i(TAG, "Locale " + initialLocale
                    + ", Format Locale " + initialFormatLocale);
        }
    }

    @After
    public void tearDown() {
        Locale.setDefault(initialLocale);

        if (Build.VERSION.SDK_INT >= 24) {
            Locale.setDefault(Locale.Category.FORMAT, initialFormatLocale);
        }
    }

    @Test
    public void testGetDefaultFormatLocale() {
        // Going in, the Locale and the temperature units user pref are unknown.

        Locale.setDefault(Locale.GERMANY);
        assertEquals(Locale.GERMANY, getDefaultFormatLocale());

        if (Build.VERSION.SDK_INT >= 24) {
            Locale.setDefault(Locale.UK);
            Locale.setDefault(Locale.Category.FORMAT, Locale.ITALY);
            assertEquals(Locale.ITALY, getDefaultFormatLocale());

            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            assertEquals(Locale.US, getDefaultFormatLocale());
        }
    }

    @Test
    public void testUseFahrenheit() {
        // Going in, the Locale and the temperature units user pref are unknown.

        assertTrue(useFahrenheit(Locale.US));
        assertTrue(useFahrenheit(BAHAMAS));

        assertFalse(useFahrenheit(Locale.GERMANY));
        assertFalse(useFahrenheit(SPAIN));

        Locale.setDefault(Locale.UK);
        assertFalse(useFahrenheit());

        if (Build.VERSION.SDK_INT >= 24) {
            Locale.setDefault(BAHAMAS);
            assertTrue(useFahrenheit());

            Locale.setDefault(Locale.Category.FORMAT, Locale.UK);
            assertFalse(useFahrenheit());

            // TODO: Simulate a user's Regional preference by setting the format locale with
            //  extension tag, e.g. to en_US_#u-mu-fahrenhe.
        }
    }

}
