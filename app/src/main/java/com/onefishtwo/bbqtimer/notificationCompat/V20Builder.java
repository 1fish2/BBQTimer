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

package com.onefishtwo.bbqtimer.notificationCompat;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

/**
 * Notification Builder for API level 20-, implemented via NotificationCompat.Builder plus
 * additional OS bug workarounds.
 */
class V20Builder implements NotificationBuilder {
    @NonNull
    private final NotificationCompat.Builder builder;

    public V20Builder(@NonNull Context context, @NonNull String channelId) {
        builder = new NotificationCompat.Builder(context, channelId);
    }

    @NonNull
    @Override
    public NotificationBuilder setWhen(long when) {
        builder.setWhen(when);
        return this;
    }

    /**
     * NOTE: This is supposed to work in API 17+ but it doesn't work right in API 17-18.
     */
    @NonNull
    @Override
    public NotificationBuilder setShowWhen(boolean show) {
        builder.setShowWhen(show);
        return this;
    }

    /**
     * NOTE: In API 17+.
     *<p/>
     * NOTE: Updating an existing notification to/from UsesChronometer only works in API 21+. In
     * API 19, going from chronometer to hide-the-time continues the running chronometer. In
     * API 16 - 18, hiding the When field doesn't actually work, and the changing to/from a
     * chronometer shows BOTH the time and the chronometer.
     */
    @NonNull
    @Override
    public NotificationBuilder setUsesChronometer(boolean b) {
        builder.setUsesChronometer(b);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setSmallIcon(int icon) {
        builder.setSmallIcon(icon);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setContentTitle(CharSequence title) {
        builder.setContentTitle(title);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setContentText(CharSequence text) {
        builder.setContentText(text);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setLights(int argb, int onMs, int offMs) {
        builder.setLights(argb, onMs, offMs);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setSubText(CharSequence text) {
        builder.setSubText(text);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setNumber(int number) {
        // Android < v12: No-op, ditto for setContentInfo().
        // Android v12 (HONEYCOMB_MR1): Will later crash with Resources$NotFoundException
        //   "Resource ID #0x1050019" from Resources.getDimensionPixelSize().
        // Android v13-14: Can't test it since those emulators don't work.
        // Android v15-23: Puts a number in the notification.
        // ...
        if (Build.VERSION.SDK_INT >= 15) {
            builder.setNumber(number);
        }

        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setContentIntent(PendingIntent intent) {
        builder.setContentIntent(intent);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setDeleteIntent(PendingIntent intent) {
        builder.setDeleteIntent(intent);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setLargeIcon(Bitmap icon) {
        // WORKAROUND: On Android level 12, setLargeIcon() will later crash with "FATAL EXCEPTION:
        // main android.app.RemoteServiceException: Bad notification posted ...: Couldn't expand
        // RemoteViews for: StatusBarNotification(...)".
        //
        // TODO: What about Android level 13-14?
        if (Build.VERSION.SDK_INT >= 15) {
            builder.setLargeIcon(icon);
        }

        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setSound(Uri sound) {
        builder.setSound(sound, AudioManager.STREAM_ALARM);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setVibrate(long[] pattern) {
        builder.setVibrate(pattern);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setOngoing(boolean ongoing) {
        builder.setOngoing(ongoing);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setCategory(String category) {
        builder.setCategory(category);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setDefaults(int defaults) {
        builder.setDefaults(defaults);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setPriority(int pri) {
        builder.setPriority(pri);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder addAction(int icon, CharSequence title, PendingIntent intent) {
        builder.addAction(icon, title, intent);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setVisibility(int visibility) {
        builder.setVisibility(visibility);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setMediaStyleActionsInCompactView(int... actions) {
        // Noop for Android API V20-.
        return this;
    }

    @Override
    public Notification build() {
        return builder.build();
    }
}
