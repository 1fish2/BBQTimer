/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Jerry Morrison
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

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isRoot;
import static com.github.takahirom.roborazzi.RoborazziKt.captureRoboImage;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.github.takahirom.roborazzi.DefaultFileNameGenerator;
import com.github.takahirom.roborazzi.RoborazziContextKt;
import com.github.takahirom.roborazzi.RoborazziRule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

/**
 * JVM Screenshot tests using Roborazzi and Robolectric.
 */
@RunWith(AndroidJUnit4.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ScreenshotTest {

    @Rule
    public RoborazziRule roborazziRule = new RoborazziRule(new RoborazziRule.Options());

    private void capture() {
        captureRoboImage(onView(isRoot()), 
            DefaultFileNameGenerator.INSTANCE.generateFilePath("png"), 
            RoborazziContextKt.provideRoborazziContext().getOptions());
    }

    @Test
    public void captureDefault() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            capture();
        }
    }

    @Config(qualifiers = "night")
    @Test
    public void captureDarkMode() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            capture();
        }
    }

    @Config(fontScale = 1.5f)
    @Test
    public void captureHighFontScale() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            capture();
        }
    }

    @Config(qualifiers = "de")
    @Test
    public void captureGerman() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            capture();
        }
    }

    @Config(qualifiers = "land")
    @Test
    public void captureLandscape() {
        try (ActivityScenario<MainActivity> ignored = ActivityScenario.launch(MainActivity.class)) {
            capture();
        }
    }
}
