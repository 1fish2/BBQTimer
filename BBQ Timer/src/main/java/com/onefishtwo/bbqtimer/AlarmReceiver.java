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

/**
 * Uses AlarmManager to perform periodic reminder notifications.
 */
public class AlarmReceiver extends BroadcastReceiver {
    static final long WINDOW_MS = 50L; // Allow some time flexibility to save battery power.

    public AlarmReceiver() {
    }

    /** Constructs a PendingIntent for the AlarmManager to invoke AlarmReceiver. */
    private static PendingIntent makeAlarmPendingIntent(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Returns the SystemClock.elapsedRealtime() for the next reminder notification. */
    //
    // TODO: If the user changes the period without resetting the timer, compute future reminders
    // relative to the previous reminder rather than 0:00? E.g. after a 7 minute reminder you change
    // it to 4 minutes, then it would next alert at 0:11:00 rather than 0:08:00.
    private static long nextReminderTime(Context context, TimeCounter timer) {
        long periodMs = ApplicationState.getSecondsPerReminder(context) * 1000L;
        long timed = timer.getElapsedTime();
        long untilNextReminder = periodMs - (timed % periodMs);
        long now = timer.elapsedRealtimeClock();

        return now + untilNextReminder;
    }

    /** (Re)schedules the next reminder Notification via an AlarmManager Intent. */
    public static void scheduleNextReminder(Context context, TimeCounter timer) {
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = makeAlarmPendingIntent(context);
        long nextReminder = nextReminderTime(context, timer);

        if (android.os.Build.VERSION.SDK_INT >= 19) {
            alarmMgr.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextReminder - WINDOW_MS,
                    WINDOW_MS, pendingIntent);
        } else {
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextReminder, pendingIntent);
        }
    }

    /** Cancels any outstanding reminders via an AlarmManager Intent. */
    public static void cancelReminders(Context context) {
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = makeAlarmPendingIntent(context);

        alarmMgr.cancel(pendingIntent);
    }

    /**
     * Handles an AlarmManager Intent: Plays a reminder chime and/or vibration via the Notifier. The
     * Notification is also visible in the notification area and drawer iff the main activity is not
     * currently visible.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        TimeCounter timer = ApplicationState.getTimeCounter(context);

        if (!timer.isRunning()) {
            return;
        }

        boolean isMainActivityVisible = ApplicationState.isMainActivityVisible(context);
        Notifier notifier = new Notifier(context).setPlayChime(true).setVibrate(true);

        notifier.setShowNotification(!isMainActivityVisible);
        notifier.openOrCancel(timer);

        scheduleNextReminder(context, timer);
    }
}
