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

import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import org.hamcrest.Matcher;

/**
 * A ViewAction that bypasses bunch of Espresso work to call View#performClick() from the UI thread.
 * This is a WORKAROUND for a bug on certain API levels (29, 35, 36) where a normal click() on one
 * of the standard dialog buttons (BUTTON_POSITIVE, BUTTON_NEGATIVE, BUTTON_NEUTRAL) fails to
 * dismiss the dialog, so Espresso times out waiting (DialogIdlingResource) on the dialog to close
 * <em>unless</em> I manually click the button while the test waits.
 * Experiments showed that it's not a race condition. The click just doesn't get through.
 *<p>
 * Gemini 2.5 Pro generated the first cut.
 */
public class LowLevelClick implements ViewAction {
    @Override
    public Matcher<View> getConstraints() {
        return isEnabled();
    }

    @Override
    public String getDescription() {
        return "a low-level click";
    }

    @Override
    public void perform(UiController uiController, View view) {
        Activity activity = getActivity(view);

        if (activity != null) {
            activity.runOnUiThread(view::performClick);
        } else {
            // Fallback for non-Activity contexts. This might cause a 5 sec delay.
            view.performClick();
        }

        // Loop to let the main thread process the click event.
        uiController.loopMainThreadForAtLeast(50);
    }

    @Nullable
    private static Activity getActivity(View view) {
        Context context = view.getContext();
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                return (Activity) context;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        return null;
    }
}
