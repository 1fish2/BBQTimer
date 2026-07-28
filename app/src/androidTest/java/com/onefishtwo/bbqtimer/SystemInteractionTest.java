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

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.RemoteException;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * System-level interaction tests using UI Automator.
 * Verifies behavior beyond the app's internal GUI, such as Notifications and App Shortcuts.
 * Requires an unlocked Android emulator or device.
 * <p>
 * TODO: Check that the app display changes properly with rotation, text scaling, etc.; also with
 *  the popup menu open and with the recipe editor open.
 * <p>
 * TODO: Speed up some of the delays.
 */
@RunWith(AndroidJUnit4.class)
public class SystemInteractionTest {
    private UiDevice device;
    private Context context;
    private static final String PACKAGE_NAME = "com.onefishtwo.bbqtimer";
    private static final int TIMEOUT = 5000;

    /**
     * Since `testInstrumentationRunnerArguments clearPackageData: 'true'` resets the app's state,
     * grant POST_NOTIFICATIONS permission before the test starts (on API levels that require
     * notifications permission) so it won't wait for a user to grant permissions.
     */
    @Rule
    public final GrantPermissionRule permissionRule =
            Build.VERSION.SDK_INT >= 33 ? GrantPermissionRule.grant(POST_NOTIFICATIONS)
            : null;

    @Before
    public void setUp() throws RemoteException {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = ApplicationProvider.getApplicationContext();

        if (!device.isScreenOn()) {
            device.wakeUp();
        }
        device.pressHome();
    }

    @After
    public void tearDown() throws IOException, RemoteException {
        // Try to dismiss any remaining popups/menus
        device.pressBack();
        device.pressHome();

        // Restore system settings
        device.executeShellCommand("cmd uimode night no");
        device.executeShellCommand("settings put system accelerometer_rotation 1");
        device.setOrientationNatural();

        // Ensure system UI settles before the next test (or Orchestrator) starts
        device.waitForIdle();
    }

    @NonNull
    private String getString(int resId) {
        return context.getString(resId);
    }

    private void launchApp(String action) {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(PACKAGE_NAME);

        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (action != null) {
                intent.setAction(action);
            }

            context.startActivity(intent);
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT);
        }
    }

    private void launchApp() {
        launchApp(null);
    }

    @Test
    public void testNotificationDrawer() {
        // Open the app with the "Paused at 00:00" shortcut to show a paused notification.
        launchApp(Intent.ACTION_QUICK_CLOCK);

        // Open notification drawer
        device.openNotification();

        // Wait for the BBQ Timer notification by package name
        boolean found = device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT);
        assertTrue("BBQ Timer notification (by package) should be visible", found);

        // BBQ Timer uses custom RemoteViews.
        // Find a button by its content description or text.
        String pauseStr = getString(R.string.pause);
        UiObject2 initialPauseButton = device.findObject(By.descContains(pauseStr));
        UiObject2 pauseButton = initialPauseButton != null ? initialPauseButton
                : device.findObject(By.textContains(pauseStr));

        assertNotNull("Pause button should be visible", pauseButton);

        pauseButton.click();
        // Verify it changed state.
        String runStr = getString(R.string.start);
        boolean updated = device.wait(Until.hasObject(By.descContains(runStr)), TIMEOUT)
                || device.wait(Until.hasObject(By.textContains(runStr)), TIMEOUT);
        assertTrue("Notification should update after clicking pause", updated);

        device.pressBack(); // Close drawer
    }

    @Test
    @SdkSuppress(minSdkVersion = 25)
    public void testAppShortcuts() {
        device.pressHome();

        // Find the app icon in the Pixel launcher's app drawer.
        // Note: If the icon is on the Home screen after the test infrastructure installed a test
        // app, it's a temporary icon added by the Predicted app icon feature. On Android 37.1 its
        // long-press menu has a non-standard toggle between "Actions" like "App info" and
        // "Shortcuts" defined by the app. Workaround: Use the app drawer, not the Home screen.
        device.pressHome();
        device.swipe(device.getDisplayWidth() / 2, device.getDisplayHeight() - 100,
                device.getDisplayWidth() / 2, 100, 10);
        
        String appName = getString(R.string.app_name);
        UiObject2 appIcon = device.wait(Until.findObject(By.text(appName)), TIMEOUT);

        assertNotNull("BBQ Timer app icon should be in the App Drawer", appIcon);

        // Long press (click with duration)
        appIcon.click(1500);

        // Use a Pattern to match either the short or long label
        String shortLabel = getString(R.string.start_at_0_short);
        String longLabel = getString(R.string.start_at_0_long);
        Pattern labelPattern = Pattern.compile(
                Pattern.quote(shortLabel) + "|" + Pattern.quote(longLabel));
        UiObject2 startShortcut = device.wait(Until.findObject(By.text(labelPattern)), TIMEOUT);
        assertNotNull("Start shortcut should be visible (short or long label)", startShortcut);

        startShortcut.click();
        assertTrue("App should be in foreground after shortcut click",
                device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT));

        // Check that the timer is running: the 'Run' button description should be gone.
        // Using Until.gone() with exact By.desc() is fast when the description changes.
        String runStr = getString(R.string.start);
        boolean runButtonGone = device.wait(Until.gone(By.desc(runStr)), TIMEOUT);
        assertTrue("Timer should be running (Run button should change to Pause)", runButtonGone);

        String pauseStr = getString(R.string.pause);
        UiObject2 pauseButton = device.wait(Until.findObject(By.desc(pauseStr)), TIMEOUT);
        assertNotNull("Timer should be running (Pause button visible)", pauseButton);

        // TODO: Why does this test take 40 seconds?
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testThemeToggle() throws IOException {
        device.executeShellCommand("cmd uimode night yes");
        try {
            launchApp();
            UiObject2 container = device.wait(
                    Until.findObject(By.res(PACKAGE_NAME, "main_container")), TIMEOUT);
            assertNotNull("App should be visible in Dark Mode", container);

            int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            assertEquals("Resources should reflect Night Mode", Configuration.UI_MODE_NIGHT_YES, nightMode);
        } finally {
            device.executeShellCommand("cmd uimode night no");
            int nightMode = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            assertEquals("Resources should reflect Light Mode", Configuration.UI_MODE_NIGHT_NO, nightMode);
        }
    }

    @Test
    public void testFontScale() throws IOException {
        String scale = device.executeShellCommand("settings get system font_scale").trim();
        final String originalScale = (scale.isEmpty() || scale.contains("null")) ? "1.0" : scale;

        try {
            device.executeShellCommand("settings put system font_scale 1.5");
            launchApp();
            UiObject2 container = device.wait(
                    Until.findObject(By.res(PACKAGE_NAME, "main_container")), TIMEOUT);
            assertNotNull("App should be visible with 1.5x font scale", container);

            float currentScale = context.getResources().getConfiguration().fontScale;
            assertEquals("App should have 1.5x font scale", 1.5f, currentScale, 0.01f);
        } finally {
            // Restore original font scale
            device.executeShellCommand("settings put system font_scale " + originalScale);
        }
    }

    @Test
    public void testRotation() throws IOException, RemoteException {
        device.executeShellCommand("settings put system accelerometer_rotation 0");
        try {
            launchApp();
            device.setOrientationLeft();
            // Wait for rotation to finish
            boolean rotated = device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT);
            assertTrue("App should still be visible after rotation", rotated);
            assertFalse("App should not be in natural orientation (should be landscape)",
                    device.isNaturalOrientation());

            int rotation = device.getDisplayRotation();
            assertTrue("Display should be rotated (LANDSCAPE)",
                    rotation == Surface.ROTATION_90 || rotation == Surface.ROTATION_270);

            device.setOrientationNatural();
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT);
        } finally {
            device.executeShellCommand("settings put system accelerometer_rotation 1");
            device.setOrientationNatural();
        }
    }
}
