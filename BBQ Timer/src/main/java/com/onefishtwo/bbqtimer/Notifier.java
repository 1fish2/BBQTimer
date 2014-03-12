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

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

/**
 * Manages the app's Android Notifications.
 */
public class Notifier {
    private static final int NOTIFICATION_ID = 7;
    private static final int PRIORITY_HIGH = 1; // Notification.PRIORITY_HIGH added in API level 16
    private static final int PRIORITY_MAX  = 2;

    private final Context context;

    public Notifier(Context context) {
        this.context = context;
    }

    /** Opens this app's notification. */
    public void open() {
        // TODO: Show the notification only when the activity is stopped and the timer is running.
        // TODO: Update the notification periodically.
        // TODO: Control whether the user can clear the notification.
        // TODO: Play a custom sound when it's time for a periodic chime. MP3 Mono/Stereo 8-320Kbps CBR or VBR.
        // TODO: Content text, time, a large icon, ...
        // TODO: Set priority.
        // TODO: Large icon: mdpi 64x64 px, hdpi 96x96 px, xhdpi 128x128 px, xxhpdi 192x192 px.
        // TODO: setWhen() + setUsesChronometer()?
        // TODO: setTicker() text?
        // TODO: Button actions to pause/resume/reset the timer?
        // TODO: Lights?
        // TODO: Vibrate? (It requires the VIBRATE permission.)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText("n minutes elapsed")
                .setPriority(PRIORITY_HIGH) // PRIORITY_MAX?
                .setOngoing(true);
        Intent activityIntent = new Intent(context, MainActivity.class);

        // So navigating back from the Activity goes from the app to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context)
                .addParentStack(MainActivity.class)
                .addNextIntent(activityIntent);
        PendingIntent activityPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(activityPendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    /** Cancels all of this app's notifications. */
    public void cancelAll() {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.cancelAll();
    }
}
