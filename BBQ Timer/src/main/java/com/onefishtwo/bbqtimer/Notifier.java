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

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.app.TaskStackBuilder;

import com.onefishtwo.bbqtimer.notificationCompat.NotificationBuilder;
import com.onefishtwo.bbqtimer.notificationCompat.NotificationBuilderFactory;
import com.onefishtwo.bbqtimer.state.ApplicationState;

/**
 * Manages the app's Android Notifications.
 */
public class Notifier {
    private static final int NOTIFICATION_ID = 7;

    private static final long[] VIBRATE_PATTERN = {150, 82, 180, 96}; // ms off, ms on, ms off, ...
    private static final int[][] ACTION_INDICES = {{}, {0}, {0, 1}, {0, 1, 2}, {0, 1, 2}};

    private final Context context;
    private boolean showNotification = true;
    private boolean playChime = false;
    private boolean vibrate = false;

    public Notifier(Context context) {
        this.context = context;
    }

    /**
     * Builder-style setter: Whether {@link #openOrCancel} should display the notification in the
     * notification area and drawer. Default = true.
     */
    public Notifier setShowNotification(boolean showNotification) {
        this.showNotification = showNotification;
        return this;
    }

    /**
     * Builder-style setter: Whether {@link #openOrCancel} should play a notification chime for a
     * reminder. Default = false.
     */
    public Notifier setPlayChime(boolean playChime) {
        this.playChime = playChime;
        return this;
    }

    /**
     * Builder-style setter: Whether to vibrate and flash the notification light. Default = false.
     */
    public Notifier setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
        return this;
    }

    private Uri getSoundUri(int soundId) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + soundId);
    }

    /** Returns a localized description of the timer's run state, e.g. "Paused 00:12.3". */
    public String timerRunState(TimeCounter timer) {
        if (timer.isRunning()) {
            return context.getString(R.string.timer_running);
        } else if (timer.isPaused()) {
            return context.getString(R.string.timer_paused, timer.formatHhMmSsFraction());
        } else {
            return context.getString(R.string.timer_stopped);
        }
    }

    /**
     * <em>Opens</em> this app's notification with visible, audible, and/or tactile content
     * depending on {@link #setShowNotification(boolean)}, {@link #setPlayChime(boolean)}, and
     * {@link #setVibrate(boolean)}, <em>or cancels</em> the app's notification if they're all
     * false.
     *
     * @param state -- the ApplicationState state to display.
     */
    public void openOrCancel(ApplicationState state) {
        if (!(showNotification || playChime || vibrate)) {
            cancelAll();
            return;
        }

        Notification notification = buildNotification(state);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /** Builds a notification. */
    protected Notification buildNotification(ApplicationState state) {
        NotificationBuilder builder = NotificationBuilderFactory.builder(context)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (showNotification) {
            TimeCounter timer = state.getTimeCounter();
            boolean isRunning = timer.isRunning();
            Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_large_notification);

            builder.setSmallIcon(R.drawable.notification_icon)
                    .setLargeIcon(largeIcon)
                    .setContentTitle(context.getString(R.string.app_name));

            if (isRunning) {
                builder.setWhen(System.currentTimeMillis() - timer.getElapsedTime())
                        .setUsesChronometer(true);
            } else {
                // Hide the "when" field, which isn't useful while paused, so it doesn't take space
                // in the compact view along with 3 action buttons (Reset, Start, Stop). (The
                // alternative is to let Android show the time of day when pause occurred.)
                builder.setShowWhen(false);
            }

            // Set the notification body text to explain the run state.
            if (isRunning && state.isEnableReminders()) {
                int reminderSecs   = state.getSecondsPerReminder();
                int minutes        = reminderSecs / 60;
                String contentText = minutes > 1
                        ? context.getString(R.string.notification_body, minutes)
                        : context.getString(R.string.notification_body_singular);
                int numReminders   = AlarmReceiver.numRemindersSoFar(state);

                builder.setContentText(contentText);

                if (numReminders > 0) {
                    builder.setNumber(numReminders);
                }
            } else {
                builder.setContentText(timerRunState(timer));
                if (timer.isPaused()) {
                    builder.setSubText(context.getString(R.string.dismiss_tip));
                } else if (isRunning) {
                    builder.setSubText(context.getString(R.string.no_reminders_tip));
                }
            }

            // Make an Intent to launch the Activity from the notification.
            Intent activityIntent = new Intent(context, MainActivity.class);

            // So navigating back from the Activity goes from the app to the Home screen.
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context)
                    .addParentStack(MainActivity.class)
                    .addNextIntent(activityIntent);
            PendingIntent activityPendingIntent =
                    stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(activityPendingIntent);

            int numActions = 0;

            // Action button to reset the timer.
            PendingIntent resetIntent = TimerAppWidgetProvider.makeActionIntent(context,
                    TimerAppWidgetProvider.ACTION_RESET);
            if (timer.isPaused() && !timer.isReset()) {
                builder.addAction(R.drawable.ic_action_replay, context.getString(R.string.reset),
                        resetIntent);
                ++numActions;
            }

            // Action button to run the timer.
            PendingIntent runIntent = TimerAppWidgetProvider.makeActionIntent(context,
                    TimerAppWidgetProvider.ACTION_RUN);
            if (!isRunning) {
                builder.addAction(R.drawable.ic_action_play, context.getString(R.string.start),
                        runIntent);
                ++numActions;
            }

            // Action button to pause the timer.
            PendingIntent pauseIntent = TimerAppWidgetProvider.makeActionIntent(context,
                    TimerAppWidgetProvider.ACTION_PAUSE);
            if (!timer.isPaused()) {
                builder.addAction(R.drawable.ic_action_pause, context.getString(R.string.pause),
                        pauseIntent);
                ++numActions;
            }

            // Action button to stop the timer.
            PendingIntent stopIntent = TimerAppWidgetProvider.makeActionIntent(context,
                    TimerAppWidgetProvider.ACTION_STOP);
            if (!timer.isStopped()) {
                builder.addAction(R.drawable.ic_action_stop, context.getString(R.string.stop),
                        stopIntent);
                ++numActions;
            }

            // Allow stopping via dismissing the notification (unless it's "ongoing").
            builder.setDeleteIntent(stopIntent);

            // --- Introduced in API V21 Lollipop ---
            // Show 2 actions in MediaStyle's compact notification view (the view that appears in
            // lock screen notifications).
            builder.setMediaStyleActionsInCompactView(ACTION_INDICES[numActions]);

            if (isRunning) {
                builder.setOngoing(true);
            }
        }

        if (playChime || vibrate) {
            int defaults = Notification.DEFAULT_LIGHTS;

            if (playChime) {
                builder.setSound(getSoundUri(R.raw.cowbell4));
            }

            if (vibrate) {
                builder.setVibrate(VIBRATE_PATTERN);
            }
            builder.setDefaults(defaults);
        }

       return builder.build();
    }

    /** Cancels all of this app's notifications. */
    public void cancelAll() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);

        notificationManager.cancelAll();
    }
}
