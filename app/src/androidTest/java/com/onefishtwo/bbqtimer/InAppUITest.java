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


import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import static android.Manifest.permission.POST_NOTIFICATIONS;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.doubleClick;
import static androidx.test.espresso.action.ViewActions.longClick;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
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
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.onefishtwo.bbqtimer.CustomMatchers.withCompoundDrawable;
import static com.onefishtwo.bbqtimer.CustomViewActions.waitMsec;
import static com.onefishtwo.bbqtimer.TimeIntervalMatcher.inTimeInterval;
import static com.onefishtwo.bbqtimer.TimeIntervalMatcher.inWholeTimeInterval;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;

/** Within-app Espresso UI tests. */
// TODO: Test the app's home screen widget.
// TODO: Add a multi-app UIAutomation test of the app's notifications.
@LargeTest
@RunWith(AndroidJUnit4.class)
public class InAppUITest {
    private static final String TAG = "InAppUITest";
    private static final TimeIntervalMatcher TIME_ZERO = inTimeInterval(0, 0); // supports locales

    private ViewInteraction playPauseButton; // play/pause, formerly known as start/stop
    private ViewInteraction resetButton; // reset (pause @ 00:00); pause/replay icon or hidden
    private ViewInteraction stopButton; // stop @ 00:00
    private ViewInteraction timeView;
    private ViewInteraction enableRemindersToggle;
    private ViewInteraction alarmPeriodTextField;
    private ViewInteraction countdownDisplay;
    private ViewInteraction background;

    @NonNull
    @Rule
    public final ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    // Since `testInstrumentationRunnerArguments clearPackageData: 'true'` reset the app's state,
    // grant POST_NOTIFICATIONS permission before the test starts so it won't wait for the user to
    // grant permissions.
    @Rule
    public final GrantPermissionRule permissionRule =
            Build.VERSION.SDK_INT >= 33 ? GrantPermissionRule.grant(POST_NOTIFICATIONS)
            : null;

    @Before
    public void setUp() {
        playPauseButton = onView(withId(R.id.pauseResumeButton));
        resetButton = onView(withId(R.id.resetButton));
        stopButton = onView(withId(R.id.stopButton));
        timeView = onView(withId(R.id.countUpDisplay));
        enableRemindersToggle = onView(withId(R.id.enableReminders));
        alarmPeriodTextField = onView(withId(R.id.alarmPeriod));
        countdownDisplay = onView(withId(R.id.countdownDisplay));
        background = onView(withId(R.id.main_container));
    }

    @After
    public void tearDown() {
        playPauseButton = null;
        resetButton = null;
        stopButton = null;
        timeView = null;
        enableRemindersToggle = null;
        alarmPeriodTextField = null;
        countdownDisplay = null;
        background = null;
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

    /**
     * Tests all the nodes and arcs in the app's play/pause/reset/stop FSM.
     * <p/>
     * NOTE: Testing the timer is inherently timing-dependent. A test run could fail by not
     * allowing the app enough time to respond. ARM emulators need extra response time. Mocking
     * the TimeCounter clock might fix this even with MainActivity's Handler.
     */
    @Test
    public void playPauseStopUITest() {
        final long minutes5 = 5 * 60_000L;

        checkStopped(); // Stopped
        checkReminder(true);
        alarmPeriodTextField.check(matches(withText("5")));
        countdownDisplay.check(matches(withText("05:00")));
        countdownDisplay.check(matches(withText(inWholeTimeInterval(minutes5, minutes5))));

        enableRemindersToggle.perform(click());
        checkReminder(false);

        enableRemindersToggle.perform(click());
        checkReminder(true);

        resetButton.perform(click()); // Pause/Reset
        checkPausedAt0();

        stopButton.perform(click()); // Stop
        checkStopped();

        playPauseButton.perform(click()); // Play
        checkPlaying();
        playPauseButton.perform(waitMsec(1000));
        TimeIntervalMatcher time1 = inTimeInterval(1000, 1800);
        checkPlayingAt(time1);

        stopButton.perform(click()); // Stop
        checkStopped();
        playPauseButton.perform(waitMsec(100));
        checkStopped();

        resetButton.perform(click()); // Pause/Reset
        checkPausedAt0();
        playPauseButton.perform(waitMsec(100));
        checkPausedAt0();

        playPauseButton.perform(click()); // Play
        checkPlaying();
        playPauseButton.perform(waitMsec(1000));
        checkPlayingAt(time1);

        playPauseButton.perform(click()); // Pause
        TimeIntervalMatcher time2 = inTimeInterval(time1.time, time1.time + 300);
        checkPausedAt(time2);

        playPauseButton.perform(waitMsec(100));
        TimeIntervalMatcher time3 = inTimeInterval(time2.time, time2.time); // same time
        checkPausedAt(time3);

        playPauseButton.perform(click()); // Play
        checkPlaying();
        playPauseButton.perform(waitMsec(1000));
        TimeIntervalMatcher time4 = inTimeInterval(time3.time + 1000, time3.time + 1700);
        checkPlayingAt(time4);

        playPauseButton.perform(click()); // Pause
        TimeIntervalMatcher time5 = inTimeInterval(time4.time, time4.time + 500);
        checkPausedAt(time5);

        long downFrom5lo = (minutes5 - time5.time - 100L) / 1000L * 1000L;
        long downFrom5hi = (minutes5 - time5.time       ) / 1000L * 1000L + 1000L;
        TimeIntervalMatcher countdown5 = inWholeTimeInterval(downFrom5lo, downFrom5hi);
        countdownDisplay.check(matches(withText(countdown5)));

        resetButton.perform(click()); // Reset
        checkPausedAt0();

        playPauseButton.perform(click()); // Play
        checkPlaying();
        playPauseButton.perform(waitMsec(1000));
        checkPlayingAt(time1);

        playPauseButton.perform(click()); // Pause
        TimeIntervalMatcher time6 = inTimeInterval(time1.time, time1.time + 200);
        checkPausedAt(time6);

        stopButton.perform(click()); // Stop
        checkStopped();

        countdownDisplay.perform(click()); // Play
        checkPlaying();
        countdownDisplay.perform(waitMsec(1000));
        countdownDisplay.perform(click()); // Pause
        checkPausedAt(time1);
    }

    /** Checks the enable-reminders checkbox and related widgets. */
    private void checkReminder(boolean expectEnabled) {
        enableRemindersToggle.check(matches(isCompletelyDisplayed()));

        if (expectEnabled) {
            enableRemindersToggle.check(matches(isChecked()));
            countdownDisplay.check(matches(isCompletelyDisplayed()));
            countdownDisplay.check(matches(isClickable()));
        } else {
            enableRemindersToggle.check(matches(isNotChecked()));
            countdownDisplay.check(matches(not(isDisplayed())));
        }

        alarmPeriodTextField.check(matches(isCompletelyDisplayed()));
        alarmPeriodTextField.check(matches(isEnabled()));
    }

    /** Checks that the UI is in the fully Stopped at 00:00 state. */
    private void checkStopped() {
        playPauseButton.check(matches(isCompletelyDisplayed()));
        resetButton.check(matches(isCompletelyDisplayed()));
        stopButton.check(matches(not(isDisplayed())));
        timeView.check(matches(withText(TIME_ZERO)));

        playPauseButton.check(matches(withCompoundDrawable(R.drawable.ic_play)));
        resetButton.check(matches(withCompoundDrawable(R.drawable.ic_pause)));

        checkReminder(true);

        // TODO: Check timeView's color state.
    }

    /** Checks that the UI is in the Paused @ 00:00 state, aka the Reset state. */
    private void checkPausedAt0() {
        playPauseButton.check(matches(isCompletelyDisplayed()));
        resetButton.check(matches(not(isDisplayed())));
        stopButton.check(matches(isCompletelyDisplayed()));
        timeView.check(matches(withText(TIME_ZERO)));

        playPauseButton.check(matches(withCompoundDrawable(R.drawable.ic_play)));
        stopButton.check(matches(withCompoundDrawable(R.drawable.ic_stop)));

        checkReminder(true);

        // TODO: Check timeView's color state, flashing between either of two color states.
    }

    /** Checks that the UI is in the Playing state, aka Run. */
    private void checkPlaying() {
        playPauseButton.check(matches(isCompletelyDisplayed()));
        resetButton.check(matches(not(isDisplayed())));
        stopButton.check(matches(isCompletelyDisplayed()));

        playPauseButton.check(matches(withCompoundDrawable(R.drawable.ic_pause)));
        stopButton.check(matches(withCompoundDrawable(R.drawable.ic_stop)));

        checkReminder(true);

        // TODO: Check timeView's color state.
    }

    /** Checks that the UI is in the Playing state at a matching time value. */
    private void checkPlayingAt(@NonNull TimeIntervalMatcher time) {
        checkPlaying();
        timeView.check(matches(withText(time)));
    }

    /** Checks that the UI is in the Paused state at a matching time value. */
    private void checkPausedAt(@NonNull TimeIntervalMatcher time) {
        playPauseButton.check(matches(isCompletelyDisplayed()));
        resetButton.check(matches(isCompletelyDisplayed()));
        stopButton.check(matches(isCompletelyDisplayed()));

        playPauseButton.check(matches(withCompoundDrawable(R.drawable.ic_play)));
        resetButton.check(matches(withCompoundDrawable(R.drawable.ic_replay)));
        stopButton.check(matches(withCompoundDrawable(R.drawable.ic_stop)));
        timeView.check(matches(withText(time)));

        checkReminder(true);

        // TODO: Check timeView's color state, flashing between either of two color states.
    }

    /** Tests the alarm time interval TextEdit field and enableRemindersToggle widgets. */
    @Test
    public void alarmTimeUITest() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.onefishtwo.bbqtimer", appContext.getPackageName());

        Context appContext2 = ApplicationProvider.getApplicationContext();
        assertEquals("com.onefishtwo.bbqtimer", appContext2.getPackageName());

        checkReminder(true);
        alarmPeriodTextField.check(matches(withText("5")));

        enableRemindersToggle.perform(click());
        checkReminder(false);

        enableRemindersToggle.perform(click());
        checkReminder(true);
        checkStopped();

        // FRAGILE: Changing the EditText to a Material EditText with a Clear (X) button made this
        // test fragile, where longClick() or doubleClick() alone fails to select the text.
        // doubleClick() is understandable since the first click shows the (X) and slides the text
        // leftwards. longClick() seems to work on phones but not on Nexus 7.
        // Workaround: Put in multiple digits before doing longClick().
        alarmPeriodTextField.check(matches(not(hasFocus())));
        alarmPeriodTextField.perform(click());
        alarmPeriodTextField.check(matches(hasFocus()));
        alarmPeriodTextField.perform(replaceText("111"));
        alarmPeriodTextField.perform(longClick());
        alarmPeriodTextField.check(matches(hasFocus()));
        alarmPeriodTextField.perform(typeTextIntoFocusedView("1:2:35\n"));
        alarmPeriodTextField.check(matches(withText("1:02:35"))); // expanded from "1:2:35"
        delayForDefocusTextFieldWorkaround();
        alarmPeriodTextField.check(matches(doesNotHaveFocus()));

        // FRAGILE: Adding the click() avoids on SDK 22 "SecurityException: Injecting to another
        // application requires INJECT_EVENTS permission" from innerInjectMotionEvent().
        // Adding the waitMsec() works around this code failing to select the text, which would
        // happen when running all the InAppUITest tests together.
        alarmPeriodTextField.perform(click(), waitMsec(100), doubleClick());
        alarmPeriodTextField.check(matches(hasFocus()));
        alarmPeriodTextField.perform(typeTextIntoFocusedView(":5"));

        // FRAGILE: background.perform(click()) might accept text input, or click on a different
        // widget which will discard text input, or do something else. So do this instead:
        alarmPeriodTextField.perform(pressImeActionButton(), closeSoftKeyboard());

        // Since background.perform(click()) hasn't yet worked in this test, do something with the
        // background View to avoid a "Field is assigned but never accessed" inspection warning.
        //
        // FRAGILE: Attempts to do background.perform(waitMsec(...)) before the click() or instead
        // of the playPauseButton.perform(waitMsec(...)) can make the "wait" occur BEFORE the click
        // and thus break the test! Why?
        background.check(matches(isDisplayed()));

        alarmPeriodTextField.check(matches(withText("0:05"))); // normalized
        alarmPeriodTextField.check(matches(doesNotHaveFocus()));
        playPauseButton.perform(waitMsec(100)); // work around Espresso test flakiness

        playPauseButton.perform(click(), waitMsec(6_000), click()); // Play for 6 secs then Pause
        // *** TODO: Test that it alarmed once **
        TimeIntervalMatcher time6 = inTimeInterval(6_000, 7_000);
        checkPausedAt(time6);

        // TODO: Test moving focus with TAB and arrow keys.

        stopButton.perform(click());
        checkStopped();
    }

    /** Tests the CLEAR_TEXT endIcon in the alarmPeriodLayout TextInputLayout. */
    @Test
    public void endIconTest() {
        ViewInteraction clearTextImageButton = onView(anyOf(
                withContentDescription("Clear text"),     // en
                withContentDescription("Text löschen"))); // de
        delayForDefocusTextFieldWorkaround();
        clearTextImageButton.check(matches(not(isDisplayed())));
        alarmPeriodTextField.check(matches(withText("5")));

        alarmPeriodTextField.perform(click());
        clearTextImageButton.check(matches(isDisplayed()));

        // Click the CLEAR_TEXT endIcon.
        clearTextImageButton.perform(click());
        alarmPeriodTextField.check(matches(withText("")));

        // Type into the empty text field.
        alarmPeriodTextField.perform(typeTextIntoFocusedView("1:3\n"));
        delayForDefocusTextFieldWorkaround();
        alarmPeriodTextField.check(matches(withText("1:03")));
        alarmPeriodTextField.check(matches(doesNotHaveFocus()));

        // Start typing into the text field, then click another widget to cancel the edit.
        enableRemindersToggle.check(matches(isChecked()));
        alarmPeriodTextField.perform(click(), typeTextIntoFocusedView("88"));
        enableRemindersToggle.perform(click());
        alarmPeriodTextField.check(matches(withText("1:03")));
        alarmPeriodTextField.check(matches(doesNotHaveFocus()));
        enableRemindersToggle.check(matches(isNotChecked()));

        // To aid manual testing, leave reminders enabled and a convenient alarm period set.
        // These steps ALSO discovered that on HVGA slider 320x480 running at least API 22-23, the
        // adjustPan feature (pans to keep the text field visible when the soft keyboard opens) only
        // worked once after the Activity opens or rotates. The fix for that was to fix the text
        // field auto-focussing by making a LinearLayout focusable instead of having onResume()
        // call defocusTextField(alarmPeriod).
        enableRemindersToggle.perform(click());
        enableRemindersToggle.check(matches(isChecked()));
        alarmPeriodTextField.perform(click());
        alarmPeriodTextField.perform(replaceText("111"));
        alarmPeriodTextField.perform(longClick());
        alarmPeriodTextField.perform(typeTextIntoFocusedView("2\n"));
        alarmPeriodTextField.check(matches(withText("2")));
    }

    /** Delay to accommodate the defocusTextField() workaround. */
    private void delayForDefocusTextFieldWorkaround() {
        if (Build.VERSION.SDK_INT <= 27) {
            alarmPeriodTextField.perform(waitMsec(60));
        }
    }

    /** Tests the popup-menu startIcon and the recipe editor dialog. */
    @Test
    public void popupMenuTest() {
        ViewInteraction popupMenuButton = onView(
                allOf(withContentDescription(R.string.intervals_menu), isDisplayed()));
        alarmPeriodTextField.check(matches(not(hasFocus())));
        alarmPeriodTextField.check(matches(withText("5")));

        // Open the popup menu. This makes the Activity Views inaccessible or...
        popupMenuButton.perform(click());

        ViewInteraction cmdEdit = checkMenuCommand(R.string.edit_this_list);
        ViewInteraction cmd_6 = checkMenuCommandPrefix("6 ");
        ViewInteraction cmd_7 = checkMenuCommandPrefix("7 ");
        ViewInteraction cmd__30 = checkMenuCommand(":30");
        ViewInteraction cmd_1 = checkMenuCommand("1");
        // NOTE: Commands scrolled off the bottom require scrolling to access.

        // Pick the first few intervals from the menu.
        cmd__30.perform(click());
        alarmPeriodTextField.check(matches(withText("0:30")));

        popupMenuButton.perform(click());
        cmd_1.perform(click());
        alarmPeriodTextField.check(matches(withText("1")));

        popupMenuButton.perform(click());
        cmd_6.perform(click());
        alarmPeriodTextField.check(matches(withText("6")));

        popupMenuButton.perform(click());
        cmd_7.perform(click());
        alarmPeriodTextField.check(matches(withText("7")));

        popupMenuButton.perform(click());
        alarmPeriodTextField.check(doesNotExist()); // not in the current view hierarchy
        Espresso.pressBack(); // dismiss the popup menu
        alarmPeriodTextField.check(matches(isDisplayed()));

        // Open the recipe editor dialog, edit the text, then Cancel.
        popupMenuButton.perform(click());
        if (Build.VERSION.SDK_INT == 23) {
            Log.w(TAG, "===== Workaround: Punting this test since clicking to open a dialog" +
                    " gets stuck, not returning to the test method on Android M API 23");
            Espresso.pressBack(); // dismiss the popup menu
            background.perform(waitMsec(1000)); // for visual verification
            return;
        }
        cmdEdit.perform(click());
        ViewInteraction dialogTitle = checkTextView(R.string.edit_list_title);
        dialogTitle.check(matches(withId(androidx.appcompat.R.id.alertTitle)));
        dialogTitle.check(matches(isDisplayed()));

        ViewInteraction saveButton = onView(
                allOf(withId(android.R.id.button1), withText(R.string.save_edits)));
        ViewInteraction cancelButton = onView(
                allOf(withId(android.R.id.button2), withText(R.string.cancel_edits)));
        ViewInteraction resetEditorButton = onView(
                allOf(withId(android.R.id.button3), withText(R.string.reset)));
        saveButton.check(matches(isDisplayed()));
        cancelButton.check(matches(isDisplayed()));
        resetEditorButton.check(matches(isDisplayed()));

        ViewInteraction editText = onView(withId(R.id.recipes_text_field));
        editText.check(matches(isDisplayed()));
        editText.check(matches(withText(containsString("\n:30\n"))));

        editText.perform(longClick(), replaceText("77777 ***TO CANCEL***\n"));
        cancelButton.perform(scrollTo(), click());

        // Open the recipe editor dialog, edit the text, then Save.
        popupMenuButton.perform(click());
        cmdEdit.perform(click());
        dialogTitle.check(matches(isDisplayed()));
        editText.check(matches(withText(containsString("\n:30\n"))));

        String replacement = "88888 ***TO SAVE***\n";
        editText.perform(longClick(), replaceText(replacement));
        editText.check(matches(not(withText(containsString("\n:30\n")))));
        saveButton.perform(scrollTo(), click());

        // Open the recipe editor dialog, check the saved text, edit it, then Reset.
        popupMenuButton.perform(click());
        cmd__30.check(doesNotExist());
        checkMenuCommand(replacement.trim());

        cmdEdit.perform(click());
        dialogTitle.check(matches(isDisplayed()));
        editText.check(matches(withText(replacement)));

        editText.perform(longClick(), waitMsec(500),
                replaceText("99999 ***TO RESET***\n"),
                waitMsec(500)); // delay for a visual check
        resetEditorButton.perform(scrollTo(), click());

        // Check that the menu's contents were reset.
        popupMenuButton.perform(click());
        cmd__30.check(matches(isDisplayed()));
        cmd_1.check(matches(isDisplayed()));
        cmd_6.check(matches(isDisplayed()));
        cmd_7.check(matches(isDisplayed()));

        cmd_1.perform(waitMsec(500), click()); // delay for a visual check
    }

    /** @noinspection SameParameterValue*/
    private ViewInteraction checkTextView(@StringRes int resId) {
        ViewInteraction view = onView(withText(resId));
        view.check(matches(isDisplayed()));
        return view;
    }

    /** @noinspection SameParameterValue*/
    private ViewInteraction checkMenuCommand(@StringRes int resId) {
        ViewInteraction view = onView(
                allOf(withId(android.R.id.title),
                        withText(resId)));
        view.check(matches(isDisplayed()));
        return view;
    }

    private ViewInteraction checkMenuCommand(String label) {
        ViewInteraction view = onView(
                allOf(withId(android.R.id.title),
                        withText(label)));
        view.check(matches(isDisplayed()));
        return view;
    }

    private ViewInteraction checkMenuCommandPrefix(String labelPrefix) {
        ViewInteraction view = onView(
                allOf(withId(android.R.id.title),
                        withText(startsWith(labelPrefix))));
        view.check(matches(isDisplayed()));
        return view;
    }
}
