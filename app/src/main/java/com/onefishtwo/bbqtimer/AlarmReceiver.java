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

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;

import com.onefishtwo.bbqtimer.state.ApplicationState;

/**
 * Uses AlarmManager to perform periodic reminder notifications.
 *<p>
 * ASSUMES: The app has USE_EXACT_ALARM permission (defined in API 33) and
 * SCHEDULE_EXACT_ALARM in API ≤ 32.
 */
public class AlarmReceiver extends BroadcastReceiver {
    private static final String TAG = "AlarmReceiver";

    // Some docs on alarms and doze mode that haven't gone offline:
    // https://developer.android.com/reference/android/app/AlarmManager#setAlarmClock(android.app.AlarmManager.AlarmClockInfo,%20android.app.PendingIntent)
    // https://developer.android.com/about/versions/14/changes/schedule-exact-alarms#migration
    // https://code.google.com/p/android-developer-preview/issues/detail?id=2225#c11
    // https://commonsware.com/blog/2015/06/03/random-musing-m-developer-preview-ugly-part-one.html
    // http://stackoverflow.com/search?q=%5Bandroid%5D+doze
    // http://stackoverflow.com/questions/32492770

    /**
     * An Extra to store an alarm Intent's target time, in system elapsed time msec.
     * setAlarmClock() uses a wall clock RTC target. If the clock gets adjusted forwards past that
     * target, the OS will trigger the alarm early (the goal time "passed") and <em>then</em> send
     * an ACTION_TIME_CHANGED intent.
     * <p/>
     * Workaround: If an alarm triggers early, reschedule it instead of notifying the user.
     */
    private static final String EXTRA_ELAPSED_REALTIME_TARGET =
            "com.onefishtwo.bbqtimer.ElapsedRealtimeTarget";
    public static final String ACTION_ALARM = "com.onefishtwo.bbqtimer.ACTION_ALARM";

    /** Tolerance value for an early alarm. */
    private static final long ALARM_TOLERANCE_MS = 10L;

    /**
     * Constructs a PendingIntent for the AlarmManager to invoke AlarmReceiver.
     *
     * @param elapsedRealtimeTarget the target time for this alarm, in system elapsed time msec.
     *                              This is stored in an Intent Extra to enable detecting if the
     *                              alarm triggered early. The value doesn't matter when making an
     *                              Intent to cancel the alarm since Extras don't affect Intent
     *                              retrieval.
     */
    @NonNull
    private static PendingIntent makeAlarmPendingIntent(@NonNull Context context,
            long elapsedRealtimeTarget) {
        Intent intent = new Intent(context, AlarmReceiver.class);

        // See https://stackoverflow.com/questions/32492770
        intent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        intent.setAction(ACTION_ALARM);
        intent.putExtra(EXTRA_ELAPSED_REALTIME_TARGET, elapsedRealtimeTarget);

        // (ibid) "FLAG_CANCEL_CURRENT seems to be required to prevent a bug where the
        // intent doesn't fire after app reinstall in KitKat." -- It didn't seem to work better, but
        // it's hard to tell since MY_PACKAGE_REPLACED is unreliable, at least in the emulator. In
        // any case it breaks alarmMgr.cancel(), see http://stackoverflow.com/questions/26434490/
        return PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /** Constructs a PendingIntent for AlarmManager.AlarmClockInfo() to show/edit the timer. */
    @NonNull
    private static PendingIntent makeActivityPendingIntent(@NonNull Context context) {
        Intent activityIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                .setAction(Intent.ACTION_EDIT); // distinguish from Launcher & Notifier intents

        return PendingIntent.getActivity(context, 0, activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE); // was FLAG_ONE_SHOT
    }

    /** Get a string description of an Intent, including extras, for debugging. */
    @NonNull
    @SuppressWarnings("unused")
    public static String debugDumpIntent(@NonNull Intent intent) {
        StringBuilder sb = new StringBuilder(intent.toString());
        Bundle extras = intent.getExtras();

        if (extras != null) {
            extras.keySet(); // <-- reify the Bundle so .toString() will elaborate
            sb.append(" extras: ").append(extras);
        }

        return sb.toString();
    }

    /** Returns the SystemClock.elapsedRealtime() for the next reminder notification. */
    private static long nextReminderTime(@NonNull ApplicationState state) {
        TimeCounter timer = state.getTimeCounter();
        long periodMs     = state.getMillisecondsPerReminder();
        long now          = timer.elapsedRealtimeClock();

        if (periodMs <= 0) { // prevent divide-by-zero
            Log.w(TAG, "Invalid reminder interval: " + periodMs);
            return now;
        }

        long timed = Math.max(0, timer.getElapsedTime());
        long untilNextReminder = periodMs - (timed % periodMs);

        // Don't (re)schedule within a small window. That'd double-alarm if the notification
        // arrives on the early side of the given window due to a clock adjustment or AlarmManager
        // window. (Maybe don't even schedule within the alarm sound's duration.)
        //
        // NOTE: This could play a double-alarm if the Intent arrives ALARM_TOLERANCE_MS early (due
        // to the clock getting adjusted backwards) then this code runs within the same msec. That's
        // unlikely, less bad than dropping an alarm, and attempts to avoid it caused worse problems
        // with a second alarm ~5 seconds after the regular alarm if Android was busy in another
        // app. onReceive() takes 9-60 ms [?] to open a notifier, not 5 secs, so that's not it.
        if (untilNextReminder < ALARM_TOLERANCE_MS) {
            untilNextReminder += periodMs;
        }

        return now + untilNextReminder;
    }

    /**
     * (Re)schedules the next reminder Notification via an AlarmManager Intent.
     * Deals with system idle/doze modes.
     */
    private static void scheduleNextReminder(@NonNull Context context,
            @NonNull ApplicationState state) {
        AlarmManager alarmMgr = context.getSystemService(AlarmManager.class);
        long nextReminder = nextReminderTime(state);
        PendingIntent pendingIntent = makeAlarmPendingIntent(context, nextReminder);

        if (alarmMgr == null) {
            Log.w(TAG, "scheduleNextReminder: null alarmMgr");
            return;
        }

        setAlarmClock(context, alarmMgr, state, nextReminder, pendingIntent);
    }

    static boolean canScheduleAlarms(@NonNull AlarmManager alarmMgr) {
        return Build.VERSION.SDK_INT < 31 || alarmMgr.canScheduleExactAlarms();
    }

    private static void warnAboutMissingAlarmPermission(@NonNull Context context) {
        try {
            Toast.makeText(context, R.string.need_alarm_access, Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            // e.g. CalledFromWrongThreadException or NullPointerException when on a receiver,
            // background, or worker thread
            Log.e(TAG, "Couldn't show MissingAlarmPermission Toast", e);
        }
    }

    /**
     * Converts the elapsed time value to a wall clock time value and calls setAlarmClock().
     * setAlarmClock() is supposed to be exact and wake the device even if dozing.
     * <p>
     * setAlarmClock() displays a user-visible alarm clock icon in the notification bar, with
     * further alarm info in the system notification widgets.
     * <p>
     * API 31 - 32: setAlarmClock() needs revocable SCHEDULE_EXACT_ALARM. The app could ask the user
     * to re-grant the SCHEDULE_EXACT_ALARM permission via a dialog then invoke an intent with the
     * ACTION_REQUEST_SCHEDULE_EXACT_ALARM intent action. SCHEDULE_EXACT_ALARM is a Special App
     * Access permission, not a runtime permission. The OS never auto-revokes special app access
     * permissions due to inactivity. So it would get revoked only if the user digs into that system
     * settings screen, which should be too rare to mess with code to open a Snackbar to open that
     * settings screen, monitor the settings change, etc.
     * <p>
     * API 33+: non-revocable USE_EXACT_ALARM for calendar and alarm clock apps.
     * <p>
     * TODO: Ask the user for permission? Degrade to setExactAndAllowWhileIdle() or an inexact
     *  alarm? Is that useful enough?
     *
     * @param nextReminder the SystemClock.elapsedRealtime() for the next reminder notification
     * @param pendingIntent the PendingIntent to wake this receiver in nextReminder msec
     */
    @RequiresPermission(anyOf = {
            android.Manifest.permission.USE_EXACT_ALARM,
            android.Manifest.permission.SCHEDULE_EXACT_ALARM,
            android.Manifest.permission.SET_ALARM})
    private static void setAlarmClock(@NonNull Context context, @NonNull AlarmManager alarmMgr,
            @NonNull ApplicationState state, long nextReminder, @NonNull PendingIntent pendingIntent) {
        if (!canScheduleAlarms(alarmMgr)) {
            Log.e(TAG, "Cannot schedule exact alarm: missing permission");
            warnAboutMissingAlarmPermission(context);
            return;
        }

        PendingIntent activityPI = makeActivityPendingIntent(context);
        TimeCounter timer        = state.getTimeCounter();
        long reminderWallTime    = timer.elapsedTimeToWallTime(nextReminder);
        AlarmManager.AlarmClockInfo info =
                new AlarmManager.AlarmClockInfo(reminderWallTime, activityPI);

        try {
            alarmMgr.setAlarmClock(info, pendingIntent);
        } catch (SecurityException | IllegalStateException e) {
            Log.e(TAG, "Need SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM permission", e);
            // NOTE: A Toast appears for a home screen widget but not for a notification button.
            warnAboutMissingAlarmPermission(context);
        }
    }

    /**
     * Handles a clock or timezone adjustment (ACTION_TIME_CHANGED or
     * ACTION_TIMEZONE_CHANGED) by updating alarms as needed. It's needed with:
     *<ul>
     *     <li>{@link AlarmManager#setAlarmClock(AlarmManager.AlarmClockInfo, PendingIntent)} to
     *     reschedule the alarm since that API uses RTC wall-clock time instead of elapsed
     *     interval time.</li>
     *     <li>{@link AlarmManager#set(int, long, PendingIntent)} to work around
     *     <a href="https://code.google.com/p/android/issues/detail?id=2880">Issue 2880</a>, where
     *     setting the clock backwards delays outstanding alarms.</li>
     *</ul>
     */
    public static void handleClockAdjustment(@NonNull Context context) {
        updateNotifications(context);
    }

    /**
     * Updates the app's Android Notifications area/drawer and scheduled periodic reminder
     * Notification alarms for the running/paused timer state and the reminders-enabled state.
     * (It no longer depends on the visible/invisible activity state.)
     */
    public static void updateNotifications(@NonNull Context context) {
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
    public static void cancelReminders(@NonNull Context context) {
        AlarmManager alarmMgr = context.getSystemService(AlarmManager.class);
        PendingIntent pendingIntent = makeAlarmPendingIntent(context, 0);
        PendingIntent activityPI = makeActivityPendingIntent(context);

        if (alarmMgr == null) {
            Log.w(TAG, "cancelReminders: null alarmMgr");
            return;
        }

        alarmMgr.cancel(pendingIntent);
        alarmMgr.cancel(activityPI);
    }

    /**
     * Returns true if the Intent is more than {@link #ALARM_TOLERANCE_MS} earlier than its
     * setAlarmClock() target time. See {@link #EXTRA_ELAPSED_REALTIME_TARGET} for why.
     */
    private boolean isAlarmEarly(@NonNull Intent intent, @NonNull TimeCounter timer) {
        long now    = timer.elapsedRealtimeClock();
        long target = intent.getLongExtra(EXTRA_ELAPSED_REALTIME_TARGET, now);
        long howLate = now - target;

        if (howLate < -ALARM_TOLERANCE_MS) {
            Log.i(TAG, "ALARM EARLY " + (-howLate) + " msec " + intent);
            return true;
        } else if (howLate > ALARM_TOLERANCE_MS) {
            Log.i(TAG, "ALARM LATE " + howLate + " msec " + intent);
        }
        return false;
    }

    /**
     * Handles an AlarmManager Intent: Shows/plays a reminder alarm and vibration via the Notifier
     * and schedules the next repeating alarm. Detects and quiets early alarms.
     */
    @SuppressLint("VisibleForTests")
    @Override
    public final void onReceive(@NonNull Context context, @NonNull Intent intent) {
        TimerAppWidgetProvider.saveIntentActionForTesting(context, intent);

        if (!ACTION_ALARM.equals(intent.getAction())) {
            return;
        }

        ApplicationState state = ApplicationState.sharedInstance(context);
        TimeCounter timer      = state.getTimeCounter();

        if (timer.isRunning()) {
            if (!isAlarmEarly(intent, timer)) {
                Log.i(TAG, intent.toString()); // intent.getAction() == null
                Notifier notifier = new Notifier(context).setAlarm(true);
                notifier.openOrCancel(state);
                TimerAppWidgetProvider.updateAllWidgets(context, state);
            }

            scheduleNextReminder(context, state);
        }
    }
}
