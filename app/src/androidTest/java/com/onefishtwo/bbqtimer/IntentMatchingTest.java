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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.ParcelFileDescriptor;
import android.os.SystemClock;
import android.util.Log;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.onefishtwo.bbqtimer.state.ApplicationState;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Android tests to verify that all expected Intents still get received by MainActivity,
 * TimerAppWidgetProvider, AlarmReceiver, and ResumeReceiver, especially with
 * android:intentMatchingFlags="enforceIntentFilter" and "allowNullAction" enabled in the
 * AndroidManifest.xml for Android 16+.
 */
@RunWith(AndroidJUnit4.class)
public class IntentMatchingTest {
    public static final String TAG = "IntentMatchingTest";
    private Context context;
    private TimeCounter timer;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        ApplicationState state = ApplicationState.sharedInstance(context);

        timer = state.getTimeCounter();
        timer.stop();
        state.save(context);

        // Reset test hooks in SharedPreferences
        TimerAppWidgetProvider.saveActionForTesting(context, null);
    }

    @After
    public void tearDown() {
        timer = null;
        context = null;
    }

    private void waitForBroadcast() {
        SystemClock.sleep(500);
    }

    /** Returns the last Intent Action saved by an app Intent receiver for testing. */
    private String getLastAction() {
        return context.getSharedPreferences(
                        TimerAppWidgetProvider.PREFS_TESTING, Context.MODE_PRIVATE)
                .getString(TimerAppWidgetProvider.PREF_LAST_ACTION, null);
    }

    /**
     * Constructs an implicit Intent for testing BBQTimer.
     * <p>
     * This sets FLAG_DEBUG_LOG_RESOLUTION which:
     *   (1) logs the Intent resolution [filter LogCat for "IntentResolver" to see it], and
     *   (2) triggers TimerAppWidgetProvider.saveIntentActionForTesting() to save this Intent's
     *       action in SharedPreferences, causing StrictMode disk I/O policy violations, while
     *       ordinary production Intents won't.
     */
    private Intent makeImplicitIntent(@Nullable String action) {
        Intent intent = new Intent(action);
        intent.setPackage(context.getPackageName());
        intent.addFlags(Intent.FLAG_DEBUG_LOG_RESOLUTION);
        return intent;
    }

    private Intent makeMainActivityIntent(@Nullable String action) {
        // Use an implicit intent with package name to force Intent Filter matching.
        Intent intent = makeImplicitIntent(action);
        // intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /** Verifies that a filter exists for the given action in the receiver's manifest entry. */
    private void verifyReceiverFilterExists(String action, @SuppressWarnings("SameParameterValue") Class<?> receiverClass) {
        List<ResolveInfo> receivers = context.getPackageManager()
                .queryBroadcastReceivers(makeImplicitIntent(action), 0);
        boolean found = false;
        for (ResolveInfo info : receivers) {
            if (info.activityInfo.name.equals(receiverClass.getName())) {
                found = true;
                break;
            }
        }
        assertTrue("Manifest should have a filter for action: " + action + " in "
                + receiverClass.getSimpleName(), found);
    }

    @Test
    public void testMainActivityNullAction() {
        // With allowNullAction, an implicit intent with no action should match the filter.
        // See makeImplicitIntent() re: FLAG_DEBUG_LOG_RESOLUTION
        Intent intent = new Intent(context, MainActivity.class);
        intent.setAction(null);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_DEBUG_LOG_RESOLUTION);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(Assert::assertNotNull); // activity
        }
    }

    /** Sets Intent.FLAG_DEBUG_LOG_RESOLUTION (0x8) like makeImplicitIntent(). */
    @Test
    public void testMainActivityNullActionViaShell() {
        String pkg = context.getPackageName();
        String cmd = "am start -n " + pkg + "/.MainActivity -f 0x8";
        try {
            Log.d(TAG, "Running shell command: " + cmd);
            ParcelFileDescriptor pfd = InstrumentationRegistry.getInstrumentation().getUiAutomation()
                    .executeShellCommand(cmd);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new ParcelFileDescriptor.AutoCloseInputStream(pfd)))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    Log.d(TAG, "Shell: " + line);
                    if (line.contains("Error") || line.contains("Exception")) {
                        throw new RuntimeException("Shell command failed: " + line);
                    }
                }
            }
            pfd.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testMainActivityActionMain() {
        Intent intent = makeMainActivityIntent(Intent.ACTION_MAIN);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(Assert::assertNotNull); // activity
        }
    }

    @Test
    public void testMainActivityActionEdit() {
        Intent intent = makeMainActivityIntent(Intent.ACTION_EDIT);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(Assert::assertNotNull); // activity
        }
    }

    @Test
    public void testMainActivityActionRun() {
        Intent intent = makeMainActivityIntent(Intent.ACTION_RUN);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity ->
                    assertTrue("Timer should be running after MainActivity ACTION_RUN",
                            timer.isRunning()));
        }
    }

    @Test
    public void testMainActivityActionQuickClock() {
        Intent intent = makeMainActivityIntent(Intent.ACTION_QUICK_CLOCK);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                assertTrue("Timer should be paused after MainActivity ACTION_QUICK_CLOCK",
                        timer.isPaused());
                assertEquals(0, timer.getElapsedTime());
            });
        }
    }

    @Test
    public void testTimerAppWidgetProviderIntents() {
        // System/protected actions that we can't send via context.sendBroadcast() but should verify
        // their filters exist.
        String[] protectedActions = {
                "android.appwidget.action.APPWIDGET_DELETED",
                "android.appwidget.action.APPWIDGET_DISABLED",
                "android.appwidget.action.APPWIDGET_ENABLED",
                "android.appwidget.action.APPWIDGET_ENABLE_AND_UPDATE",
                "android.appwidget.action.APPWIDGET_RESTORED",
                "android.appwidget.action.APPWIDGET_UPDATE",
                "android.appwidget.action.APPWIDGET_UPDATE_OPTIONS",
        };

        // App-specific actions that we can test for implicit delivery.
        String[] appActions = {
                TimerAppWidgetProvider.ACTION_CYCLE,
                TimerAppWidgetProvider.ACTION_PAUSE,
                TimerAppWidgetProvider.ACTION_RESET,
                TimerAppWidgetProvider.ACTION_RUN,
                TimerAppWidgetProvider.ACTION_RUN_PAUSE,
                TimerAppWidgetProvider.ACTION_STOP
        };

        for (String action : protectedActions) {
            verifyReceiverFilterExists(action, TimerAppWidgetProvider.class);
        }

        for (String action : appActions) {
            verifyReceiverFilterExists(action, TimerAppWidgetProvider.class);

            TimerAppWidgetProvider.saveActionForTesting(context, null);
            context.sendBroadcast(makeImplicitIntent(action));
            waitForBroadcast();
            assertEquals("Failed for action: " + action, action, getLastAction());
        }
    }

    @Test
    public void testAlarmReceiverIntent() {
        String action = AlarmReceiver.ACTION_ALARM;
        context.sendBroadcast(makeImplicitIntent(action));
        waitForBroadcast();
        assertEquals(action, getLastAction());
    }

    @Test
    public void testResumeReceiverIntents() {
        // Verify filters exist for protected actions.
        verifyReceiverFilterExists(Intent.ACTION_BOOT_COMPLETED, ResumeReceiver.class);
        verifyReceiverFilterExists(Intent.ACTION_TIME_CHANGED, ResumeReceiver.class);
        verifyReceiverFilterExists(Intent.ACTION_TIMEZONE_CHANGED, ResumeReceiver.class);
        verifyReceiverFilterExists(Intent.ACTION_LOCALE_CHANGED, ResumeReceiver.class);
        verifyReceiverFilterExists(Intent.ACTION_MY_PACKAGE_REPLACED, ResumeReceiver.class);

        // Test receipt via implicit broadcast (using a non-protected action added for testing).
        String action = Intent.ACTION_RUN;
        context.sendBroadcast(makeImplicitIntent(action));
        waitForBroadcast();
        assertEquals(action, getLastAction());
    }
}
