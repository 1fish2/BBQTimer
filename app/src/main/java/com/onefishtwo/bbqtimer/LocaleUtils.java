// The MIT License (MIT)
//
// Copyright (c) 2022 Jerry Morrison
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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.text.util.LocalePreferences;

import java.util.Locale;

/**
 * Utility methods for locale-aware formatting and preferences.
 */
public final class LocaleUtils {

    private LocaleUtils() {
        // Private constructor to prevent instantiation of utility class
    }

    /** Returns the default Locale intended for formatting dates, numbers, and/or currencies. */
    @NonNull
    public static Locale getDefaultFormatLocale() {
        return Locale.getDefault(Locale.Category.FORMAT);
    }

    /**
     * Indicates whether the given locale should format temperatures in Fahrenheit °F rather
     * than Celsius °C.
     * <p>
     * To clarify {@link LocalePreferences#getTemperatureUnit(Locale)}:
     * <p style="margin-left: 30px">
     *   Returns any user regional preference temperature unit (from the Locale's extension value)
     *   on Android 14+, else optionally "resolves" a fallback from the Locale, else returns "".
     *   On API 33+ the fallback comes from a NumberFormatter; on older APIs it's inferred from the
     *   Locale's country code -- only {"BS", "BZ", "KY", "PR", "PW", "US"} use Fahrenheit.
     * </p>
     */
    public static boolean useFahrenheit(@NonNull Locale locale) {
        String temperatureUnit = LocalePreferences.getTemperatureUnit(locale);
        return LocalePreferences.TemperatureUnit.FAHRENHEIT.equals(temperatureUnit);
    }

    /** Indicates whether to format temperatures in Fahrenheit °F rather than °C. */
    @SuppressWarnings("unused")
    @RestrictTo(RestrictTo.Scope.TESTS)
    public static boolean useFahrenheit() {
        Locale locale = getDefaultFormatLocale();
        return useFahrenheit(locale);
    }

    /** Format a temperature in Fahrenheit °F or Celsius °C, rounded to an integer. */
    @NonNull
    public static String formatTemperatureFromFahrenheit(double fahrenheit) {
        return formatTemperatureFromFahrenheit(fahrenheit, getDefaultFormatLocale());
    }

    /**
     * Format a temperature in Fahrenheit °F or Celsius °C for a specified locale, rounded to an integer.
     */
    @NonNull
    public static String formatTemperatureFromFahrenheit(double fahrenheit, @NonNull Locale locale) {
        if (useFahrenheit(locale)) {
            return String.format(locale, "%.0f°F", fahrenheit);
        } else {
            double celsius = (fahrenheit - 32.0) * 5.0 / 9.0;
            return String.format(locale, "%.0f°C", celsius);
        }
    }
}
