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

import android.app.Notification;
import android.app.PendingIntent;
import android.graphics.Bitmap;
import android.net.Uri;

/**
 * An interface to alternate implementations of
 * {@link android.support.v4.app.NotificationCompat.Builder} to workaround
 * <a href="https://code.google.com/p/android-developer-preview/issues/detail?id=1659">Issue
 * 1659: "There is no NotificationCompat.MediaStyle in the support library"</a>. It only has the
 * features needed by this app.
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
public interface NotificationBuilder {
    NotificationBuilder setWhen(long when);
    NotificationBuilder setShowWhen(boolean show);
    NotificationBuilder setUsesChronometer(boolean b);
    NotificationBuilder setSmallIcon(int icon);
    NotificationBuilder setContentTitle(CharSequence title);
    NotificationBuilder setContentText(CharSequence text);
    NotificationBuilder setSubText(CharSequence text);
    NotificationBuilder setNumber(int number);
    NotificationBuilder setContentIntent(PendingIntent intent);
    NotificationBuilder setDeleteIntent(PendingIntent intent);
    NotificationBuilder setLargeIcon(Bitmap icon);
    NotificationBuilder setSound(Uri sound);
    NotificationBuilder setVibrate(long[] pattern);
    NotificationBuilder setOngoing(boolean ongoing);
    NotificationBuilder setCategory(String category);
    NotificationBuilder setDefaults(int defaults);
    NotificationBuilder setPriority(int pri);
    NotificationBuilder addAction(int icon, CharSequence title, PendingIntent intent);
    NotificationBuilder setVisibility(int visibility);

    /** Sets MediaStyle with setShowActionsInCompactView(). */
    NotificationBuilder setMediaStyleActionsInCompactView(int... actions);

    // NOTE: I tried using setColor() to set the accent color. But on Android API K- it's not
    // worth testing across different handset builds that tinkered with notification colors. On
    // Android L-M it's unsafe since setting a notification's accent color will sometimes set its
    // replacement MediaStyle heads-up notification's background color. On Android N it seems to
    // have no effect.
    //
    // It might be worth calling setColor() on Android M to work around the low-contrast text bug
    // http://stackoverflow.com/q/38415467/1682419

    Notification build();
}
