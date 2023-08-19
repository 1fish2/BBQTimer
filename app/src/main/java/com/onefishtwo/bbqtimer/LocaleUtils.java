package com.onefishtwo.bbqtimer;

import android.os.Build;

import java.util.Locale;
import java.util.Set;

public class LocaleUtils {
    /**
     * <a href="https://worldpopulationreview.com/country-rankings/countries-that-use-fahrenheit">
     *     Countries that use fahrenheit</a> */
    private static final Set<String> FAHRENHEIT_COUNTRIES = Set.of(
            "US", // US
            "BS", // Bahamas
            "KY", // Cayman Islands
            "LR", // Liberia
            "PW", // Palau
            "FM", // Federated States of Micronesia
            "MH"  // Marshall Islands
    );

    /** Returns the default Locale for formatting dates, numbers, and/or currencies. */
    public static Locale getFormatDefault() {
        if (Build.VERSION.SDK_INT >= 24) {
            return Locale.getDefault(Locale.Category.FORMAT);
        } else {
            return Locale.getDefault();
        }
    }

    /** Indicates whether the locale should format temperatures in Fahrenheit °F rather than °C. */
    public static boolean useFahrenheit(Locale locale) {
        String country = locale.getCountry();
        return FAHRENHEIT_COUNTRIES.contains(country);
    }

    /** Indicates whether to format temperatures in Fahrenheit °F rather than °C. */
    public static boolean useFahrenheit() {
        Locale locale = getFormatDefault();
        return useFahrenheit(locale);
    }

    /** Format a temperature in Fahrenheit °F or Celsius °C, rounded to an integer. */
    public static String formatTemperatureFromFahrenheit(double fahrenheit) {
        Locale locale = getFormatDefault();

        if (useFahrenheit(locale)) {
            return String.format(locale, "%.0f°F", fahrenheit);
        } else {
            double celsius = (fahrenheit - 32.0) * 5 / 9;
            return String.format(locale, "%.0f°C", celsius);
        }
    }
}
