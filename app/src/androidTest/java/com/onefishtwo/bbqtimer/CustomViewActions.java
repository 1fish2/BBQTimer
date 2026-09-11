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

import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;

import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Checkable;

import androidx.annotation.NonNull;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

final class CustomViewActions {

    private CustomViewActions() {}

    /**
     * Returns a {@code ViewAction} that waits for {@code msec} milliseconds. This should get a
     * fairly precise delay plus the usual wait-for-idle.
     *
     * @see UiController#loopMainThreadForAtLeast about looping the main thread
     * @see <a href="http://blog.sqisland.com/2015/06/espresso-elapsed-time.html">Espresso: Elapsed
     * time</a> on creating and using an {@code IdlingResource} that can wait a long time. Reports
     * say Espresso polls an IdlingResource about every 5 seconds, so the timing precision is low.
     */
    @NonNull
    public static ViewAction waitMsec(final long msec) {
        return new ViewAction() {
            @NonNull
            @Override
            public Matcher<View> getConstraints() {
                return isAssignableFrom(View.class);
            }

            @NonNull
            @Override
            public String getDescription() {
                return "Wait " + msec + " msec.";
            }

            @Override
            public void perform(@NonNull UiController uiController, @NonNull View view) {
                uiController.loopMainThreadForAtLeast(msec);
            }
        };
    }

    /** Clicks a checkbox if needed to put it into the desired state. */
    @SuppressWarnings({"SameParameterValue", "unused"})
    @NonNull
    public static ViewAction setChecked(final boolean checked) {
        return new ViewAction() {
            @NonNull
            @Override
            public Matcher<View> getConstraints() {
                return new BaseMatcher<>() {
                    @Override
                    public boolean matches(Object item) {
                        return item instanceof Checkable;
                    }

                    @Override
                    public void describeTo(Description description) {
                        description.appendText("is an instance of android.widget.Checkable");
                    }
                };
            }

            @NonNull
            @Override
            public String getDescription() {
                return "set checked to " + checked;
            }

            @Override
            public void perform(@NonNull UiController uiController, @NonNull View view) {
                Checkable checkableView = (Checkable) view;

                if (checkableView.isChecked() != checked) {
                    click().perform(uiController, view);
                }
            }
        };
    }

    /**
     * Returns a ViewAction that clicks at the center-left of a View.
     *
     * @return a ViewAction that performs the click.
     */
    @NonNull
    public static ViewAction clickAtCenterLeft() {
        return new GeneralClickAction(
                Tap.SINGLE,
                GeneralLocation.CENTER_LEFT,
                Press.PINPOINT,
                InputDevice.SOURCE_UNKNOWN,
                MotionEvent.BUTTON_PRIMARY);
    }
}
