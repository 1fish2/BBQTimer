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

    private static final long[] VIBRATE_PATTERN = {150, 82, 180, 96}; // ms off, ms on, ms off, ...

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
     * Builder-style setter: Whether {@link #openOrCancel} should play a notification chime.
     * Default = false.
     */
    public Notifier setPlayChime(boolean playChime) {
        this.playChime = playChime;
        return this;
    }

    /**
     * Builder-style setter: Whether to vibrate and flash the notification light. Default = true.
     */
    public Notifier setVibrate(boolean vibrate) {
        this.vibrate = vibrate;
        return this;
    }

    private Uri getSoundUri(int soundId) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + soundId);
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
            Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(),
                    R.drawable.ic_large_notification);

            builder.setSmallIcon(R.drawable.notification_icon)
                    .setLargeIcon(largeIcon)
                    .setContentTitle(context.getString(R.string.app_name))
                    .setOngoing(true)
                    .setWhen(System.currentTimeMillis() - timer.getElapsedTime())
                    .setUsesChronometer(true);

            if (state.isEnableReminders()) { // show reminder alarm info in the notification body
                int reminderSecs   = state.getSecondsPerReminder();
                int minutes        = reminderSecs / 60;
                String contentText = minutes > 1
                        ? context.getString(R.string.notification_body, minutes)
                        : context.getString(R.string.notification_body_singular);
                long elapsedMs     = timer.getElapsedTime();
                int numReminders   = (int)(elapsedMs / (reminderSecs * 1000L));

                builder.setContentText(contentText);

                // WORKAROUND: On Android v12 HONEYCOMB_MR1, setNumber() will later crash with
                // Resources$NotFoundException "Resource ID #0x1050019" from
                // Resources.getDimensionPixelSize(). It works on API v15.
                //
                // On older builds, calling builder.setContentInfo() is a candidate workaround
                // except in fact it has no visible impact.
                if (numReminders > 0) {
                    if (Build.VERSION.SDK_INT >= 15) { // TODO: Test Android versions 13 .. 14.
                        builder.setNumber(numReminders);
                    }
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

            // Add an action Intent to pause the timer from the notification.
            PendingIntent pauseIntent = TimerAppWidgetProvider.makeActionIntent(context,
                    TimerAppWidgetProvider.ACTION_PAUSE);
            builder.addAction(R.drawable.ic_action_pause, context.getString(R.string.stop),
                    pauseIntent);

            // --- Introduced in API V21 Lollipop ---
            // Show action #0 (Pause) in MediaStyle's compact notification view, which is what
            // appears in lock screen notifications.
            builder.setMediaStyleActionsInCompactView(0);
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
