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


import android.support.test.espresso.ViewInteraction;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.View;

import org.hamcrest.Matcher;
import org.hamcrest.core.IsInstanceOf;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.Espresso.pressBack;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.longClick;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.onefishtwo.bbqtimer.CustomMatchers.childAtPosition;
import static com.onefishtwo.bbqtimer.CustomMatchers.ignoringFailures;
import static com.onefishtwo.bbqtimer.CustomMatchers.withCompoundDrawable;
import static com.onefishtwo.bbqtimer.CustomViewActions.waitMsec;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;

/** Within-app Espresso UI tests. */
// TODO: Add a multi-app test that checks the app's notifications.
@LargeTest
@RunWith(AndroidJUnit4.class)
public class InAppUITest {
    /** The resetButton is hidden on older versions of Android. */
    private static final boolean HIDE_RESET_FEATURE = MainActivity.HIDE_RESET_FEATURE;

    private ViewInteraction playPauseButton; // play/pause, formerly known as start/stop
    private ViewInteraction resetButton; // reset (pause @ 00:00); pause/replay icon or hidden
    private ViewInteraction stopButton; // stop @ 00:00
    private ViewInteraction timeView;
    private Matcher<View> resetIsDisplayed; // isDisplayed() -- or not if always hidden

    @Rule
    public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(
            MainActivity.class);

    @Before
    public void setUp() throws Exception {
        playPauseButton = onView(withId(R.id.startStopButton));
        resetButton = onView(withId(R.id.resetButton));
        stopButton = onView(withId(R.id.stopButton));
        timeView = onView(withId(R.id.display));
        resetIsDisplayed = HIDE_RESET_FEATURE ? not(isDisplayed()) : isDisplayed();
    }

    @After
    public void tearDown() throws Exception {
        playPauseButton = null;
        resetButton = null;
        stopButton = null;
        timeView = null;
        resetIsDisplayed = null;
    }

// MainActivity's FSM:
//
// Stopped  @ 00:00     |>  ||      --> Playing, Reset
//
// Reset    @ 00:00     |>      []  --> Playing, Stopped [on Android 21+]
//
// Playing  hh:mm++     ||      []  --> Paused, Stopped
//
// Paused   > 00:00     |>  ®   []  --> Playing, Reset, Stopped

    /** Tests all the nodes and arcs in the app's play/pause/reset/stop FSM. */
    @Test
    public void playPauseStopUITest() {
        // Click the Stop button if clickable so the test can begin in a well-defined state.
        ignoringFailures(onView(withId(R.id.stopButton))).perform(click());

        checkStopped();

        if (!HIDE_RESET_FEATURE) {
            resetButton.perform(click());
            checkPausedAt0();

            stopButton.perform(click());
            checkStopped();
        }

        playPauseButton.perform(click());
        checkPlaying();
        playPauseButton.perform(waitMsec(1000));
        checkPlaying();
        // TODO: Check that timeView's text is within a given time range.

        stopButton.perform(click());
        checkStopped();
        playPauseButton.perform(waitMsec(100));
        checkStopped();

        if (!HIDE_RESET_FEATURE) {
            resetButton.perform(click());
            checkPausedAt0();
            playPauseButton.perform(waitMsec(100));
            checkPausedAt0();
        }

        playPauseButton.perform(click());
        checkPlaying();

        playPauseButton.perform(waitMsec(1000));
        checkPlaying();
        // TODO: Check that timeView's text is within a given time range.

        playPauseButton.perform(click());
        checkPausedNotAt0();
        // TODO: Check that timeView's text is within a given time range.
        playPauseButton.perform(waitMsec(100));
        checkPausedNotAt0();
        // TODO: Check that timeView's text didn't change.

        playPauseButton.perform(click());
        checkPlaying();
        playPauseButton.perform(waitMsec(1000));
        checkPlaying();
        // TODO: Check that timeView's text is within a given time range.

        playPauseButton.perform(click());
        checkPausedNotAt0();
        // TODO: Check that timeView's text is within a given time range.

        if (!HIDE_RESET_FEATURE) {
            resetButton.perform(click());
            checkPausedAt0();

            playPauseButton.perform(click());
            checkPlaying();
            playPauseButton.perform(waitMsec(1000));
            checkPlaying();

            playPauseButton.perform(click());
            checkPausedNotAt0();
            // TODO: Check that timeView's text is within a given time range.
        }

        stopButton.perform(click());
        checkStopped();
    }

    /** Checks that the UI is in the fully Stopped at 00:00 state. */
    private void checkStopped() {
        playPauseButton.check(matches(isDisplayed()));
        resetButton.check(matches(resetIsDisplayed));
        stopButton.check(matches(not(isDisplayed())));
        timeView.check(matches(withText("00:00.0")));

        playPauseButton.check(matches(withCompoundDrawable(R.drawable.ic_action_play)));
        resetButton.check(matches(withCompoundDrawable(R.drawable.ic_action_pause)));

        // TODO: Check timeView's color state.
    }

    /** Checks that the UI is in the Paused @ 00:00 state, aka the Reset state. */
    private void checkPausedAt0() {
        playPauseButton.check(matches(isDisplayed()));
        resetButton.check(matches(not(isDisplayed())));
        stopButton.check(matches(isDisplayed())); // TODO: check its icon
        timeView.check(matches(withText("00:00.0")));

        playPauseButton.check(matches(withCompoundDrawable(R.drawable.ic_action_play)));
        stopButton.check(matches(withCompoundDrawable(R.drawable.ic_action_stop)));

        // TODO: Check timeView's color state, flashing between either of two color states.
    }

    /** Checks that the UI is in the Playing state, aka Run. */
    private void checkPlaying() {
        playPauseButton.check(matches(isDisplayed()));
        resetButton.check(matches(not(isDisplayed())));
        stopButton.check(matches(isDisplayed()));

        playPauseButton.check(matches(withCompoundDrawable(R.drawable.ic_action_pause)));
        stopButton.check(matches(withCompoundDrawable(R.drawable.ic_action_stop)));

        // TODO: Check timeView's color state.
    }

    /** Checks that the UI is in the Paused state, not at 00:00. */
    private void checkPausedNotAt0() {
        playPauseButton.check(matches(isDisplayed()));
        resetButton.check(matches(resetIsDisplayed));
        stopButton.check(matches(isDisplayed()));

        playPauseButton.check(matches(withCompoundDrawable(R.drawable.ic_action_play)));
        resetButton.check(matches(withCompoundDrawable(R.drawable.ic_action_replay)));
        stopButton.check(matches(withCompoundDrawable(R.drawable.ic_action_stop)));

        // TODO: Check timeView's color state, flashing between either of two color states.
    }

    public void minutePickerUITest() {
        // TODO: Finish writing this test. This rough code is from an Espresso test recording.
        ViewInteraction numberPicker = onView(withId(R.id.minutesPicker));
        numberPicker.perform(longClick());

        pressBack();

        ViewInteraction minutePickerEditText = onView(
                allOf(withClassName(is("android.widget.NumberPicker$CustomEditText")),
                        withParent(withId(R.id.minutesPicker)),
                        isDisplayed()));
        minutePickerEditText.perform(replaceText("0.5"), pressImeActionButton()); // closeSoftKeyboard()

        ViewInteraction enableRemindersCheckBox = onView(withId(R.id.enableReminders));
        enableRemindersCheckBox.perform(click(), click());

        ViewInteraction button = onView(
                allOf(withId(R.id.resetButton),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(
                                                android.widget.LinearLayout.class),
                                        1),
                                1),
                        isDisplayed()));
        button.check(matches(isDisplayed()));

        ViewInteraction textView2 = onView(
                allOf(withId(R.id.display), withText("00:00.0"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.main_container),
                                        0),
                                0),
                        isDisplayed()));
        textView2.check(matches(withText("00:00.0")));

        ViewInteraction editText = onView(
                allOf(IsInstanceOf.<View>instanceOf(android.widget.EditText.class),
                        withText("1"),
                        childAtPosition(
                                childAtPosition(
                                        IsInstanceOf.<View>instanceOf(
                                                android.widget.LinearLayout.class),
                                        1),
                                1),
                        isDisplayed()));
        editText.check(matches(withText("1")));
    }

}
