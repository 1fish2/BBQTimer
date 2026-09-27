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

    private static final Locale BAHAMAS = Locale.forLanguageTag("en-BS");
    private static final Locale SPAIN = Locale.forLanguageTag("es-ES");

    private Locale initialLocale;
    private Locale initialFormatLocale;

    @Before
    public void setUp() {
        initialLocale = Locale.getDefault();
        initialFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
        Log.i(TAG, "Locale " + initialLocale
                + ", Format Locale " + initialFormatLocale);
    }

    @After
    public void tearDown() {
        Locale.setDefault(initialLocale);
        Locale.setDefault(Locale.Category.FORMAT, initialFormatLocale);
    }

    @Test
    public void testGetDefaultFormatLocale() {
        // Going in, the Locale and the temperature units user pref are unknown.

        Locale.setDefault(Locale.GERMANY);
        assertEquals("Default locale should match GERMANY", Locale.GERMANY, getDefaultFormatLocale());

        Locale.setDefault(Locale.UK);
        Locale.setDefault(Locale.Category.FORMAT, Locale.ITALY);
        assertEquals("FORMAT category locale should match ITALY", Locale.ITALY, getDefaultFormatLocale());

        Locale.setDefault(Locale.Category.FORMAT, Locale.US);
        assertEquals("FORMAT category locale should match US", Locale.US, getDefaultFormatLocale());
    }

    @Test
    public void testUseFahrenheit() {
        // Going in, the Locale and the temperature units user pref are unknown.

        assertTrue("US should use Fahrenheit", useFahrenheit(Locale.US));
        assertTrue("Bahamas should use Fahrenheit", useFahrenheit(BAHAMAS));

        assertFalse("Germany should use Celsius", useFahrenheit(Locale.GERMANY));
        assertFalse("Spain should use Celsius", useFahrenheit(SPAIN));

        Locale.setDefault(Locale.UK);
        assertFalse("Default UK locale should use Celsius", useFahrenheit());

        Locale.setDefault(BAHAMAS);
        assertTrue("Default Bahamas locale should use Fahrenheit", useFahrenheit());

        Locale.setDefault(Locale.Category.FORMAT, Locale.UK);
        assertFalse("FORMAT category UK locale should use Celsius", useFahrenheit());
    }

    @Test
    public void testUseFahrenheit_withRegionalPreferences() {
        // Test Unicode BCP 47 measurement unit overrides (-u-mu-fahrenhe / -u-mu-celsius)
        Locale usCelsiusOverride = Locale.forLanguageTag("en-US-u-mu-celsius");
        Locale ukFahrenheitOverride = Locale.forLanguageTag("en-GB-u-mu-fahrenhe");

        assertFalse("US locale with explicit Celsius extension should use Celsius",
                useFahrenheit(usCelsiusOverride));
        assertTrue("UK locale with explicit Fahrenheit extension should use Fahrenheit",
                useFahrenheit(ukFahrenheitOverride));
    }

}
