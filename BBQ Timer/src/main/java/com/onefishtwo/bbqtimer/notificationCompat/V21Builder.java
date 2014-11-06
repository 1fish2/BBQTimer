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

package com.onefishtwo.bbqtimer.notificationCompat;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;

/**
 * Notification Builder for API level 21+, implemented via Notification.Builder since
 * NotificationCompat.Builder doesn't support MediaStyle.
 *</p>
 * ASSUMES: Build.VERSION.SDK_INT >= 21. Conditionally load this class.
 */
@TargetApi(21)
class V21Builder implements NotificationBuilder {
    private Notification.Builder builder;

    public V21Builder(Context context) {
        builder = new Notification.Builder(context);
    }

    @Override
    public NotificationBuilder setWhen(long when) {
        builder.setWhen(when);
        return this;
    }

    @Override
    public NotificationBuilder setUsesChronometer(boolean b) {
        builder.setUsesChronometer(b);
        return this;
    }

    @Override
    public NotificationBuilder setSmallIcon(int icon) {
        builder.setSmallIcon(icon);
        return this;
    }

    @Override
    public NotificationBuilder setContentTitle(CharSequence title) {
        builder.setContentTitle(title);
        return this;
    }

    @Override
    public NotificationBuilder setContentText(CharSequence text) {
        builder.setContentText(text);
        return this;
    }

    @Override
    public NotificationBuilder setSubText(CharSequence text) {
        builder.setSubText(text);
        return this;
    }

    @Override
    public NotificationBuilder setNumber(int number) {
        builder.setNumber(number);
        return this;
    }

    @Override
    public NotificationBuilder setContentIntent(PendingIntent intent) {
        builder.setContentIntent(intent);
        return this;
    }

    @Override
    public NotificationBuilder setDeleteIntent(PendingIntent intent) {
        builder.setDeleteIntent(intent);
        return this;
    }

    @Override
    public NotificationBuilder setLargeIcon(Bitmap icon) {
        builder.setLargeIcon(icon);
        return this;
    }

    @Override
    public NotificationBuilder setSound(Uri sound) {
        builder.setSound(sound);
        return this;
    }

    @Override
    public NotificationBuilder setVibrate(long[] pattern) {
        builder.setVibrate(pattern);
        return this;
    }

    @Override
    public NotificationBuilder setOngoing(boolean ongoing) {
        builder.setOngoing(ongoing);
        return this;
    }

    @Override
    public NotificationBuilder setCategory(String category) {
        builder.setCategory(category);
        return this;
    }

    @Override
    public NotificationBuilder setDefaults(int defaults) {
        builder.setDefaults(defaults);
        return this;
    }

    @Override
    public NotificationBuilder setPriority(int pri) {
        builder.setPriority(pri);
        return this;
    }

    @Override
    public NotificationBuilder addAction(int icon, CharSequence title, PendingIntent intent) {
        builder.addAction(icon, title, intent);
        return this;
    }

    @Override
    public NotificationBuilder setVisibility(int visibility) {
        builder.setVisibility(visibility);
        return this;
    }

    @Override
    public NotificationBuilder setMediaStyleActionsInCompactView(int... actions) {
        new Notification.MediaStyle(builder).setShowActionsInCompactView(actions);
        return this;
    }

    @Override
    public Notification build() {
        return builder.build();
    }
}
