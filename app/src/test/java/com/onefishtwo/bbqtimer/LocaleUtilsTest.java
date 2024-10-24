package com.onefishtwo.bbqtimer;

import static com.onefishtwo.bbqtimer.LocaleUtils.formatTemperatureFromFahrenheit;
import static com.onefishtwo.bbqtimer.LocaleUtils.getDefaultFormatLocale;
import static com.onefishtwo.bbqtimer.LocaleUtils.useFahrenheit;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

// In a unit test, Build.VERSION.SDK_INT == 0.
public class LocaleUtilsTest {
    private static final Locale BAHAMAS = new Locale("en", "BS");
    private static final Locale SPAIN = new Locale("es", "ES");

    private Locale initialLocale, initialFormatLocale;

    @Before
    public void setUp() {
        initialLocale = Locale.getDefault();
        initialFormatLocale = Locale.getDefault(Locale.Category.FORMAT);
    }

    @After
    public void tearDown() {
        Locale.setDefault(initialLocale);
        Locale.setDefault(Locale.Category.FORMAT, initialFormatLocale);
    }

    @Test
    public void testGetDefaultFormatLocale() {
        assertEquals(Locale.US, getDefaultFormatLocale());

        Locale.setDefault(Locale.GERMANY);
        assertEquals(Locale.GERMANY, getDefaultFormatLocale());
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
        Locale.setDefault(Locale.Category.FORMAT, Locale.GERMANY);

        assertEquals("0°C", formatTemperatureFromFahrenheit(32.0));
        assertEquals("100°C", formatTemperatureFromFahrenheit(212.0));
        assertEquals("63°C", formatTemperatureFromFahrenheit(145.0));
        assertEquals("74°C", formatTemperatureFromFahrenheit(165.0));

        // Test rounding
        assertEquals("100°C", formatTemperatureFromFahrenheit(212.8)); // 100.444°C
        assertEquals("101°C", formatTemperatureFromFahrenheit(212.9)); // 100.5°C
    }

}
