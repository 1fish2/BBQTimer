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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.util.TypedValue;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Configurator;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * System-level interaction tests using UI Automator.
 * Verifies behavior beyond the app's internal GUI, such as Notifications and App Shortcuts.
 * Requires an unlocked Android emulator or device.
 * <p>
 * TODO: Also test with the popup menu open and with the recipe editor open. Test more features in
 *  portrait and landscape modes. Check some Notification contents. Test the home screen widget.
 */
@RunWith(AndroidJUnit4.class)
public class SystemInteractionTest {
    private UiDevice device;
    private Context context;
    private static final String PACKAGE_NAME = "com.onefishtwo.bbqtimer";
    private static final int TIMEOUT = 5000;

    @SuppressWarnings("NewClassNamingConvention")
    enum NightDayMode {
        NIGHT,
        DAY,
        UNDEFINED;

        static NightDayMode fromUiModeBits(int mode) {
            return switch (mode & Configuration.UI_MODE_NIGHT_MASK) {
                case Configuration.UI_MODE_NIGHT_YES -> NIGHT;
                case Configuration.UI_MODE_NIGHT_NO -> DAY;
                default -> UNDEFINED;
            };
        }

        static NightDayMode fromContext(Context context) {
            return fromUiModeBits(context.getResources().getConfiguration().uiMode);
        }
    }

    interface CheckExpectation {
        boolean check();
    }

    static boolean pollForExpectation(CheckExpectation checker) {
        long deadline = SystemClock.uptimeMillis() + TIMEOUT;

        while (SystemClock.uptimeMillis() < deadline) {
            if (checker.check()) {
                return true;
            }
            SystemClock.sleep(100);
        }

        return false;
    }

    /** Waits up to TIMEOUT for the Context's configuration to reflect expectedNightMode. */
    private boolean waitForNightMode(@NonNull Context ctx, @NonNull NightDayMode expectedNightMode) {
        return pollForExpectation(() -> NightDayMode.fromContext(ctx) == expectedNightMode);
    }

    /**
     * Rule that saves device settings (Day/Night mode, font scale, rotation) before each test
     * and restores them afterward.
     */
    @SuppressWarnings({"JUnitTestCaseWithNoTests", "NewClassNamingConvention"})
    private static class RestoreSystemSettings extends ExternalResource {
        private String originalNightMode = "no";
        private String originalFontScale = "1.0";
        private String originalAccelRotation = "1";
        private long automatorIdleTimeout;

        @Override
        protected void before() {
            UiDevice dev = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            Context ctx = ApplicationProvider.getApplicationContext();

            automatorIdleTimeout = Configurator.getInstance().getWaitForIdleTimeout();
            originalNightMode = readNightMode(dev, ctx);
            originalFontScale = readFontScale(dev);
            originalAccelRotation = readAccelRotation(dev);
        }

        @NonNull
        private String readNightMode(@NonNull UiDevice dev, @NonNull Context ctx) {
            try {
                String nightOutput = dev.executeShellCommand("cmd uimode night").toLowerCase(Locale.US);

                if (nightOutput.contains("yes")) {
                    return "yes";
                } else if (nightOutput.contains("auto")) {
                    return "auto";
                } else {
                    return "no";
                }
            } catch (Exception ignored) {
                return NightDayMode.fromContext(ctx) == NightDayMode.NIGHT ? "yes" : "no";
            }
        }

        @NonNull
        private String readFontScale(@NonNull UiDevice dev) {
            try {
                String scale = dev.executeShellCommand("settings get system font_scale").trim();

                return (scale.isEmpty() || scale.contains("null")) ? "1.0" : scale;
            } catch (Exception ignored) {
                return "1.0";
            }
        }

        @NonNull
        private String readAccelRotation(@NonNull UiDevice dev) {
            try {
                String accel = dev.executeShellCommand("settings get system accelerometer_rotation").trim();

                return (accel.isEmpty() || accel.contains("null")) ? "1" : accel;
            } catch (Exception ignored) {
                return "1";
            }
        }

        @Override
        protected void after() {
            UiDevice dev = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

            // Try to dismiss remaining popups/menus
            try {
                dev.pressBack();
                dev.pressHome();
            } catch (Exception ignored) {}

            // Restore Night Mode
            try {
                dev.executeShellCommand("cmd uimode night " + originalNightMode);
            } catch (Exception ignored) {}

            // Restore Font Scale
            try {
                dev.executeShellCommand("settings put system font_scale " + originalFontScale);
            } catch (Exception ignored) {}

            // Restore Orientation & Accelerometer Rotation via high-level UiDevice methods
            try {
                dev.setOrientationNatural();
                if ("0".equals(originalAccelRotation)) {
                    dev.freezeRotation();
                } else {
                    dev.unfreezeRotation();
                }
            } catch (Exception ignored) {}

            Configurator.getInstance().setWaitForIdleTimeout(automatorIdleTimeout);
        }
    }

    /**
     * Grant POST_NOTIFICATIONS permission before a test starts (on API levels that require
     * notifications permission) so it won't wait for a user to grant permissions.
     */
    @Rule
    public final GrantPermissionRule permissionRule =
            Build.VERSION.SDK_INT >= 33 ? GrantPermissionRule.grant(POST_NOTIFICATIONS)
            : null;

    @Rule
    public final TestRule settingsRule = new RestoreSystemSettings();

    @Before
    public void setUp() throws RemoteException {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        context = ApplicationProvider.getApplicationContext();

        if (!device.isScreenOn()) {
            device.wakeUp();
        }

        device.pressHome(); // these tests begin on the Home screen
    }

    /** Gets an app string resource. */
    @NonNull
    private String getString(int resId) {
        return context.getString(resId);
    }

    /** Launches the app with a launcher action (or null for none) and waits for it. */
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

    /** Returns the Activity that is in the foreground. */
    @Nullable
    private Activity getResumedActivity() {
        final Activity[] activityHolder = new Activity[1];

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumedActivities =
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            if (!resumedActivities.isEmpty()) {
                activityHolder[0] = resumedActivities.iterator().next();
            }
        });

        return activityHolder[0];
    }

    // --- Tests --------------------------------------------------------------

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
        int width = device.getDisplayWidth();
        int height = device.getDisplayHeight();

        // Find the app icon in the Pixel launcher's app drawer.
        // Note: If the icon is on the Home screen after the test infrastructure installed a test
        // app, that's a "Predicted app" temporary icon, and on Android 37.1 its long-press menu has
        // a non-standard toggle between "Actions" (like "App info") and app "Shortcuts".
        // Workaround: Use the app drawer, not the Home screen.
        device.swipe(width / 2, height - 300, width / 2, 100, 10);

        String appName = getString(R.string.app_name);
        UiObject2 appIcon = device.wait(Until.findObject(By.text(appName)), TIMEOUT);

        assertNotNull("BBQ Timer app icon should be in the App Drawer", appIcon);

        // Long press to open the menu
        appIcon.click(800);

        // Use a Pattern to match either the short or long menu item label
        String shortLabel = getString(R.string.start_at_0_short);
        String longLabel = getString(R.string.start_at_0_long);
        Pattern labelPattern = Pattern.compile(
                Pattern.quote(shortLabel) + "|" + Pattern.quote(longLabel));
        UiObject2 startShortcut = device.wait(Until.findObject(By.text(labelPattern)), TIMEOUT);
        assertNotNull("\"Start\" shortcut should be visible (short or long label)", startShortcut);

        startShortcut.click();
        Log.i("DEBUG", "1");

        // NOTE: The app's RUNNING timer updates the UI every 100ms when running, which prevents
        // UIAutomator's waitForIdle() from detecting an idle state, so it'd time out, log, and
        // return after 10s by default. Set a short idle timeout for quick queries.
        Configurator.getInstance().setWaitForIdleTimeout(100);

        assertTrue("App should be in the foreground after the shortcut click",
                device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT));
        Log.i("DEBUG", "2");

        // The "Stop" button should be visible.
        String stopStr = getString(R.string.stop);
        UiObject2 stopButton = device.wait(Until.findObject(By.desc(stopStr)), TIMEOUT);
        Log.i("DEBUG", "3");
        assertNotNull("Timer should be running (\"Stop\" button visible)", stopButton);
        Log.i("DEBUG", "4");
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testThemeToggle() throws IOException {
        device.executeShellCommand("cmd uimode night yes");

        launchApp();
        UiObject2 container = device.wait(
                Until.findObject(By.res(PACKAGE_NAME, "main_container")), TIMEOUT);
        assertNotNull("App should be visible", container);

        // Wait for system configuration to reflect Night Mode
        assertTrue("System configuration should reflect Night Mode within timeout",
                waitForNightMode(context, NightDayMode.NIGHT));

        // Verify the running Activity reflects Night Mode and has a dark background
        Activity activity = getResumedActivity();
        assertNotNull("MainActivity should be resumed", activity);
        NightDayMode nightDayMode = NightDayMode.fromContext(activity);
        assertEquals("Activity resources should reflect Night Mode", NightDayMode.NIGHT, nightDayMode);

        TypedValue typedValue = new TypedValue();
        assertTrue("Theme should resolve colorBackground",
                activity.getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true));
        int darkBgColor = typedValue.data;
        if (Build.VERSION.SDK_INT >= 26) {
            assertTrue("Night mode background color should have low luminance",
                    Color.luminance(darkBgColor) < 0.5f);
        }

        // Switch to Light Mode
        device.executeShellCommand("cmd uimode night no");

        // Wait for system configuration to reflect Light Mode
        assertTrue("System configuration should reflect Day Mode within timeout",
                waitForNightMode(context, NightDayMode.DAY));

        // Poll for the running Activity to update its resources to Day Mode
        Activity resumed = getResumedActivity();
        assertTrue("Activity resources should reflect Day Mode within timeout",
                waitForNightMode(resumed, NightDayMode.DAY));

        // Verify the theme background color is light
        assertTrue("Theme should resolve colorBackground",
                resumed.getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true));
        int lightBgColor = typedValue.data;
        if (Build.VERSION.SDK_INT >= 26) {
            assertTrue("Light mode background color should have high luminance",
                    Color.luminance(lightBgColor) >= 0.5f);
        }
    }

    @Test
    public void testFontScale() throws IOException {
        launchApp();

        device.executeShellCommand("settings put system font_scale 1.5");

        // Wait for the configuration change to propagate to the test process.
        boolean applied = pollForExpectation(() ->
                Math.abs(context.getResources().getConfiguration().fontScale - 1.5f) < 0.01f);
        assertTrue("Font scale 1.5 should be applied to system configuration", applied);

        device.waitForIdle();

        UiObject2 container = device.wait(
                Until.findObject(By.res(PACKAGE_NAME, "main_container")), TIMEOUT);
        assertNotNull("App should be visible", container);

        float currentScale = context.getResources().getConfiguration().fontScale;
        assertEquals("App should have 1.5x font scale", 1.5f, currentScale, 0.01f);
    }

    @Test
    public void testRotation() throws RemoteException {
        device.freezeRotation(); // for a more controlled test

        launchApp();

        UiObject2 container = device.wait(
                Until.findObject(By.res(PACKAGE_NAME, "main_container")), TIMEOUT);
        assertNotNull("App container should be visible", container);

        // Explicitly set to Landscape mode (works on both phones and tablets)
        device.setOrientationLandscape();

        // Wait for the app's Activity configuration to reflect landscape orientation
        assertTrue("App Activity should reflect landscape orientation",
                pollForExpectation(() -> {
                    Activity activity = getResumedActivity();
                    return activity != null && activity.getResources().getConfiguration().orientation
                            == Configuration.ORIENTATION_LANDSCAPE;
                }));

        // Verify the app's container layout bounds are wider than tall in landscape
        UiObject2 landscapeContainer = device.wait(
                Until.findObject(By.res(PACKAGE_NAME, "main_container")), TIMEOUT);
        assertNotNull("App container should be visible in landscape", landscapeContainer);
        Rect landscapeBounds = landscapeContainer.getVisibleBounds();
        assertTrue("App layout bounds should be wider than tall in landscape ("
                + landscapeBounds.width() + " x " + landscapeBounds.height() + ")",
                landscapeBounds.width() > landscapeBounds.height());

        // TODO: Test that rotation closes the soft keyboard and the popup interval menu, while
        //  it doesn't close the recipe editor dialog.

        // Explicitly set to Portrait mode (works on both phones and tablets)
        device.setOrientationPortrait();

        // Wait for the app's Activity configuration to reflect portrait orientation
        assertTrue("App Activity should reflect portrait orientation",
                pollForExpectation(() -> {
                    Activity activity = getResumedActivity();
                    return activity != null && activity.getResources().getConfiguration().orientation
                            == Configuration.ORIENTATION_PORTRAIT;
                }));

        UiObject2 portraitContainer = device.wait(
                Until.findObject(By.res(PACKAGE_NAME, "main_container")), TIMEOUT);
        assertNotNull("App container should be visible in portrait", portraitContainer);
        Rect portraitBounds = portraitContainer.getVisibleBounds();
        assertTrue("App layout bounds should be taller than wide in portrait ("
                + portraitBounds.width() + " x " + portraitBounds.height() + ")",
                portraitBounds.height() > portraitBounds.width());
    }
}
