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
public interface NotificationBuilder {
    public NotificationBuilder setWhen(long when);
    public NotificationBuilder setUsesChronometer(boolean b);
    public NotificationBuilder setSmallIcon(int icon);
    public NotificationBuilder setContentTitle(CharSequence title);
    public NotificationBuilder setContentText(CharSequence text);
    public NotificationBuilder setNumber(int number);
    public NotificationBuilder setContentIntent(PendingIntent intent);
    public NotificationBuilder setLargeIcon(Bitmap icon);
    public NotificationBuilder setSound(Uri sound);
    public NotificationBuilder setVibrate(long[] pattern);
    public NotificationBuilder setOngoing(boolean ongoing);
    public NotificationBuilder setCategory(String category);
    public NotificationBuilder setDefaults(int defaults);
    public NotificationBuilder setPriority(int pri);
    public NotificationBuilder addAction(int icon, CharSequence title, PendingIntent intent);
    public NotificationBuilder setVisibility(int visibility);

    /** Sets MediaStyle with setShowActionsInCompactView(). */
    public NotificationBuilder setMediaStyleActionsInCompactView(int... actions);

    public Notification build();
}
