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

import static com.onefishtwo.bbqtimer.TestUtils.pollForExpectation;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.app.UiAutomation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.onefishtwo.bbqtimer.state.ApplicationState;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Android tests to verify that all expected Intents still get received by MainActivity,
 * TimerAppWidgetProvider, AlarmReceiver, and ResumeReceiver, especially with
 * android:intentMatchingFlags="enforceIntentFilter" and "allowNullAction" enabled in the
 * AndroidManifest.xml for Android 16+, and additional restrictions to come.
 * <p>
 * TODO: Initiate APPWIDGET_* via UiAutomation on the Home screen?
 * <p>
 * TODO: Initiate ACTION_LOCALE_CHANGED via the Settings app?
 * <p>
 * No luck sending ACTION_BOOT_COMPLETED or ACTION_MY_PACKAGE_REPLACED.
 */
@RunWith(AndroidJUnit4.class)
public class IntentMatchingTest {
    public static final String TAG = "IntentMatchingTest";
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile("Error|Exception");

    private Context context;
    private String originalTimezone;
    private TimeCounter timer;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();
        originalTimezone = TimeZone.getDefault().getID();
        ApplicationState state = ApplicationState.sharedInstance(context);

        timer = state.getTimeCounter();
        timer.stop();
        state.save(context);

        // Disable automatic time zone so network updates don't stomp on a test
        executeShellCommand("settings put global auto_time_zone 0");

        // Reset the test data saved in SharedPreferences
        TimerAppWidgetProvider.saveActionForTesting(context, null);
    }

    @After
    public void tearDown() {
        if (!TimeZone.getDefault().getID().equals(originalTimezone)) {
            setTimezone(originalTimezone);
        }
        executeShellCommand("settings put global auto_time_zone 1");

        timer = null;
        context = null;
    }

    /** Set Android's timezone to trigger an ACTION_TIMEZONE_CHANGED Intent. */
    private static void setTimezone(String timezone) {
        if (Build.VERSION.SDK_INT >= 30) {
            executeShellCommand("cmd alarm set-timezone " + timezone);
        } else {
            executeShellCommand("setprop persist.sys.timezone " + timezone);
            executeShellCommand("settings put global time_zone " + timezone);
        }
    }

    private void waitForAction(String expectedAction) {
        assertTrue("Timed out waiting for action: " + expectedAction,
                pollForExpectation(() -> expectedAction.equals(getLastAction())));
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
     * This sets FLAG_DEBUG_LOG_RESOLUTION which:<br>
     *   (1) logs the Intent resolution [filter LogCat for "IntentResolver" to see it], and<br>
     *   (2) triggers most of the app's receivers to call
     *       {@link TimerAppWidgetProvider#saveIntentActionForTesting} to store the Intent's action
     *       in SharedPreferences for test assertions.
     * <p>
     * TODO: This needs a better way to check that the Intent was received. Changing the system
     *  time or timezone doesn't set the FLAG_DEBUG_LOG_RESOLUTION flag. We could drop the flag
     *  if saveIntentActionForTesting() used a background thread to write SharedPrefernces. Or
     *  have the test set a listener? Or send info via another Intent, or a java.util.concurrent
     *  object?
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
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return intent;
    }

    /** Verifies that a filter exists for the given action in the receiver's manifest entry. */
    private void verifyReceiverFilterExists(String action, Class<?> receiverClass) {
        List<ResolveInfo> receivers = context.getPackageManager()
                .queryBroadcastReceivers(makeImplicitIntent(action), 0);
        boolean found = receivers.stream()
                .anyMatch(info -> receiverClass.getName().equals(info.activityInfo.name));
        assertTrue("Manifest should have a filter for action: " + action + " in "
                + receiverClass.getSimpleName(), found);
    }

   /**
    * Executes a shell command, e.g. to send a protected Intent, and returns stdout combined with
    * stderr -- EXCEPT there's no access to stderr on API < 31.
    * <p>
    * For API 31 - 33, this pipes `command + " 2>&1"; exit` into `sh` to get stdout and stderr.
    * [This could also `echo $?` to get the exit code.]
    * [On API 34, this could use executeShellCommandRwe(cmd) to read stdout and stderr.]
    * <p>
    * ASSUMES: The output is shorter than 64KB to fit in standard pipe buffers. Longer output
    * would require forking a thread to read output before closing stdin.
    * <p>
    * The caller can add "-f 0x8" or "--debug-log-resolution" to "am broadcast" for
    * FLAG_DEBUG_LOG_RESOLUTION, like {@link #makeImplicitIntent}.
    * <p>
    * NOTE: `uiAutomation.executeShellCommand("sh -c 'am broadcast FOO 2>&1'")` DOES NOT WORK! Java
    * tokenizes it as ['am, broadcast, FOO, 2>&1'], can't find `'am`, and doesn't redirect. Other
    * variations also fail.
    */
   public static String executeShellCommand(String cmd) {
       UiAutomation uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
       String outputText;

       if (Build.VERSION.SDK_INT >= 31) {
           String extendedCmd = cmd + " 2>&1";

           Log.d(TAG, "Running shell command: " + extendedCmd);

           try {
               @SuppressWarnings("SpellCheckingInspection")
               ParcelFileDescriptor[] pfds = uiAutomation.executeShellCommandRw("sh");

               writeStdin(pfds[1], extendedCmd);

               outputText = readStdout(pfds[0]);
               Log.d(TAG, "Shell stdout+stderr: «" + outputText + "»");
           } catch (IOException e) {
               throw new RuntimeException("Failed shell command: " + extendedCmd, e);
           }
       } else {
           Log.d(TAG, "Running shell command: " + cmd);

           try (ParcelFileDescriptor pfd = uiAutomation.executeShellCommand(cmd)) {
               outputText = readStdout(pfd);
               Log.d(TAG, "Shell output: «" + outputText + "»");
           } catch (IOException e) {
               throw new RuntimeException("Failed shell command: " + cmd, e);
           }

           if (EXCEPTION_PATTERN.matcher(outputText).find()) {
               throw new RuntimeException("Shell command failed:\n" + outputText);
           }
       }

       return outputText;
   }

   /** Writes extendedCmd and an "exit" command to stdin. */
   private static void writeStdin(ParcelFileDescriptor stdinPfd, String extendedCmd) {
       try (PrintWriter stdinWriter = new PrintWriter(new OutputStreamWriter(
               new ParcelFileDescriptor.AutoCloseOutputStream(stdinPfd), StandardCharsets.UTF_8))) {
           stdinWriter.println(extendedCmd);
           stdinWriter.println("exit");
           stdinWriter.flush();
       }
   }

   @NonNull
   private static String readStdout(ParcelFileDescriptor stdoutPfd) throws IOException {
       StringBuilder sb = new StringBuilder();

       try (BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(
               new ParcelFileDescriptor.AutoCloseInputStream(stdoutPfd), StandardCharsets.UTF_8))) {
           String line;
           while ((line = stdoutReader.readLine()) != null) {
               sb.append(line).append("\n");
           }
       }

       return sb.toString();
   }

    @Test
    public void testMainActivityNullAction() {
        // With allowNullAction, an implicit intent with no action should match the filter.
        Intent intent = new Intent(context, MainActivity.class);

        intent.setAction(null);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_DEBUG_LOG_RESOLUTION);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> assertFalse(activity.isFinishing()));
        }
    }

    @Test
    public void testMainActivityNullActionViaShell() {
        String pkg = context.getPackageName();
        String cmd = "am start -n " + pkg + "/.MainActivity";

        executeShellCommand(cmd); // + a non-Intent way to check that the Activity launched?
    }

    @Test
    public void testMainActivityActionMain() {
        Intent intent = makeMainActivityIntent(Intent.ACTION_MAIN);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> assertFalse(activity.isFinishing()));
        }
    }

    @Test
    public void testMainActivityActionEdit() {
        Intent intent = makeMainActivityIntent(Intent.ACTION_EDIT);
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> assertFalse(activity.isFinishing()));
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
        // App-specific actions that are testable by sending implicit Intents.
        String[] appActions = {
                TimerAppWidgetProvider.ACTION_CYCLE,
                TimerAppWidgetProvider.ACTION_PAUSE,
                TimerAppWidgetProvider.ACTION_RESET,
                TimerAppWidgetProvider.ACTION_RUN,
                TimerAppWidgetProvider.ACTION_RUN_PAUSE,
                TimerAppWidgetProvider.ACTION_STOP
        };

        for (String action : appActions) {
            verifyReceiverFilterExists(action, TimerAppWidgetProvider.class);

            TimerAppWidgetProvider.saveActionForTesting(context, null);
            checkActionViaImplicitIntent(action);
        }
    }

    @Test
    public void testTimerAppWidgetProviderProtectedIntents() {
        // System/protected actions where broadcasting the Intent from app code gets a SecurityException.
        // Punt. Just verify that the Intent filters are in the manifest.
        String[] protectedActions = {
                "android.appwidget.action.APPWIDGET_DELETED",
                "android.appwidget.action.APPWIDGET_DISABLED",
                "android.appwidget.action.APPWIDGET_ENABLED",
                "android.appwidget.action.APPWIDGET_ENABLE_AND_UPDATE",
                "android.appwidget.action.APPWIDGET_RESTORED",
                "android.appwidget.action.APPWIDGET_UPDATE",
                "android.appwidget.action.APPWIDGET_UPDATE_OPTIONS",
        };

        for (String action : protectedActions) {
            verifyReceiverFilterExists(action, TimerAppWidgetProvider.class);
        }
    }

    private void checkActionViaImplicitIntent(String action) {
        context.sendBroadcast(makeImplicitIntent(action));
        waitForAction(action);
    }

    @Test
    public void testAlarmReceiverIntent() {
        checkActionViaImplicitIntent(AlarmReceiver.ACTION_ALARM);
    }

    @Test
    public void testResumeReceiverIntentFilters() {
        // At least verify that intent-filters exist for the protected actions.
        verifyReceiverFilterExists(Intent.ACTION_BOOT_COMPLETED, ResumeReceiver.class);
        verifyReceiverFilterExists(Intent.ACTION_TIME_CHANGED, ResumeReceiver.class);
        verifyReceiverFilterExists(Intent.ACTION_TIMEZONE_CHANGED, ResumeReceiver.class);
        verifyReceiverFilterExists(Intent.ACTION_LOCALE_CHANGED, ResumeReceiver.class);
        verifyReceiverFilterExists(Intent.ACTION_MY_PACKAGE_REPLACED, ResumeReceiver.class);

        // Test that ResumeReceiver receives an implicit broadcast of an unprotected action added to
        // its intent-filter just for testing.
        checkActionViaImplicitIntent(Intent.ACTION_RUN); // "android.intent.action.RUN", not TimerAppWidgetProvider.ACTION_RUN
    }

    @Test
    public void testResumeReceiverProtectedSetTimeIntent() {
        // Test a protected action via a specialized shell command, where broadcasting the Intent
        // gets a SecurityException.
        long backup = System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(1);

        TimerAppWidgetProvider.saveActionForTesting(context, null);
        executeShellCommand("cmd alarm set-time " + backup);
        waitForAction(Intent.ACTION_TIME_CHANGED);
    }

    @Test
    public void testResumeReceiverProtectedSetTimezoneIntent() {
        // Test a protected action via a specialized shell command, where broadcasting the Intent
        // gets a SecurityException.
        String tz = originalTimezone.equals("America/New_York") ? "Europe/London" : "America/New_York";

        TimerAppWidgetProvider.saveActionForTesting(context, null);
        setTimezone(tz);
        waitForAction(Intent.ACTION_TIMEZONE_CHANGED);
    }
}
