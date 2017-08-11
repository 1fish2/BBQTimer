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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;

import com.onefishtwo.bbqtimer.R;

/**
 * Notification Builder for API level 21+.
 * Previously this had to be implemented via Notification.Builder.
 * Now with appcompat-v7:23, NotificationCompat.Builder supports MediaStyle.
 *<p/>
 * ASSUMES: {@code Build.VERSION.SDK_INT >= 21}. Conditionally load this class.
 */
@TargetApi(21)
class V21Builder implements NotificationBuilder {
    @NonNull
    private final NotificationCompat.Builder builder;
    private final int workaroundColor;

    public V21Builder(@NonNull Context context, @NonNull String channelId) {
        builder = new NotificationCompat.Builder(context, channelId);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            workaroundColor = context.getColor(R.color.gray_text);
        } else {
            workaroundColor = 0x9e9e9e;
        }
    }

    @NonNull
    @Override
    public NotificationBuilder setWhen(long when) {
        builder.setWhen(when);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setShowWhen(boolean show) {
        builder.setShowWhen(show);
        return this;
    }

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
        // Android v24-25 (N): No-op.
        // Android v26 (O): Puts a number in the long-press menu of an app in supported launchers.
        builder.setNumber(number);
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
        builder.setLargeIcon(icon);
        return this;
    }

    @NonNull
    @Override
    public NotificationBuilder setSound(Uri sound) {
        // TODO: Switch to setSound(Uri, AudioAttributes) when supported by NotificationCompat.
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
        android.support.v4.media.app.NotificationCompat.MediaStyle style =
                new android.support.v4.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(actions);
        builder.setStyle(style);

        // Workaround a Marshmallow bug where the heads-up notification shows low contrast dark gray
        // text on darker gray background. setColor() sometimes sets the background color tho it's
        // supposed to set the accent color. Unfortunately it also changes pull-down notifications
        // and carries over from one notification to its replacement.
        // See http://stackoverflow.com/q/38415467/1682419

        // It'd be nice to call setColor() for other non-MediaStyle cases to set the accent color.
        // But on Android API K- it's risky since different handset builds tinkered with colors. On
        // Android N it seems to have no effect.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            builder.setColor(workaroundColor);
        }

        return this;
    }

    @Override
    public Notification build() {
        return builder.build();
    }
}
