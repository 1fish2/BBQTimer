/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Jerry Morrison
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
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.clearText;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.action.ViewActions.typeTextIntoFocusedView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.doesNotHaveFocus;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withContentDescription;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withTagValue;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.onefishtwo.bbqtimer.CustomViewActions.waitMsec;
import static com.onefishtwo.bbqtimer.TimeIntervalMatcher.inTimeInterval;
import static com.onefishtwo.bbqtimer.TimeIntervalMatcher.inWholeTimeInterval;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.object.HasToString.hasToString;
import static org.junit.Assert.assertEquals;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.DataInteraction;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.PerformException;
import androidx.test.espresso.ViewAction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;

import java.util.concurrent.atomic.AtomicInteger;

/** Within-app Espresso UI tests. */
@SuppressWarnings("ClassWithTooManyFields")
@LargeTest
@RunWith(AndroidJUnit4.class)
public class InAppUITest implements Notifier.NotificationListener {
    private static final String TAG = "InAppUITest";
    private static final TimeIntervalMatcher TIME_ZERO = inTimeInterval(0, 0); // supports locales

    // View Matcher constants to search for these Views on demand
    private static final Matcher<View> PLAY_PAUSE_BUTTON = withId(R.id.pauseResumeButton); // Play/Pause (formerly Start/Stop)
    private static final Matcher<View> RESET_BUTTON = withId(R.id.resetButton); // Reset/Pause
    private static final Matcher<View> STOP_BUTTON = withId(R.id.stopButton);
    private static final Matcher<View> TIME_VIEW = withId(R.id.countUpDisplay);
    private static final Matcher<View> ENABLE_REMINDERS_TOGGLE = withId(R.id.enableReminders);
    private static final Matcher<View> ALARM_PERIOD_TEXT_FIELD = withId(R.id.alarmPeriod);
    private static final Matcher<View> COUNTDOWN_DISPLAY = withId(R.id.countdownDisplay);
    private static final Matcher<View> BACKGROUND = withId(R.id.main_container);
    private static final Matcher<View> TEXT_INPUT_END_ICON = withId(com.google.android.material.R.id.text_input_end_icon);
    private static final Matcher<View> POPUP_MENU = allOf(withContentDescription(R.string.intervals_menu), isDisplayed());
    private static final Matcher<View> DIALOG_TITLE = allOf(withText(R.string.edit_list_title), isDisplayed());
    private static final Matcher<View> DIALOG_SAVE_BUTTON = allOf(withId(android.R.id.button1), withText(R.string.save_edits));
    private static final Matcher<View> DIALOG_CANCEL_BUTTON = allOf(withId(android.R.id.button2), withText(R.string.cancel_edits));
    private static final Matcher<View> DIALOG_RESET_BUTTON = allOf(withId(android.R.id.button3), withText(R.string.reset));
    private static final Matcher<View> RECIPES_TEXT = withId(R.id.recipes_text_field);

    private FragmentManager fm; // for logging the dialog fragment status
    private final AtomicInteger notificationCount = new AtomicInteger(0);

    /** Launch MainActivity before each test and close it after the test completes. */
    @NonNull
    @Rule
    public final ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    /**
     * Since `testInstrumentationRunnerArguments clearPackageData: 'true'` resets the app's state,
     * grant POST_NOTIFICATIONS permission before the test starts (on API levels that require
     * notifications permission) so it won't wait for a user to grant permissions.
     */
    @Rule
    public final GrantPermissionRule permissionRule =
            Build.VERSION.SDK_INT >= 33 ? GrantPermissionRule.grant(POST_NOTIFICATIONS)
            : null;

    @Rule
    public final TestRule disableAnimationsRule = new DisableAnimationsRule();

    @Override
    public void onNotificationPosted() {
        notificationCount.incrementAndGet();
    }

    @Before
    public void setUp() {
        activityScenarioRule.getScenario().onActivity(
                activity -> fm = activity.getSupportFragmentManager());

        notificationCount.set(0);
        Notifier.setNotificationListener(this);
    }

    private void logFragmentStatus(String step) {
        boolean open = fm.findFragmentByTag(RecipeEditorDialogFragment.TAG) != null;
        Log.d(TAG, "*** At " + step + ", the dialog fragment is " + (open ? "open" : "closed"));
    }

    @After
    public void tearDown() {
        Notifier.setNotificationListener(null);
    }

    @NonNull
    private static ViewAction recipeDismissClick() {
        return DismissClickViewAction.dismissClick(RecipeEditorDialogFragment.TAG);
    }

// MainActivity's FSM:
//
// Stopped  @ 00:00     |>  ||      --> Playing, Reset (= Paused @ 0:00)
//
// Reset    @ 00:00     |>      []  --> Playing, Stopped
//
// Playing  hh:mm++     ||      []  --> Paused, Stopped
//
// Paused   > 00:00     |>  ⟲  []  --> Playing, Reset, Stopped

    /** Checks initial stopped state and toggling the reminder checkbox. */
    @Test
    public void testInitialStoppedStateAndRemindersToggle() {
        final long minutes5 = 5 * 60_000L;

        checkStopped(); // Stopped
        checkReminder(true);
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("5")));
        onView(COUNTDOWN_DISPLAY).check(matches(withText("05:00")));
        onView(COUNTDOWN_DISPLAY).check(matches(withText(inWholeTimeInterval(minutes5, minutes5))));

        onView(ENABLE_REMINDERS_TOGGLE).perform(click());
        checkReminder(false);

        onView(ENABLE_REMINDERS_TOGGLE).perform(click());
        checkReminder(true);
    }

    /** Tests transitioning from Stopped -> Reset -> Stopped. */
    @Test
    public void testStoppedResetStopTransitions() {
        checkStopped();

        onView(RESET_BUTTON).perform(click()); // Pause/Reset
        checkPausedAt0();

        onView(STOP_BUTTON).perform(click()); // Stop
        checkStopped();
    }

    /** Tests Play, Pause, Resume, and Stop FSM transitions. */
    @Test
    public void testPlayPauseResumeStopTransitions() {
        final long minutes5 = 5 * 60_000L;

        checkStopped();

        onView(PLAY_PAUSE_BUTTON).perform(click()); // Play
        checkPlaying();
        onView(PLAY_PAUSE_BUTTON).perform(waitMsec(1000));
        TimeIntervalMatcher time1 = inTimeInterval(1000, 1900);
        checkPlayingAt(time1);

        onView(STOP_BUTTON).perform(click()); // Stop
        checkStopped();
        onView(PLAY_PAUSE_BUTTON).perform(waitMsec(100));
        checkStopped();

        onView(RESET_BUTTON).perform(click()); // Pause/Reset
        checkPausedAt0();
        onView(PLAY_PAUSE_BUTTON).perform(waitMsec(100));
        checkPausedAt0();

        onView(PLAY_PAUSE_BUTTON).perform(click()); // Play
        checkPlaying();
        onView(PLAY_PAUSE_BUTTON).perform(waitMsec(1000));
        checkPlayingAt(time1);

        onView(PLAY_PAUSE_BUTTON).perform(click()); // Pause
        TimeIntervalMatcher time2 = inTimeInterval(time1.time, time1.time + 300);
        checkPausedAt(time2);

        onView(PLAY_PAUSE_BUTTON).perform(waitMsec(100));
        TimeIntervalMatcher time3 = inTimeInterval(time2.time, time2.time); // same time
        checkPausedAt(time3);

        onView(PLAY_PAUSE_BUTTON).perform(click()); // Play
        checkPlaying();
        onView(PLAY_PAUSE_BUTTON).perform(waitMsec(1000));
        TimeIntervalMatcher time4 = inTimeInterval(time3.time + 1000, time3.time + 1900);
        checkPlayingAt(time4);

        onView(PLAY_PAUSE_BUTTON).perform(click()); // Pause
        TimeIntervalMatcher time5 = inTimeInterval(time4.time, time4.time + 500);
        checkPausedAt(time5);

        long downFrom5lo = (minutes5 - time5.time - 100L) / 1000L * 1000L;
        long downFrom5hi = (minutes5 - time5.time       ) / 1000L * 1000L + 1000L;
        TimeIntervalMatcher countdown5 = inWholeTimeInterval(downFrom5lo, downFrom5hi);
        onView(COUNTDOWN_DISPLAY).check(matches(withText(countdown5)));

        onView(RESET_BUTTON).perform(click()); // Reset
        checkPausedAt0();

        onView(PLAY_PAUSE_BUTTON).perform(click()); // Play
        checkPlaying();
        onView(PLAY_PAUSE_BUTTON).perform(waitMsec(1000));
        checkPlayingAt(time1);

        onView(PLAY_PAUSE_BUTTON).perform(click()); // Pause
        TimeIntervalMatcher time6 = inTimeInterval(time1.time, time1.time + 200);
        checkPausedAt(time6);

        onView(STOP_BUTTON).perform(click()); // Stop
        checkStopped();
    }

    /** Tests clicking the countdown display directly to Play and Pause. */
    @Test
    public void testPlayPauseViaCountdownDisplay() {
        checkStopped();

        onView(COUNTDOWN_DISPLAY).perform(click()); // Play
        checkPlaying();
        onView(COUNTDOWN_DISPLAY).perform(waitMsec(1000));
        TimeIntervalMatcher time1 = inTimeInterval(1000, 1900);
        onView(COUNTDOWN_DISPLAY).perform(click()); // Pause
        checkPausedAt(time1);
    }

    /** Checks the enable-reminders checkbox and related widgets. */
    private void checkReminder(boolean expectEnabled) {
        onView(ENABLE_REMINDERS_TOGGLE).check(matches(isCompletelyDisplayed()));

        if (expectEnabled) {
            onView(ENABLE_REMINDERS_TOGGLE).check(matches(isChecked()));
            onView(COUNTDOWN_DISPLAY).check(matches(isCompletelyDisplayed()));
            onView(COUNTDOWN_DISPLAY).check(matches(isClickable()));
        } else {
            onView(ENABLE_REMINDERS_TOGGLE).check(matches(isNotChecked()));
            onView(COUNTDOWN_DISPLAY).check(matches(not(isDisplayed())));
        }

        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(isCompletelyDisplayed()));
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(isEnabled()));
    }

    /** Checks that the UI is in the fully Stopped at 00:00 state. */
    private void checkStopped() {
        onView(PLAY_PAUSE_BUTTON).check(matches(isCompletelyDisplayed()));
        onView(RESET_BUTTON).check(matches(isCompletelyDisplayed()));
        onView(STOP_BUTTON).check(matches(not(isDisplayed())));
        onView(TIME_VIEW).check(matches(withText(TIME_ZERO)));

        onView(PLAY_PAUSE_BUTTON).check(matches(withTagValue(is(R.drawable.ic_play))));
        onView(RESET_BUTTON).check(matches(withTagValue(is(R.drawable.ic_pause))));

        checkReminder(true);

        // TODO: Check timeView's color state.
    }

    /** Checks that the UI is in the Paused @ 00:00 state, aka the Reset state. */
    private void checkPausedAt0() {
        onView(PLAY_PAUSE_BUTTON).check(matches(isCompletelyDisplayed()));
        onView(RESET_BUTTON).check(matches(not(isDisplayed())));
        onView(STOP_BUTTON).check(matches(isCompletelyDisplayed()));
        onView(TIME_VIEW).check(matches(withText(TIME_ZERO)));

        onView(PLAY_PAUSE_BUTTON).check(matches(withTagValue(is(R.drawable.ic_play))));
        onView(STOP_BUTTON).check(matches(withTagValue(is(R.drawable.ic_stop))));

        checkReminder(true);

        // TODO: Check timeView's color state, flashing between either of two color states.
    }

    /** Checks that the UI is in the Playing state, aka Run. */
    private void checkPlaying() {
        onView(PLAY_PAUSE_BUTTON).check(matches(isCompletelyDisplayed()));
        onView(RESET_BUTTON).check(matches(not(isDisplayed())));
        onView(STOP_BUTTON).check(matches(isCompletelyDisplayed()));

        onView(PLAY_PAUSE_BUTTON).check(matches(withTagValue(is(R.drawable.ic_pause))));
        onView(STOP_BUTTON).check(matches(withTagValue(is(R.drawable.ic_stop))));

        checkReminder(true);

        // TODO: Check timeView's color state.
    }

    /** Checks that the UI is in the Playing state at a matching time value. */
    private void checkPlayingAt(@NonNull Matcher<String> time) {
        checkPlaying();
        onView(TIME_VIEW).check(matches(withText(time)));
    }

    /** Checks that the UI is in the Paused state at a matching time value. */
    private void checkPausedAt(@NonNull Matcher<String> time) {
        onView(PLAY_PAUSE_BUTTON).check(matches(isCompletelyDisplayed()));
        onView(RESET_BUTTON).check(matches(isCompletelyDisplayed()));
        onView(STOP_BUTTON).check(matches(isCompletelyDisplayed()));

        onView(PLAY_PAUSE_BUTTON).check(matches(withTagValue(is(R.drawable.ic_play))));
        onView(RESET_BUTTON).check(matches(withTagValue(is(R.drawable.ic_replay))));
        onView(STOP_BUTTON).check(matches(withTagValue(is(R.drawable.ic_stop))));
        onView(TIME_VIEW).check(matches(withText(time)));

        checkReminder(true);

        // TODO: Check timeView's color state, flashing between either of two color states.
    }

    /** Tests editing and input normalization of the alarm time interval text field. */
    @Test
    public void testAlarmTimeInputFormatting() {
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.onefishtwo.bbqtimer", appContext.getPackageName());

        Context appContext2 = ApplicationProvider.getApplicationContext();
        assertEquals("com.onefishtwo.bbqtimer", appContext2.getPackageName());

        checkReminder(true);
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("5")));

        onView(ENABLE_REMINDERS_TOGGLE).perform(click());
        checkReminder(false);

        onView(ENABLE_REMINDERS_TOGGLE).perform(click());
        checkReminder(true);
        checkStopped();

        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(not(hasFocus())));
        onView(ALARM_PERIOD_TEXT_FIELD).perform(click());
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(hasFocus()));
        onView(ALARM_PERIOD_TEXT_FIELD).perform(replaceText("01:62:5")); // 2:02:05 when accepted

        onView(ALARM_PERIOD_TEXT_FIELD).perform(closeSoftKeyboard());
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("01:62:5")));

        onView(ALARM_PERIOD_TEXT_FIELD).perform(longClick());
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(hasFocus()));
        onView(ALARM_PERIOD_TEXT_FIELD).perform(pressImeActionButton(), closeSoftKeyboard());
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("2:02:05")));

        delayForDefocusTextFieldWorkaround();
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(doesNotHaveFocus()));

        try {
            onView(ALARM_PERIOD_TEXT_FIELD).perform(click());
        } catch (PerformException e) {
            Log.w(TAG, "*** alarmPeriodTextField click failed (soft keyboard?): "
                    + e.getMessage());
            onView(ALARM_PERIOD_TEXT_FIELD).perform(closeSoftKeyboard());
        }
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(hasFocus()));
        onView(ALARM_PERIOD_TEXT_FIELD).perform(clearText()); // WORKAROUND landscape mode soft keyboard
        onView(ALARM_PERIOD_TEXT_FIELD).perform(typeTextIntoFocusedView(":5"));

        onView(ALARM_PERIOD_TEXT_FIELD).perform(pressImeActionButton(), closeSoftKeyboard());

        onView(BACKGROUND).check(matches(isDisplayed()));

        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("0:05"))); // normalized
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(doesNotHaveFocus()));
    }

    /** Tests notification counts generated during timer playback. */
    @Test
    public void testAlarmTimeNotificationTrigger() {
        checkStopped();

        onView(ALARM_PERIOD_TEXT_FIELD).perform(clearText());
        onView(ALARM_PERIOD_TEXT_FIELD).perform(typeText(":5\n"));
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("0:05")));

        assertEquals(0, notificationCount.get());
        onView(PLAY_PAUSE_BUTTON).perform(click()); // Play...
        assertEquals(1, notificationCount.get());
        onView(PLAY_PAUSE_BUTTON).perform(waitMsec(6_000)); // ...for 6 secs...
        assertEquals(2, notificationCount.get()); // 5 second timed notification
        try {
            onView(PLAY_PAUSE_BUTTON).perform(CustomViewActions.clickAtCenterLeft()); // ...then Pause
        } catch (Exception e) {
            onView(COUNTDOWN_DISPLAY).perform(CustomViewActions.clickAtCenterLeft()); // ...then Pause
        }
        assertEquals(3, notificationCount.get());
        TimeIntervalMatcher time6 = inTimeInterval(6_000, 8_000);
        checkPausedAt(time6);

        onView(PLAY_PAUSE_BUTTON).perform(waitMsec(400));
        onView(STOP_BUTTON).perform(click());
        checkStopped();

        assertEquals(3, notificationCount.get());
    }

    /** Tests the CLEAR_TEXT endIcon in the alarmPeriodLayout TextInputLayout. */
    @Test
    public void endIconTest() {
        delayForDefocusTextFieldWorkaround();

        // Check that the endIcon isn't displayed and the alarmPeriod EditText didn't autofocus.
        onView(TEXT_INPUT_END_ICON).check(matches(not(isDisplayed())));
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(not(hasFocus())));
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("5")));

        onView(ALARM_PERIOD_TEXT_FIELD).perform(click());
        onView(TEXT_INPUT_END_ICON).check(matches(isDisplayed()));

        // WORKAROUND: Close the soft keyboard in case it's covering/subsuming the text field in
        // landscape mode and thus covering the clear-text endIcon "(x)". This weakens the test.
        onView(ALARM_PERIOD_TEXT_FIELD).perform(closeSoftKeyboard());

        // Click the CLEAR_TEXT endIcon.
        onView(TEXT_INPUT_END_ICON).perform(click());
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("")));

        // Type into the empty text field.
        onView(ALARM_PERIOD_TEXT_FIELD).perform(typeTextIntoFocusedView("1:3\n"));
        delayForDefocusTextFieldWorkaround();
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("1:03")));
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(doesNotHaveFocus()));

        // Start typing into the text field, then click another widget to cancel the edit.
        onView(ENABLE_REMINDERS_TOGGLE).check(matches(isChecked()));
        onView(ALARM_PERIOD_TEXT_FIELD).perform(click(), typeTextIntoFocusedView("88"));
        onView(ALARM_PERIOD_TEXT_FIELD).perform(closeSoftKeyboard()); // WORKAROUND
        onView(ENABLE_REMINDERS_TOGGLE).perform(click());
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("1:03")));
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(doesNotHaveFocus()));
        onView(ENABLE_REMINDERS_TOGGLE).check(matches(isNotChecked()));

        // To aid manual testing, leave reminders enabled and a convenient alarm period set.
        // These steps ALSO discovered that on HVGA slider 320x480 running at least API 22-23, the
        // adjustPan feature (pans to keep the text field visible when the soft keyboard opens) only
        // worked once after the Activity opens or rotates. The fix for that was to fix the text
        // field autofocussing by making a LinearLayout focusable instead of having onResume()
        // call defocusTextField(alarmPeriod).
        onView(ENABLE_REMINDERS_TOGGLE).perform(click());
        onView(ENABLE_REMINDERS_TOGGLE).check(matches(isChecked()));
        onView(ALARM_PERIOD_TEXT_FIELD).perform(click());
        onView(ALARM_PERIOD_TEXT_FIELD).perform(replaceText("111"));
        onView(ALARM_PERIOD_TEXT_FIELD).perform(closeSoftKeyboard()); // WORKAROUND
        onView(ALARM_PERIOD_TEXT_FIELD).perform(longClick());
        onView(ALARM_PERIOD_TEXT_FIELD).perform(closeSoftKeyboard()); // WORKAROUND
        // TODO: Is there a way to select-all when the soft keyboard is subsuming the text field,
        //  then do `typeTextIntoFocusedView("2\n")`?
        onView(ALARM_PERIOD_TEXT_FIELD).perform(replaceText("2"));
        onView(ALARM_PERIOD_TEXT_FIELD).perform(pressImeActionButton());
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("2")));
    }

    /** Delay to accommodate the MainActivity#defocusTextField() workaround. */
    private void delayForDefocusTextFieldWorkaround() {
        if (Build.VERSION.SDK_INT <= 27) {
            onView(ALARM_PERIOD_TEXT_FIELD).perform(waitMsec(60));
        }
    }

    /** Tests selecting preset time intervals from the popup menu. */
    @Test
    public void testSelectPresetIntervalsFromPopupMenu() {
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(not(hasFocus())));
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("5")));

        // Open the popup menu.
        onView(POPUP_MENU).perform(click());

        DataInteraction cmd_6 = checkMenuCommandPrefix("6 ");
        DataInteraction cmd_7 = checkMenuCommandPrefix("7 ");
        DataInteraction cmd__30 = checkMenuCommand(":30");
        DataInteraction cmd_1 = checkMenuCommand("1");

        // Pick the first few intervals from the menu.
        cmd__30.perform(click());
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("0:30")));

        onView(POPUP_MENU).perform(click());
        cmd_1.perform(click());
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("1")));

        onView(POPUP_MENU).perform(click());
        cmd_6.perform(click());
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("6")));

        onView(POPUP_MENU).perform(click());
        cmd_7.perform(click());
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(withText("7")));

        onView(POPUP_MENU).perform(click());
        onView(ALARM_PERIOD_TEXT_FIELD).check(doesNotExist()); // not in the current (menu) view hierarchy
        Espresso.pressBack(); // dismiss the popup menu
        onView(ALARM_PERIOD_TEXT_FIELD).check(matches(isDisplayed()));
    }

    /** Tests canceling edits in the recipe editor dialog. */
    @Test
    public void testRecipeEditorDialogCancel() {
        // Open the recipe editor dialog, edit the text, then Cancel.
        onView(POPUP_MENU).perform(click());
        DataInteraction cmdEdit = checkMenuCommand(R.string.edit_this_list);
        cmdEdit.perform(click());

        onView(DIALOG_TITLE).check(matches(withId(R.id.recipes_title)));

        onView(DIALOG_SAVE_BUTTON).check(matches(isDisplayed()));
        onView(DIALOG_CANCEL_BUTTON).check(matches(isDisplayed()));
        onView(DIALOG_RESET_BUTTON).check(matches(isDisplayed()));

        onView(RECIPES_TEXT).check(matches(isDisplayed()));
        onView(RECIPES_TEXT).check(matches(withText(containsString("\n:30\n"))));
        onView(RECIPES_TEXT).perform(longClick(), replaceText("77777 ***TO CANCEL***\n"));

        onView(DIALOG_CANCEL_BUTTON).perform(scrollTo(), recipeDismissClick());
        logFragmentStatus("dialog should be closing");

        onView(POPUP_MENU).perform(click());
        Espresso.pressBack(); // dismiss the popup menu
    }

    /** Tests saving new recipes and resetting the recipe list in the dialog. */
    @Test
    public void testRecipeEditorDialogSaveAndReset() {
        // Open the recipe editor dialog, edit the text, then Save.
        onView(POPUP_MENU).perform(longClick()); // shortcut to the recipe editor [cmdEdit]
        onView(DIALOG_TITLE).check(matches(isDisplayed()));

        onView(RECIPES_TEXT).check(matches(withText(containsString("\n:30\n"))));

        String replacement = "88888 ***TO SAVE***\n";
        onView(RECIPES_TEXT).perform(longClick(), replaceText(replacement));
        onView(RECIPES_TEXT).check(matches(not(withText(containsString("\n:30\n")))));
        onView(DIALOG_SAVE_BUTTON).perform(scrollTo(), recipeDismissClick());
        logFragmentStatus("dialog should be saving");

        // Open the recipe editor dialog, check the saved text, edit it, then Reset.
        onView(POPUP_MENU).perform(click());

        // Verify ":30" is not in the popup menu using onView() instead of onData()
        onView(withText(":30")).check(doesNotExist());

        checkMenuCommand(replacement.trim());

        DataInteraction cmdEdit = checkMenuCommand(R.string.edit_this_list);
        cmdEdit.perform(click());
        onView(DIALOG_TITLE).check(matches(isDisplayed()));
        onView(RECIPES_TEXT).check(matches(withText(replacement)));

        onView(RECIPES_TEXT).perform(longClick(), waitMsec(500),
                replaceText("99999 ***TO RESET***\n"),
                waitMsec(500)); // delay for a visual check
        onView(DIALOG_RESET_BUTTON).perform(scrollTo(), recipeDismissClick());
        logFragmentStatus("dialog should be resetting");

        // Check that the menu's contents were reset.
        onView(POPUP_MENU).perform(click());
        checkMenuCommand(":30").check(matches(isDisplayed()));
        checkMenuCommand("1").check(matches(isDisplayed()));
        checkMenuCommandPrefix("6 ").check(matches(isDisplayed()));
        checkMenuCommandPrefix("7 ").check(matches(isDisplayed()));

        checkMenuCommand("1").perform(waitMsec(500), click()); // delay for a visual check
    }

    /**
     * Finds a data item displaying the given string. .click() will scroll it into view if needed.
     * @noinspection SameParameterValue
     */
    private DataInteraction checkMenuCommand(@StringRes int resId) {
        // WORKAROUND: onData(withText(resId)) doesn't work, so get the string and use that.
        Context appContext = ApplicationProvider.getApplicationContext();
        String text = appContext.getString(resId);
        return checkMenuCommand(text);
    }

    /**
     * Finds a data item displaying the given string. .click() will scroll it into view if needed.
     */
    private DataInteraction checkMenuCommand(String label) {
        // Find the menu item, scrolling it into view if needed.
        DataInteraction di = onData(hasToString(equalTo(label)));
        di.check(matches(isDisplayed()));
        return di;
    }

    /**
     * Finds a data item that starts with the given string. .click() will scroll it into view if
     * needed.
     */
    private DataInteraction checkMenuCommandPrefix(String labelPrefix) {
        // Find the menu item, scrolling it into view if needed.
        DataInteraction di = onData(hasToString(startsWith(labelPrefix)));
        di.check(matches(isDisplayed()));
        return di;
    }
}
