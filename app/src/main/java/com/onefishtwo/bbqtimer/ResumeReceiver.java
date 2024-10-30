// The MIT License (MIT)
//
// Copyright (c) 2015 Jerry Morrison
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

import androidx.annotation.NonNull;

import com.onefishtwo.bbqtimer.state.ApplicationState;

/**
 * A BroadcastReceiver to resume/adjust/stop the running timer and notification after an app
 * upgrade, clock adjustment, timezone adjustment, locale change, or ACTION_BOOT_COMPLETED (either
 * system reboot then login, or user interaction with the app after Force Stop on Android 15+).
 *<p/>
 * NOTE: With Android 7.0 (API 24) and 8.0 (API 26) Broadcast Intent limitations, apps can register
 * for a subset of the original implicit broadcast actions, including:
 *   ACTION_TIME_CHANGED, ACTION_TIMEZONE_CHANGED, ACTION_LOCALE_CHANGED.
 * This intent is explicit to a specific package, so it still works:
 *   ACTION_MY_PACKAGE_REPLACED.
 *<p/>
 * BTW the OS doesn't send broadcasts to "stopped" applications. See
 * <a href="http://developer.android.com/about/versions/android-3.1.html#launchcontrols">Launch
 * Controls</a> and <a href="https://code.google.com/p/android/issues/detail?id=18225">Issue
 * 18225</a>.
 */
// TODO: Due to Android OS bug https://code.google.com/p/android/issues/detail?id=2880 , after
// the clock gets set backwards, the OS won't send ACTION_DATE_CHANGED until the clock catches
// up to what was going to be the next day.
//
// Cf http://stackoverflow.com/questions/21758246/android-action-date-changed-broadcast which
// suggests a workaround by implementing a similar alarm.
//
// Also see https://developer.android.com/about/versions/oreo/background.html#broadcasts on how
// manifest registration for ACTION_DATE_CHANGED implicit broadcasts doesn't work on Android 8+
// but this app needed them only on Android 4.4-.
public class ResumeReceiver extends BroadcastReceiver {
    private static final String TAG = "ResumeReceiver";

    /** Handles an incoming Intent. */
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        String action = intent.getAction();

        Log.i(TAG, "Broadcast intent: " + action);

        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            ApplicationState state = ApplicationState.sharedInstance(context);

            AlarmReceiver.updateNotifications(context);
            TimerAppWidgetProvider.updateAllWidgets(context, state);

        } else if (Intent.ACTION_TIME_CHANGED.equals(action) // "android.intent.action.TIME_SET"
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            AlarmReceiver.handleClockAdjustment(context);

        } else if (Intent.ACTION_LOCALE_CHANGED.equals(action)) {
            Notifier notifier = new Notifier(context);

            notifier.onLocaleChange();
            AlarmReceiver.updateNotifications(context);

        } else if (Intent.ACTION_BOOT_COMPLETED.equals(action)) {
            // BOOT_COMPLETED: Reboot then login or Android 15+ Force Stop then user interaction.
            // NOTE: Usually after reboot, ApplicationState.sharedInstance() already stopped the
            // timer due to a future startTime, and in that case Notifications will be clear and
            // APPWIDGET_UPDATE Intents will reset the widgets.
            // This code stops the timer after the remaining reboot case and resets everything after
            // the Force Stop case.
            ApplicationState state = ApplicationState.sharedInstance(context);
            TimeCounter timer = state.getTimeCounter();

            if (!timer.isStopped()) {
                timer.stop();
                state.save(context);
                Log.i(TAG, "*** Stopped and saved the timer after BOOT_COMPLETED");
                AlarmReceiver.updateNotifications(context);
                TimerAppWidgetProvider.updateAllWidgets(context, state);
            }
        }
    }
}
