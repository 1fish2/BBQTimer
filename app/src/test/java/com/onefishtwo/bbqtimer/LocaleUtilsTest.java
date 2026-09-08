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
    private static final Locale BAHAMAS = Locale.forLanguageTag("es-BS");
    private static final Locale SPAIN = Locale.forLanguageTag("es-ES");

    private Locale initialLocale, initialFormatLocale;

    @Before
    public void setUp() {
        initialLocale = Locale.getDefault();
        initialFormatLocale = Locale.getDefault(Locale.Category.FORMAT);

        setSystemLocales(Locale.US); // deterministic test
    }

    @After
    public void tearDown() {
        Locale.setDefault(initialLocale);
        Locale.setDefault(Locale.Category.FORMAT, initialFormatLocale);
    }

    private void setSystemLocales(Locale locale) {
        Locale.setDefault(locale);
        Locale.setDefault(Locale.Category.FORMAT, locale);
    }

    @Test
    public void testGetDefaultFormatLocale() {
        assertEquals(Locale.US, getDefaultFormatLocale());

        Locale.setDefault(Locale.GERMANY);
        assertEquals(Locale.GERMANY, getDefaultFormatLocale());
    }

    @Test
    public void testUseFahrenheit_withExplicitLocal() {
        assertTrue(useFahrenheit(Locale.US));
        assertTrue(useFahrenheit(BAHAMAS));

        assertFalse(useFahrenheit(Locale.GERMANY));
        assertFalse(useFahrenheit(SPAIN));
    }

    @Test
    public void testUseFahrenheit_withDefaultLocale() {
        assertTrue(useFahrenheit());

        Locale.setDefault(Locale.UK);
        assertFalse(useFahrenheit());
    }

    @Test
    public void testFormatTemperatureFromFahrenheit_inFahrenheit() {
        assertEquals("32°F", formatTemperatureFromFahrenheit(32.0));
        assertEquals("212°F", formatTemperatureFromFahrenheit(212.0));
        assertEquals("145°F", formatTemperatureFromFahrenheit(145.0));
        assertEquals("165°F", formatTemperatureFromFahrenheit(165.0));
    }

    @Test
    public void testFormatTemperatureFromFahrenheit_inCelsius() {
        setSystemLocales(Locale.GERMANY);

        assertEquals("0°C", formatTemperatureFromFahrenheit(32.0));
        assertEquals("100°C", formatTemperatureFromFahrenheit(212.0));
        assertEquals("63°C", formatTemperatureFromFahrenheit(145.0));
        assertEquals("74°C", formatTemperatureFromFahrenheit(165.0));
    }

    @Test
    public void testFormatTemperatureFromFahrenheit_rounding() {
        assertEquals("100°F", formatTemperatureFromFahrenheit(100.499));
        assertEquals("101°F", formatTemperatureFromFahrenheit(100.5));

        setSystemLocales(Locale.GERMANY);

        assertEquals("0°C", formatTemperatureFromFahrenheit(32.0));
        assertEquals("100°C", formatTemperatureFromFahrenheit(212.0));
        assertEquals("63°C", formatTemperatureFromFahrenheit(145.0));
        assertEquals("74°C", formatTemperatureFromFahrenheit(165.0));

        assertEquals("100°C", formatTemperatureFromFahrenheit(212.8)); // 100.444°C
        assertEquals("101°C", formatTemperatureFromFahrenheit(212.9)); // 100.5°C
    }

    @Test
    public void testFormatTemperatureFromFahrenheit_negativeAndFreezing() {
        assertEquals("-40°F", formatTemperatureFromFahrenheit(-40.0, Locale.US));
        assertEquals("-40°C", formatTemperatureFromFahrenheit(-40.0, Locale.GERMANY));
        assertEquals("0°F", formatTemperatureFromFahrenheit(0.0, Locale.US));
        assertEquals("-18°C", formatTemperatureFromFahrenheit(0.0, Locale.GERMANY));
    }

    @Test
    public void testFormatTemperatureFromFahrenheitWithExplicitLocale() {
        assertEquals("32°F", formatTemperatureFromFahrenheit(32.0, Locale.US));
        assertEquals("0°C", formatTemperatureFromFahrenheit(32.0, Locale.GERMANY));
    }

}
