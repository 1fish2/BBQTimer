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
import androidx.annotation.NonNull;

/**
 * An interface to alternate Notification[Compat] Builder implementations (based on android.app,
 * android.support.v4.app, android.support.v7.app, android.support.v4.media.app) to workaround bugs
 * like <a href="https://code.google.com/p/android-developer-preview/issues/detail?id=1659">Issue
 * 1659: "There is no NotificationCompat.MediaStyle in the support library"</a>, which required
 * different implementations on different versions of Android.
 *<p/>
 * This interface has only the features needed by this app.
 */
@SuppressWarnings({"SameParameterValue", "UnusedReturnValue"})
public interface NotificationBuilder {
    @NonNull
    NotificationBuilder setWhen(long when);
    @NonNull
    NotificationBuilder setShowWhen(boolean show);
    @NonNull
    NotificationBuilder setUsesChronometer(boolean b);
    @NonNull
    NotificationBuilder setSmallIcon(int icon);
    @NonNull
    NotificationBuilder setContentTitle(CharSequence title);
    @NonNull
    NotificationBuilder setContentText(CharSequence text);
    @NonNull
    NotificationBuilder setLights(int argb, int onMs, int offMs);
    @NonNull
    NotificationBuilder setSubText(CharSequence text);
    @SuppressWarnings("unused")
    @NonNull
    NotificationBuilder setNumber(int number);
    @NonNull
    NotificationBuilder setContentIntent(PendingIntent intent);
    @NonNull
    NotificationBuilder setDeleteIntent(PendingIntent intent);
    @NonNull
    NotificationBuilder setLargeIcon(Bitmap icon);
    @NonNull
    NotificationBuilder setSound(Uri sound);
    @NonNull
    NotificationBuilder setVibrate(long[] pattern);
    @NonNull
    NotificationBuilder setOngoing(boolean ongoing);
    @NonNull
    NotificationBuilder setCategory(String category);
    @SuppressWarnings("unused")
    @NonNull
    NotificationBuilder setDefaults(int defaults);
    @NonNull
    NotificationBuilder setPriority(int pri);
    @NonNull
    NotificationBuilder addAction(int icon, CharSequence title, PendingIntent intent);
    @NonNull
    NotificationBuilder setVisibility(int visibility);

    /** Sets MediaStyle with setShowActionsInCompactView(). */
    @NonNull
    NotificationBuilder setMediaStyleActionsInCompactView(int... actions);

    Notification build();
}
