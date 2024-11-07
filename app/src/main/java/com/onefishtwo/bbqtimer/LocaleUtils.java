package com.onefishtwo.bbqtimer;

import androidx.annotation.NonNull;
import androidx.core.text.util.LocalePreferences;

import java.util.Locale;

public class LocaleUtils {

    /** Returns the default Locale intended for formatting dates, numbers, and/or currencies. */
    public static Locale getDefaultFormatLocale() {
        return Locale.getDefault(Locale.Category.FORMAT);
    }

    /** Indicates whether the given locale should format temperatures in Fahrenheit °F rather
     * than Celsius °C.
     * </p>
     * To clarify LocalePreferences.getTemperatureUnit(Locale locale, boolean resolved):
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
    public static boolean useFahrenheit() {
        Locale locale = getDefaultFormatLocale();
        return useFahrenheit(locale);
    }

    /** Format a temperature in Fahrenheit °F or Celsius °C, rounded to an integer. */
    @NonNull
    public static String formatTemperatureFromFahrenheit(double fahrenheit) {
        Locale locale = getDefaultFormatLocale();

        if (useFahrenheit(locale)) {
            return String.format(locale, "%.0f°F", fahrenheit);
        } else {
            double celsius = (fahrenheit - 32.0) * 5 / 9;
            return String.format(locale, "%.0f°C", celsius);
        }
    }
}
