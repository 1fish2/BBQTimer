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

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

/**
 * Uses AlarmManager to perform periodic reminder chimes.
 */
public class AlarmReceiver extends BroadcastReceiver {
    static final long PERIOD_MS = 5 * 60 * 1000L; // TODO: User-settable, including "never".

    public AlarmReceiver() {
    }

    /** Constructs a PendingIntent for the AlarmManager to invoke AlarmReceiver. */
    private static PendingIntent makeAlarmPendingIntent(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Schedules the next chime via an AlarmManager Intent. */
    public static void scheduleNextChime(Context context) {
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = makeAlarmPendingIntent(context);

        // TODO: Schedule once via .setWindow() android.os.Build.VERSION.SDK_INT >= 19, or via
        // .set() on earlier Android versions.
        // TODO: Schedule per the timer's elapsed time modulo the period.
        alarmMgr.setRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + PERIOD_MS, PERIOD_MS, pendingIntent);
    }

    /** Cancels any outstanding chimes via an AlarmManager Intent. */
    public static void cancelChimes(Context context) {
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = makeAlarmPendingIntent(context);

        alarmMgr.cancel(pendingIntent);
    }

    /**
     * Plays a chime via the Notifier when an AlarmManager Intent arrives. The notification includes
     * a visible notification iff the main activity is currently invisible.</p>
     *
     * TODO: Reschedule the next alarm.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        TimeCounter timer = ApplicationState.getTimeCounter(context);

        if (!timer.isRunning()) {
            return;
        }

        boolean isMainActivityVisible = ApplicationState.isMainActivityVisible(context);
        Notifier notifier = new Notifier(context).setPlayChime(true);

        notifier.setShowNotification(!isMainActivityVisible);
        notifier.openOrCancel(timer);
    }
}
