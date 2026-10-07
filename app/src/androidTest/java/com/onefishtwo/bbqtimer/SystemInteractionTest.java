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
import static com.onefishtwo.bbqtimer.TestUtils.assertPollForActivity;
import static com.onefishtwo.bbqtimer.TestUtils.assertPollForExpectation;
import static com.onefishtwo.bbqtimer.TestUtils.getResumedActivity;
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
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.Configurator;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.onefishtwo.bbqtimer.state.ApplicationState;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.io.IOException;
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
    private static final String TAG = "SystemInteractionTest";
    private UiDevice device;
    private Context context;
    private static final String PACKAGE_NAME = "com.onefishtwo.bbqtimer";
    private static final int TIMEOUT = 5000;

    @SuppressWarnings({"NewClassNamingConvention", "JUnitTestCaseWithNonTrivialConstructors"})
    enum NightDayMode {
        NIGHT(AppCompatDelegate.MODE_NIGHT_YES),
        DAY(AppCompatDelegate.MODE_NIGHT_NO),
        UNDEFINED(AppCompatDelegate.MODE_NIGHT_UNSPECIFIED);

        final int compatMode;

        NightDayMode(int mode) {
            this.compatMode = mode;
        }

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

    private static void setAppNightMode(int mode) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                AppCompatDelegate.setDefaultNightMode(mode));
    }

    private static void setAppNightMode(NightDayMode mode) {
        setAppNightMode(mode.compatMode);
    }

    /**
     * Rule that saves & restores device settings: font scale, rotation.
     */
    @SuppressWarnings({"JUnitTestCaseWithNoTests", "NewClassNamingConvention"})
    private static class RestoreSystemSettings extends ExternalResource {
        private String originalFontScale = "1.0";
        private String originalAccelRotation = "1";
        private long originalAutomatorIdleTimeout = TIMEOUT;

        @Override
        protected void before() {
            UiDevice dev = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

            originalAutomatorIdleTimeout = Configurator.getInstance().getWaitForIdleTimeout();
            originalFontScale = readFontScale(dev);
            originalAccelRotation = readAccelRotation(dev);

            // MainActivity.UpdateHandler updates the UI Views every 100ms while the timer is PAUSED
            // or RUNNING. That sends accessibility events, which keep UIAutomator's waitForIdle()
            // from detecting an idle state, so it (QueryController) times out after 10s and logs
            // "Could not detect idle state." (UIAutomator waits for accessibility events to settle,
            // vs. Espresso which waits for UIThread's message queue to empty.)
            // Set a short idle timeout for quick queries.
            Configurator.getInstance().setWaitForIdleTimeout(100);
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

            Configurator.getInstance().setWaitForIdleTimeout(originalAutomatorIdleTimeout);
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

        assertNotNull("Launch Intent for " + PACKAGE_NAME + " should not be null", intent);

        // FLAG_ACTIVITY_CLEAR_TOP ensures a clean launch Intent if an Activity instance is already
        // running from a previous test.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (action != null) {
            intent.setAction(action);
        }

        context.startActivity(intent);
        assertTrue("App should be visible after launch",
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT));
    }

    private void launchApp() {
        launchApp(null);
    }

    // --- Tests --------------------------------------------------------------

    @Test
    public void testNotificationDrawer() {
        // Open the app with the "Paused at 00:00" shortcut to show a paused notification.
        // This launch Intent should work even on API < 25 which lacks Home screen app shortcuts.
        launchApp(Intent.ACTION_QUICK_CLOCK);

        // Open the notification shade
        device.openNotification();
        try {
            Activity resumed = getResumedActivity();
            assertNotNull("The Activity should be resumed", resumed);

            ApplicationState state = ApplicationState.sharedInstance(context);
            TimeCounter timer = state.getTimeCounter();
            assertTrue(timer.isPaused());
            assertEquals(0, timer.getElapsedTime());

            boolean updated = device.wait(Until.hasObject(By.res(PACKAGE_NAME, "btnStart")), TIMEOUT);
            assertTrue("Notification should be visible with a Run button", updated);
            UiObject2 runButton = device.findObject(By.res(PACKAGE_NAME, "btnStart"));
            assertNotNull("Found the Run button", runButton);

            UiObject2 stopButton = device.findObject(By.res(PACKAGE_NAME, "btnStop"));
            assertNotNull("Found the Stop button", stopButton);

            // Click the Run button.
            runButton.click();
            assertPollForExpectation("Timer should be running after clicking Run", timer::isRunning);
            assertPollForExpectation("Time should elapse after starting", () -> timer.getElapsedTime() > 0);
            Log.d(TAG, "Running at " + timer.getElapsedTime());

            UiObject2 pauseButton = device.wait(Until.findObject(By.res(PACKAGE_NAME, "btnPause")), TIMEOUT);
            assertNotNull("Pause button should be visible in the Notification", pauseButton);

            // Click the Pause button.
            pauseButton.click();
            assertPollForExpectation("Timer should be paused after clicking Pause", timer::isPaused);
            Log.d(TAG, "Paused at " + timer.getElapsedTime());

            SystemClock.sleep(10); // Short delay so the button click is visible during visual test runs
        } finally {
            device.pressBack(); // Close drawer
        }
    }

    /**
     * Finds and returns the UI element for an app shortcut, expanding Pixel Launcher's "Shortcuts"
     * submenu if required (API 37.1+ launcher and 37.2+ app drawer workaround).
     */
    @NonNull
    private UiObject2 findAppShortcut(@NonNull String shortLabel, @NonNull String longLabel) {
        Pattern shortcutPattern = Pattern.compile(Pattern.quote(shortLabel) + "|" + Pattern.quote(longLabel));
        Pattern shortcutOrSubmenuPattern = Pattern.compile(shortcutPattern.pattern() + "|Shortcuts");

        UiObject2 element = device.wait(Until.findObject(By.text(shortcutOrSubmenuPattern)), TIMEOUT);
        assertNotNull("The \"" + shortLabel + "\" shortcut"
                + " or Shortcuts submenu should be visible", element);

        if ("Shortcuts".equals(element.getText())) {
            element.click();
            UiObject2 shortcutElement = device.wait(Until.findObject(By.text(shortcutPattern)), TIMEOUT);
            assertNotNull("The \"" + shortLabel + "\" shortcut"
                    + " should be visible in the Shortcuts submenu", shortcutElement);
            return shortcutElement;
        }

        return element;
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
        device.swipe(width / 2, height * 3 / 4, width / 2, height / 8, 10);

        String appName = getString(R.string.app_name);
        UiObject2 appIcon = device.wait(Until.findObject(By.text(appName)), TIMEOUT);

        assertNotNull("BBQ Timer app icon should be in the App Drawer", appIcon);

        // Long press to open the menu
        appIcon.longClick();

        UiObject2 startShortcut = findAppShortcut(
                getString(R.string.start_at_0_short),
                getString(R.string.start_at_0_long));
        startShortcut.click();

        assertTrue("App should be in the foreground after the shortcut click",
                device.wait(Until.hasObject(By.pkg(PACKAGE_NAME)), TIMEOUT));

        // The "Stop" button should be visible.
        String stopStr = getString(R.string.stop);
        UiObject2 stopButton = device.wait(Until.findObject(By.desc(stopStr)), TIMEOUT);
        assertNotNull("Timer should be running (\"Stop\" button visible)", stopButton);
    }

    /**
     * Test Night/Day theme at the app process level.
     * (shell `cmd uimode night no` works on API 30+ but this doesn't need to test at the OS level,
     * and testing at the app level is faster.)
     */
    @Test
    @SdkSuppress(minSdkVersion = 29)
    public void testThemeToggleAppLevel() {
        launchApp();

        // Switch the app to Night Mode
        setAppNightMode(NightDayMode.NIGHT);

        assertPollForActivity("Activity resources should reflect Night Mode",
                (act) -> NightDayMode.fromContext(act) == NightDayMode.NIGHT);

        Activity activity = getResumedActivity();
        assertNotNull("MainActivity should be resumed", activity);

        TypedValue typedValue = new TypedValue();
        assertTrue("Theme should resolve colorBackground",
                activity.getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true));
        int darkBgColor = typedValue.data;
        assertTrue("Night mode background color should have low luminance",
                Color.luminance(darkBgColor) < 0.5f);

        // Switch to Light Mode
        setAppNightMode(NightDayMode.DAY);

        // Poll for a recreated Activity with updated resources to Day Mode
        assertPollForActivity("Activity resources should reflect Day Mode",
                (act) -> NightDayMode.fromContext(act) == NightDayMode.DAY);

        // Verify the theme background color is light
        Activity resumed = getResumedActivity();
        assertNotNull("MainActivity should be resumed", resumed);

        assertTrue("Theme should resolve colorBackground",
                resumed.getTheme().resolveAttribute(android.R.attr.colorBackground, typedValue, true));
        int lightBgColor = typedValue.data;
        assertTrue("Light mode background color should have high luminance",
                Color.luminance(lightBgColor) >= 0.5f);
    }

    @Test
    public void testFontScale() throws IOException {
        launchApp();

        device.executeShellCommand("settings put system font_scale 1.5");

        assertPollForActivity("Activity should reflect font scale 1.5", (act) ->
                Math.abs(act.getResources().getConfiguration().fontScale - 1.5f) < 0.01f);

        UiObject2 container = device.wait(
                Until.findObject(By.res(PACKAGE_NAME, "main_container")), TIMEOUT);
        assertNotNull("Activity should be visible", container);
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
        assertPollForActivity("App Activity should reflect landscape orientation",
                (act) -> act.getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_LANDSCAPE);

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
        assertPollForActivity("App Activity should reflect portrait orientation",
                (act) -> act.getResources().getConfiguration().orientation
                        == Configuration.ORIENTATION_PORTRAIT);

        UiObject2 portraitContainer = device.wait(
                Until.findObject(By.res(PACKAGE_NAME, "main_container")), TIMEOUT);
        assertNotNull("App container should be visible in portrait", portraitContainer);
        Rect portraitBounds = portraitContainer.getVisibleBounds();

        // Verify bounds relative to landscape rather than absolute ratio, since the portrait mode
        // container size on near-square screens (like 720x748 cover screens) can be wider than tall
        // after system bar insets.
        assertTrue("Portrait width (" + portraitBounds.width() + ") "
                        + "should be narrower than landscape width (" + landscapeBounds.width() + ")",
                landscapeBounds.width() > portraitBounds.width());
        assertTrue("Portrait height (" + portraitBounds.height() + ") "
                        + "should be taller than landscape height (" + landscapeBounds.height() + ")",
                portraitBounds.height() > landscapeBounds.height());
    }
}
