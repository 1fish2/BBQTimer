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

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
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

    // Marshmallow rejects invisible notifications (IllegalArgumentException if there's no small
    // icon, and just adding a small icon shows a nearly empty notification). Either show a visible
    // notification in the Activity (despite the style guide) or implement another way to play
    // the alarm on Marshmallow. Showing a notification is nice because it gives immediate feedback
    // on starting/stopping notifications that are accessible on the Lollipop+ lock screen and
    // visible alarm feedback (just like out-of-activity).
    //
    private static final boolean IN_ACTIVITY_NOTIFICATIONS =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

    /**
     * On API 21+, allow Paused notifications with Reset/Stop/Start buttons and Running
     * notifications with Pause/Stop buttons, mainly so the user can control the timer from a lock
     * screen notification. Distinguish Stopped from Paused in the UI.
     * <p/>
     * Due to OS bugs in API 17 - 20, changing a notification to Paused would continue showing a
     * running chronometer [despite calling setShowWhen(false) and not calling
     * setUsesChronometer(true)] and sometimes also the notification time of day, both confusing.
     * API < 16 has no action buttons so it has no payoff in Paused notifications.
     */
    static final boolean PAUSEABLE_NOTIFICATIONS = android.os.Build.VERSION.SDK_INT >= 21;

    private static final long[] VIBRATE_PATTERN = {150, 82, 180, 96}; // ms off, ms on, ms off, ...
    private static final int[][] ACTION_INDICES = {{}, {0}, {0, 1}, {0, 1, 2}};

    private final Context context;
    private boolean playChime = false;
    private boolean vibrate = false;
    private int numActions; // the number of action buttons added to the notification

    public Notifier(Context context) {
        this.context = context;
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

    /** Constructs a PendingIntent to use as a Notification Action. */
    private PendingIntent makeActionIntent(String action) {
        return TimerAppWidgetProvider.makeActionIntent(context, action);
    }

    /**
     * Adds an action button to the given NotificationBuilder and to the
     * {@link #setMediaStyleActionsInCompactView} list.
     */
    private void addAction(NotificationBuilder builder, int iconId, int titleId,
            PendingIntent intent) {
        builder.addAction(iconId, context.getString(titleId), intent);
        ++numActions;
    }

    /**
     * Makes the first 3 added {@link #addAction} actions appear in MediaStyle's compact
     * notification view (which is the view that appears in lock screen notifications in API V21
     * Lollipop). No-op before API 21.
     */
    private void setMediaStyleActionsInCompactView(NotificationBuilder builder) {
        int num = Math.min(numActions, ACTION_INDICES.length - 1);

        if (num < 1) {
            return;
        }
        builder.setMediaStyleActionsInCompactView(ACTION_INDICES[num]);
    }

    /** Returns a localized description of the timer's run state, e.g. "Paused 00:12.3". */
    String timerRunState(TimeCounter timer) {
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
     * depending on {@code state}, {@link #setPlayChime(boolean)}, and {@link #setVibrate(boolean)},
     * <em>or cancels</em> the app's notification if there's nothing to show or sound.
     *
     * @param state -- the ApplicationState state to display.
     */
    public void openOrCancel(ApplicationState state) {
        boolean isMainActivityVisible = state.isMainActivityVisible();
        TimeCounter timer             = state.getTimeCounter();
        boolean showable = PAUSEABLE_NOTIFICATIONS ? !timer.isStopped() : timer.isRunning();
        boolean show     = showable && (IN_ACTIVITY_NOTIFICATIONS || !isMainActivityVisible);

        if (!(show || playChime || vibrate)) {
            cancelAll();
            return;
        }

        Notification notification = buildNotification(state, show);
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(NOTIFICATION_ID, notification);
    }

    /** Builds a notification, optionally visible, audible, tactile. */
    protected Notification buildNotification(ApplicationState state, boolean show) {
        NotificationBuilder builder = NotificationBuilderFactory.builder(context)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        if (IN_ACTIVITY_NOTIFICATIONS) {
            show = true;
        }

        if (show) {
            TimeCounter timer = state.getTimeCounter();
            boolean isRunning = timer.isRunning();
            Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_large_notification);

            builder.setSmallIcon(R.drawable.notification_icon)
                    .setLargeIcon(largeIcon)
                    .setContentTitle(context.getString(R.string.app_name));

            if (isRunning) {
                builder.setWhen(System.currentTimeMillis() - timer.getElapsedTime())
                        .setUsesChronometer(true); // added in API 17
            } else {
                // Hide the "when" field, which isn't useful while Paused, so it doesn't take space
                // in the compact view along with 3 action buttons (Reset, Start, Stop).
                // This doesn't actually work in API 17-18, so in API 18- it'd show the time of day
                // when the notification is built, but it's even more broken after changing the
                // "When" info in an open notification.
                builder.setShowWhen(false);
            }

            // Set the notification body text to explain the run state.
            if (isRunning && state.isEnableReminders()) {
                int reminderSecs   = state.getSecondsPerReminder();
                int minutes        = reminderSecs / 60;
                String contentText = context.getResources()
                        .getQuantityString(R.plurals.notification_body, minutes, minutes);
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

            numActions = 0;

            // Action button to reset the timer.
            PendingIntent resetIntent = makeActionIntent(TimerAppWidgetProvider.ACTION_RESET);
            if (timer.isPaused() && !timer.isReset()) {
                addAction(builder, R.drawable.ic_action_replay, R.string.reset, resetIntent);
            }

            // Action button to run (start) the timer.
            PendingIntent runIntent = makeActionIntent(TimerAppWidgetProvider.ACTION_RUN);
            if (!isRunning) {
                addAction(builder, R.drawable.ic_action_play, R.string.start, runIntent);
            }

            // Action button to pause the timer.
            PendingIntent pauseIntent = makeActionIntent(TimerAppWidgetProvider.ACTION_PAUSE);
            if (!timer.isPaused()) {
                addAction(builder, R.drawable.ic_action_pause, R.string.pause, pauseIntent);
            }

            // Action button to stop the timer.
            PendingIntent stopIntent = makeActionIntent(TimerAppWidgetProvider.ACTION_STOP);
            if (PAUSEABLE_NOTIFICATIONS && !timer.isStopped()) {
                addAction(builder, R.drawable.ic_action_stop, R.string.stop, stopIntent);
            }

            // Allow stopping via dismissing the notification (unless it's "ongoing").
            builder.setDeleteIntent(stopIntent);

            setMediaStyleActionsInCompactView(builder);

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
