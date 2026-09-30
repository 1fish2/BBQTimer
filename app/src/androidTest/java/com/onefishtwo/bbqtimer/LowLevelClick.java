/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2025 Jerry Morrison
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

import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;

import static org.hamcrest.Matchers.allOf;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

/**
 * A ViewAction that directly calls {@link View#performClick()} on the UI thread.
 * WORKAROUND for an Android framework / Espresso event injection issue on API levels 29, 35,
 * and 36 where MotionEvents from ViewActions#click() fail to reach dialog buttons
 * (BUTTON_POSITIVE, BUTTON_NEGATIVE, or BUTTON_NEUTRAL), fail to dismiss the dialog, and then
 * Espresso times out waiting (DialogIdlingResource) for the dialog to close.
 * Manually clicking the button would resume the test.
 * Experiments showed that it's not a race condition. The click just doesn't get through.
 * <p>
 * LowLevelClick calls view.performClick(), which directly invokes the view's registered
 * OnClickListener in JVM code synchronously, thus bypassing all motion event injection, input
 * dispatchers, window layering, hit-testing, and coordinate math.
 * <p>
 * Note: If somehow the dialog dismissal issue requires posting the click event to the end of the
 * main looper queue (so the current event loop iteration completes first), replace
 * `view.performClick()` with `view.post(view::performClick)`.
 */
public class LowLevelClick implements ViewAction {
    @NonNull
    @Override
    public Matcher<View> getConstraints() {
        return allOf(isDisplayed(), isEnabled());
    }

    @NonNull
    @Override
    public String getDescription() {
        return "a low-level click";
    }

    @Override
    public void perform(@NonNull UiController uiController, @NonNull View view) {
        view.performClick();

        // Loop to let the main thread process the click event.
        uiController.loopMainThreadForAtLeast(50);
    }
}
