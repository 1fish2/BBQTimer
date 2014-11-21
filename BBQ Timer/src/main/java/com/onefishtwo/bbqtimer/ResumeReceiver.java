// The MIT License (MIT)
//
// Copyright (c) 2014 Jerry Morrison
//
// Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
// associated documentation files (the "Software"), to deal in the Software without restriction,
// including without limitation the rights to use, copy, modify, merge, publish, distribute,
// sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all copies or
// substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
// NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
// NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
// DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

package com.onefishtwo.bbqtimer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * A BroadcastReceiver to resume the if-running timer and notification after an app upgrade
 * (MY_PACKAGE_REPLACED action).
 *<p/>
 * TODO: Consider also watching for <a
 * href="http://developer.android.com/reference/android/content/Intent.html#ACTION_BOOT_COMPLETED">
 * ACTION_BOOT_COMPLETED</a> to either resume running (although that requires saving the wall clock
 * start time) or to set the saved state to "stopped."
 *<p/>
 * BTW the OS doesn't send broadcasts to "stopped" applications. See
 * <a href="http://developer.android.com/about/versions/android-3.1.html#launchcontrols">Launch
 * Controls</a>.
 */
public class ResumeReceiver extends BroadcastReceiver {
    private static final String TAG = "ResumeReceiver";

    /** Handles an incoming Intent. */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "*** " + intent.getAction() + " ***");
        AlarmReceiver.updateNotifications(context);

        // NOTE: The widgets should stay in the right state but if that doesn't always work:
        // TimerAppWidgetProvider.updateAllWidgets(context, state);
    }
}
