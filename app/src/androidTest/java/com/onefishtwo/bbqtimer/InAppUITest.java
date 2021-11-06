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

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressImeActionButton;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasFocus;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isCompletelyDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isFocusable;
import static androidx.test.espresso.matcher.ViewMatchers.isFocused;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isNotEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.isNotFocused;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.onefishtwo.bbqtimer.CustomMatchers.withCompoundDrawable;
import static com.onefishtwo.bbqtimer.CustomViewActions.waitMsec;
import static com.onefishtwo.bbqtimer.TimeIntervalMatcher.inTimeInterval;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.junit.Assert.assertEquals;

/** Within-app Espresso UI tests. */
// TODO: Test the app's home screen widget.
// TODO: Add a multi-app test that checks the app's notifications.
@LargeTest
@RunWith(AndroidJUnit4.class)
public class InAppUITest {
    private ViewInteraction playPauseButton; // play/pause, formerly known as start/stop
    private ViewInteraction resetButton; // reset (pause @ 00:00); pause/replay icon or hidden
    private ViewInteraction stopButton; // stop @ 00:00
    private ViewInteraction timeView;
    private ViewInteraction enableRemindersToggle;
    private ViewInteraction minutesPicker;

    @NonNull
    @Rule
    public final ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Before
    public void setUp() {
        playPauseButton = onView(withId(R.id.startStopButton));
        resetButton = onView(withId(R.id.resetButton));
        stopButton = onView(withId(R.id.stopButton));
        timeView = onView(withId(R.id.display));
        enableRemindersToggle = onView(withId(R.id.enableReminders));
        minutesPicker = onView(withId(R.id.minutesPicker));
    }

    @After
    public void tearDown() {
        playPauseButton = null;
        resetButton = null;
        stopButton = null;
        timeView = null;
        enableRemindersToggle = null;
        minutesPicker = null;
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
        // Given the following setting in build.grade to clear the package data before tests:
        //    testInstrumentationRunnerArguments clearPackageData: 'true'
        // the initial state is known: Stopped @ 0:00 with 5 minute periodic alarms
        // so this test needn't reset state:
        //    ignoringFailures(onView(withId(R.id.stopButton))).perform(click())
        //    enableRemindersToggle.perform(setChecked(true))

        checkStopped(); // Stopped
        checkReminder(true);

        enableRemindersToggle.perform(click());
        checkReminder(false);

        enableRemindersToggle.perform(click());
        checkReminder(true);

        resetButton.perform(click()); // Reset
        checkPausedAt0();

        stopButton.perform(click()); // Stop
        checkStopped();

        playPauseButton.perform(click()); // Play
        checkPlaying();
        playPauseButton.perform(waitMsec(1000));
        TimeIntervalMatcher time1 = inTimeInterval(1000, 1700); // ARM emulators need latitude
        checkPlayingAt(time1);

        stopButton.perform(click()); // Stop
        checkStopped();
        playPauseButton.perform(waitMsec(100));
        checkStopped();

        resetButton.perform(click()); // Reset
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
        TimeIntervalMatcher time5 = inTimeInterval(time4.time, time4.time + 300);
        checkPausedAt(time5);

        resetButton.perform(click()); // Reset
        checkPausedAt0();

        playPauseButton.perform(click()); // Play
        checkPlaying();
        playPauseButton.perform(waitMsec(1000));
        checkPlayingAt(time1);

        playPauseButton.perform(click()); // Pause
        TimeIntervalMatcher time6 = inTimeInterval(time1.time, time1.time + 200);
        checkPausedAt(time6);

        stopButton.perform(click());
        checkStopped();

        playPauseButton.perform(click()); // Play
        playPauseButton.perform(waitMsec(1000));
        playPauseButton.perform(click()); // Pause
        checkPausedAt(time1);
    }

    /** Checks the enable-reminders checkbox and the minutes picker. */
    private void checkReminder(boolean expectEnabled) {
        enableRemindersToggle.check(matches(isCompletelyDisplayed()));
        minutesPicker.check(matches(isCompletelyDisplayed()));

        if (expectEnabled) {
            enableRemindersToggle.check(matches(isChecked()));
            minutesPicker.check(matches(isEnabled()));
            minutesPicker.check(matches(isFocusable()));
        } else {
            enableRemindersToggle.check(matches(isNotChecked()));
            minutesPicker.check(matches(isNotEnabled()));
            minutesPicker.check(matches(isNotFocused()));
        }
    }

    /** Checks that the UI is in the fully Stopped at 00:00 state. */
    private void checkStopped() {
        playPauseButton.check(matches(isCompletelyDisplayed()));
        resetButton.check(matches(isCompletelyDisplayed()));
        stopButton.check(matches(not(isDisplayed())));
        timeView.check(matches(withText("00:00.0")));

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
        timeView.check(matches(withText("00:00.0")));

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

    /** Tests the minutesPicker and enableRemindersToggle widgets. */
    @Test
    public void minutePickerUITest() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.onefishtwo.bbqtimer", appContext.getPackageName());

        Context appContext2 = ApplicationProvider.getApplicationContext();
        assertEquals("com.onefishtwo.bbqtimer", appContext2.getPackageName());

        checkReminder(true);
        minutesPicker.check(matches(hasFocus()));

        enableRemindersToggle.perform(click());
        checkReminder(false);

        enableRemindersToggle.perform(click());
        checkReminder(true);
        minutesPicker.check(matches(hasFocus()));

        minutesPicker.perform(click());
        minutesPicker.check(matches(hasFocus()));

        ViewInteraction minutePickerEditText = onView(
                allOf(withClassName(is("android.widget.NumberPicker$CustomEditText")),
                        withParent(withId(R.id.minutesPicker))));
        minutePickerEditText.check(matches(isFocused()));
        minutePickerEditText.check(matches(withText("5")));
        checkReminder(true);

        minutePickerEditText.perform(
                replaceText("0.5"),
                pressImeActionButton()); // closeSoftKeyboard()
        minutePickerEditText.check(matches(withText("0.5")));

        enableRemindersToggle.perform(click());
        checkReminder(false);

        enableRemindersToggle.perform(click());
        checkReminder(true);
        checkStopped();

        playPauseButton.perform(click()); // Play
        playPauseButton.perform(waitMsec(30_001)); // *** TODO: Test that it alarmed once **
        playPauseButton.perform(click()); // Pause
        TimeIntervalMatcher time1 = inTimeInterval(30_000, 31_000);
        checkPausedAt(time1);
    }

}
