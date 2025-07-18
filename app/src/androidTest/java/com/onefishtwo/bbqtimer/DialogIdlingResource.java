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

import android.util.Log;

import androidx.fragment.app.FragmentManager;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;

/**
 * An Espresso {@link IdlingResource} that monitors the presence of the tagged DialogFragment so an
 * Espresso test will wait until that dialog closes before performing more actions.
 * Some experiments indicate that Espresso checks IdlingResources before performing a ViewAction.
 */
public class DialogIdlingResource implements IdlingResource {
    public static final String TAG = "DialogIdlingResource";
    private final FragmentManager fragmentManager;
    private final String tag;
    private volatile ResourceCallback resourceCallback;

    /**
     * Create a new DialogIdlingResource and register it with the Espresso UI test framework.
     * Call this right after performing an action that dismisses a dialog so the next action will
     * wait until the dialog closes.
     * The DialogIdlingResource will unregister itself when it becomes idle.
     */
    static void registerNewIdlingResource(FragmentManager fm, String dialogTag) {
        IdlingResource idlingResource = new DialogIdlingResource(fm, dialogTag);
        IdlingRegistry.getInstance().register(idlingResource);
        Log.d(TAG, "registered");
    }

    public DialogIdlingResource(FragmentManager _fragmentManager, String _tag) {
        fragmentManager = _fragmentManager;
        tag = _tag;
    }

    @Override
    public String getName() {
        return DialogIdlingResource.class.getName() + ":" + tag;
    }

    @Override
    public boolean isIdleNow() {
        boolean idle = fragmentManager.findFragmentByTag(tag) == null;
        Log.d(TAG, idle ? "isIdleNow" : "not isIdleNow");
        if (idle) {
            if (resourceCallback != null) {
                resourceCallback.onTransitionToIdle();
            }
            IdlingRegistry.getInstance().unregister(this);
        }
        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback _resourceCallback) {
        resourceCallback = _resourceCallback;
    }
}
