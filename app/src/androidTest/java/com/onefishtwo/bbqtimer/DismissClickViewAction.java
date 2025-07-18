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

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;

import org.hamcrest.Matcher;

/**
 * A ViewAction to click on a dismiss-dialog button then make Espresso wait until the dialog closes
 * before performing more actions.
 */
public class DismissClickViewAction implements ViewAction {
    private final String dialogTag;
    private final ViewAction click = ViewActions.click();

    /**
     * Construct a ViewAction to click on a dialog button that's intended to close it ("Save",
     * "Cancel", ...), then make Espresso wait until it's closed before performing more actions.
     */
    public DismissClickViewAction(String _dialogTag) {
        dialogTag = _dialogTag;
    }

    @Override
    public Matcher<View> getConstraints() {
        return click.getConstraints();
    }

    @Override
    public String getDescription() {
        return "click to dismiss a dialog and declare 'busy' until it closes";
    }

    @Override
    public void perform(UiController uiController, View view) {
        // Get the FragmentManager from the view's context.
        FragmentActivity activity = unwrap(view.getContext());
        FragmentManager fm = activity.getSupportFragmentManager();

        // Perform the click.
        click.perform(uiController, view);

        // Register a DialogIdlingResource AFTER performing the click to start blocking further
        // actions until the dialog closes. It will then unregister itself.
        DialogIdlingResource.registerNewIdlingResource(fm, dialogTag);
    }

    private static FragmentActivity unwrap(Context context) {
        while (!(context instanceof Activity) && context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
        }

        // This could throw ClassCastException if the Activity isn't a FragmentActivity.
        //noinspection DataFlowIssue
        return (FragmentActivity) context;
    }
}
