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

import android.view.View;
import android.widget.Checkable;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

import androidx.annotation.NonNull;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import static androidx.test.espresso.action.ViewActions.click;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.isA;

class CustomViewActions {

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
    static ViewAction waitMsec(final long msec) {
        return new ViewAction() {
            @NonNull
            @Override
            public Matcher<View> getConstraints() {
                return any(View.class);
            }

            @NonNull
            @Override
            public String getDescription() {
                return "Wait " + msec + " msec.";
            }

            @Override
            public void perform(@NonNull UiController uiController, View view) {
                uiController.loopMainThreadForAtLeast(msec);
            }
        };
    }

    /** Clicks a checkbox if needed to put it into the desired state. */
    @SuppressWarnings("SameParameterValue")
    @NonNull
    static ViewAction setChecked(final boolean checked) {
        return new ViewAction() {
            @NonNull
            @Override
            public BaseMatcher<View> getConstraints() {
                return new BaseMatcher<View>() {
                    @Override
                    public boolean matches(Object item) {
                        return isA(Checkable.class).matches(item);
                    }

                    @Override
                    public void describeMismatch(Object item, Description mismatchDescription) {}

                    @Override
                    public void describeTo(Description description) {}
                };
            }

            @NonNull
            @Override
            public String getDescription() {
                return "click if needed to check";
            }

            @Override
            public void perform(UiController uiController, View view) {
                Checkable checkableView = (Checkable) view;

                if (checkableView.isChecked() != checked) {
                    click().perform(uiController, view);
                }
            }
        };
    }
}
