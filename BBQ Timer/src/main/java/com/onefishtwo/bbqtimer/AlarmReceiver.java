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

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.onefishtwo.bbqtimer.state.ApplicationState;

/**
 * Uses AlarmManager to perform periodic reminder notifications.
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    /**
     * Whether to use alarmMgr.setAlarmClock() vs. set(). It's available on API v21 but on v23 it's
     * needed to wake up on time from idle/doze power saving modes. It displays another timer icon
     * in the notification bar and lock screen with tap-through to MainActivity to edit the timer.
     *<p/>
     * Hopefully this is a UI improvement but it displays the alarm time as an absolute minute
     * rather than an interval and it may seem broken when the alarm doesn't occur at the turn of
     * the minute.
     *<p/>
     * It uses RTC wall time instead of elapsed interval time, so convert time bases and listen
     * for clock adjustments.
     *<p/>
     * TODO: Workaround the early alarm that occurs when the clock gets adjusted forwards past the
     * wall alarm time. The early alarm occurs before the ACTION_TIME_CHANGED intent.
     * (TIMEZONE_CHANGED is OK.) Ideas: (1) If an alarm occurs more than 50ms before the expected
     * next elapsed time, reschedule instead of sounding an alarm. (2) When an alarm occurs, set a
     * 1ms elapsed time alarm to trigger the audible notification after the ACTION_TIME_CHANGED
     * intent, if any. (3) Set an elapsed alarm for the desired time and also an alarm clock for 1ms
     * later just to avoid Doze mode.
     */
    // The incomplete docs:
    // https://developer.android.com/preview/features/power-mgmt.html
    // http://developer.android.com/reference/android/app/AlarmManager.html#setAndAllowWhileIdle(int, long, android.app.PendingIntent)
    // https://developer.android.com/preview/testing/guide.html#doze-standby
    //
    // See also:
    // https://code.google.com/p/android-developer-preview/issues/detail?id=2225#c11
    // https://plus.google.com/u/0/+AndroidDevelopers/posts/GdNrQciPwqo
    // https://plus.google.com/+AndroidDevelopers/posts/94jCkmG4jff
    // https://newcircle.com/s/post/1739/2015/06/12/diving-into-android-m-doze
    // https://commonsware.com/blog/2015/06/03/random-musing-m-developer-preview-ugly-part-one.html
    // http://stackoverflow.com/search?q=%5Bandroid%5D+doze
    private static final boolean USE_SET_ALARM_CLOCK = android.os.Build.VERSION.SDK_INT >= 23;

    /** Whether to set wakeup time flexibility to save battery power (on supporting OS builds). */
    private static final boolean ALLOW_WAKEUP_FLEXIBILITY =
            !USE_SET_ALARM_CLOCK && android.os.Build.VERSION.SDK_INT >= 19;
    private static final long WAKEUP_WINDOW_MS = ALLOW_WAKEUP_FLEXIBILITY ? 50L : 0L;

    /** Constructs a PendingIntent for the AlarmManager to invoke AlarmReceiver. */
    private static PendingIntent makeAlarmPendingIntent(Context context) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        // See http://stackoverflow.com/questions/32492770
        if (android.os.Build.VERSION.SDK_INT >= 16) {
            intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        }

        // (ibid) "FLAG_CANCEL_CURRENT seems to be required to prevent a bug where the
        // intent doesn't fire after app reinstall in KitKat." -- It didn't seem to work better, but
        // it's hard to tell since MY_PACKAGE_REPLACED is unreliable, at least in the emulator. In
        // any case it breaks alarmMgr.cancel(), see http://stackoverflow.com/questions/26434490/
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    /** Constructs a PendingIntent for setAlarmClock() to show/edit the timer. */
    private static PendingIntent makeActivityPendingIntent(Context context) {
        Intent activityIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setAction(Intent.ACTION_EDIT); // distinguish from Launcher & Notifier intents

        return PendingIntent.getActivity(context, 0, activityIntent,
                PendingIntent.FLAG_ONE_SHOT);
    }

    /** Get a string description of an Intent, including extras, for debugging. */
    @SuppressWarnings("unused")
    public static String debugDumpIntent(Intent intent) {
        StringBuilder sb = new StringBuilder(intent.toString());

        Bundle extras = intent.getExtras();
        if (extras != null) {
            sb.append(" extras: {");

            for (String key : extras.keySet()) {
                sb.append(key).append(':').append(extras.get(key)).append(", ");
            }
            sb.append('}');
        }
        return sb.toString();
    }

    /** Returns the SystemClock.elapsedRealtime() for the next reminder notification. */
    //
    // TODO: If the user changes the period without resetting the timer, compute future reminders
    // relative to the previous reminder rather than 0:00? E.g. after a 7 minute reminder you change
    // it to 4 minutes, then it would next alert at 0:11:00 rather than 0:08:00.
    private static long nextReminderTime(ApplicationState state) {
        TimeCounter timer = state.getTimeCounter();
        long periodMs     = state.getMillisecondsPerReminder();
        long now          = timer.elapsedRealtimeClock();
        long timed        = timer.getElapsedTime();
        long untilNextReminder = periodMs - (timed % periodMs);

        // Don't (re)schedule within the wakeup window. That'd double-trigger the notification.
        // (Maybe don't even schedule within the alarm sound's duration.)
        if (untilNextReminder <= WAKEUP_WINDOW_MS) {
            untilNextReminder += periodMs;
        }

        return now + untilNextReminder;
    }

    /**
     * (Re)schedules the next reminder Notification via an AlarmManager Intent.
     * Deals with AlarmManager time windows and system idle/doze modes.
     */
    private static void scheduleNextReminder(Context context, ApplicationState state) {
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = makeAlarmPendingIntent(context);
        long nextReminder = nextReminderTime(state);

        if (USE_SET_ALARM_CLOCK) {
            setAlarmClockV21(context, alarmMgr, state, nextReminder, pendingIntent);
        } else if (android.os.Build.VERSION.SDK_INT >= 19) {
            setAlarmWindowV19(alarmMgr, nextReminder, pendingIntent);
        } else {
            alarmMgr.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, nextReminder, pendingIntent);
        }
    }

    /**
     * API v21 version of set-alarm. setAlarmClock() alarms should work even when the device/app is
     * idle/dozing in v23, unlike set(). [setAndAllowWhileIdle() is an alternative but it may wait
     * 15 minutes or hours, at least in the M Developer Preview.]
     *<p/>
     * Converts the elapsed time value to a wall clock time value for setAlarmClock().
     */
    @TargetApi(21)
    private static void setAlarmClockV21(Context context, AlarmManager alarmMgr,
            ApplicationState state, long nextReminder, PendingIntent pendingIntent) {
        PendingIntent activityPI = makeActivityPendingIntent(context);
        TimeCounter timer        = state.getTimeCounter();
        long reminderWallTime    = timer.elapsedTimeToWallTime(nextReminder);
        AlarmManager.AlarmClockInfo info =
                new AlarmManager.AlarmClockInfo(reminderWallTime, activityPI);

        alarmMgr.setAlarmClock(info, pendingIntent);
    }

    /**
     * API v19 version of set-alarm. Giving the OS a time window is supposed to let it save battery
     * power, but the newer Marshmallow APIs don't support that so maybe it didn't pan out.
     */
    @TargetApi(19)
    private static void setAlarmWindowV19(AlarmManager alarmMgr, long nextReminder,
            PendingIntent pendingIntent) {
        alarmMgr.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                nextReminder - WAKEUP_WINDOW_MS, WAKEUP_WINDOW_MS, pendingIntent);
    }

    /**
     * Returns the number of reminder alarms that happened by "now," allowing for the wakeup time
     * flexibility where alarms may arrive a little early.
     */
    static int numRemindersSoFar(ApplicationState state) {
        TimeCounter timer        = state.getTimeCounter();
        long elapsedMsWithWindow = timer.getElapsedTime() + WAKEUP_WINDOW_MS;
        long reminderMs          = state.getSecondsPerReminder() * 1000L;

        return (int)(elapsedMsWithWindow / reminderMs);
    }

    /**
     * Handles a clock or timezone adjustment by updating alarms as needed. It's needed for
     * AlarmManager.setAlarmClock() since that API uses RTC wall-clock time instead of elapsed
     * interval time.
     */
    public static void handleClockAdjustment(Context context) {
        if (USE_SET_ALARM_CLOCK) {
            updateNotifications(context);
        }
    }

    /**
     * Updates the app's Android Notifications area/drawer and scheduled periodic reminder
     * Notification alarms for the visible/invisible activity state, the running/paused timer state,
     * and the reminders-enabled state.
     */
    public static void updateNotifications(Context context) {
        ApplicationState state        = ApplicationState.sharedInstance(context);
        boolean enableReminders       = state.isEnableReminders();
        TimeCounter timer             = state.getTimeCounter();
        boolean isRunning             = timer.isRunning();
        Notifier notifier             = new Notifier(context);

        notifier.openOrCancel(state);

        if (isRunning && enableReminders) {
            scheduleNextReminder(context, state);
        } else {
            cancelReminders(context);
        }
    }

    /** Cancels any outstanding reminders by canceling the AlarmManager Intents. */
    public static void cancelReminders(Context context) {
        AlarmManager alarmMgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
        PendingIntent pendingIntent = makeAlarmPendingIntent(context);

        alarmMgr.cancel(pendingIntent);

        if (USE_SET_ALARM_CLOCK) {
            PendingIntent activityPI = makeActivityPendingIntent(context);

            alarmMgr.cancel(activityPI);
        }
    }

    /**
     * Handles an AlarmManager Intent: Shows/plays a reminder alarm and vibration via the Notifier.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        ApplicationState state = ApplicationState.sharedInstance(context);
        TimeCounter timer      = state.getTimeCounter();

        if (timer.isRunning()) {
            Log.d(TAG, intent.toString());
            Notifier notifier = new Notifier(context).setPlayChime(true).setVibrate(true);
            notifier.openOrCancel(state);

            scheduleNextReminder(context, state);
        }
    }
}
