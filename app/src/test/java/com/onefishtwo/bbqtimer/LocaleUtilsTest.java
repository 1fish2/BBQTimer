package com.onefishtwo.bbqtimer;

import android.os.Build;

import junit.framework.TestCase;

import org.junit.Test;

import java.util.Locale;

import static com.onefishtwo.bbqtimer.LocaleUtils.formatTemperatureFromFahrenheit;
import static com.onefishtwo.bbqtimer.LocaleUtils.getFormatDefault;
import static com.onefishtwo.bbqtimer.LocaleUtils.useFahrenheit;

public class LocaleUtilsTest extends TestCase {
    private static final Locale BAHAMAS = new Locale("en", "BS");
    private static final Locale SPAIN = new Locale("es", "ES");

    private Locale initialLocale, initialFormatLocale;

    @Override
    protected void setUp() {
        initialLocale = Locale.getDefault();

        if (Build.VERSION.SDK_INT >= 24) {
            initialFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
        }
    }

    @Override
    protected void tearDown() {
        Locale.setDefault(initialLocale);

        if (Build.VERSION.SDK_INT >= 24) {
            Locale.setDefault(Locale.Category.FORMAT, initialFormatLocale);
        }
    }

    @Test
    public void testGetFormatDefault() {
        assertEquals(Locale.US, getFormatDefault());

        Locale.setDefault(Locale.GERMANY);
        assertEquals(Locale.GERMANY, getFormatDefault());

        if (Build.VERSION.SDK_INT >= 24) {
            Locale.setDefault(Locale.UK);

            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
            assertEquals(Locale.GERMANY, getFormatDefault());

            Locale.setDefault(Locale.Category.FORMAT, Locale.US);
            assertEquals(Locale.US, getFormatDefault());
        }
    }

    @Test
    public void testUseFahrenheit() {
        assertTrue(useFahrenheit(Locale.US));
        assertTrue(useFahrenheit(BAHAMAS));

        assertFalse(useFahrenheit(Locale.GERMANY));
        assertFalse(useFahrenheit(SPAIN));

        assertTrue(useFahrenheit());

        Locale.setDefault(Locale.UK);
        assertFalse(useFahrenheit());

        if (Build.VERSION.SDK_INT >= 24) {
            Locale.setDefault(BAHAMAS);
            Locale.setDefault(Locale.Category.FORMAT, Locale.UK);
            assertFalse(useFahrenheit());
        }
    }

    @Test
    public void testFormatTemperatureFromFahrenheit() {
        assertEquals("32°F", formatTemperatureFromFahrenheit(32.0));
        assertEquals("212°F", formatTemperatureFromFahrenheit(212.0));
        assertEquals("145°F", formatTemperatureFromFahrenheit(145.0));
        assertEquals("165°F", formatTemperatureFromFahrenheit(165.0));

        // Test rounding
        assertEquals("100°F", formatTemperatureFromFahrenheit(100.499));
        assertEquals("101°F", formatTemperatureFromFahrenheit(100.5));

        Locale.setDefault(Locale.GERMANY);
        if (Build.VERSION.SDK_INT >= 24) {
            Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);
        }

        assertEquals("0°C", formatTemperatureFromFahrenheit(32.0));
        assertEquals("100°C", formatTemperatureFromFahrenheit(212.0));
        assertEquals("63°C", formatTemperatureFromFahrenheit(145.0));
        assertEquals("74°C", formatTemperatureFromFahrenheit(165.0));

        // Test rounding
        assertEquals("100°C", formatTemperatureFromFahrenheit(212.8)); // 100.444°C
        assertEquals("101°C", formatTemperatureFromFahrenheit(212.9)); // 100.5°C
    }

}
