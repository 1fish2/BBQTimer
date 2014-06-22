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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.text.format.DateUtils;
import android.widget.RemoteViews;

import com.onefishtwo.bbqtimer.state.ApplicationState;

/**
 * The BBQ Timer app widget for the home and lock screens.
 */
public class TimerAppWidgetProvider extends AppWidgetProvider {
    // --- R.id.viewFlipper child indexes.
    private static final int RUNNING_CHRONOMETER_CHILD = 0;
    private static final int PAUSED_CHRONOMETER_CHILD  = 1;
    private static final int RESET_CHRONOMETER_CHILD   = 2;

    static final String ACTION_START_STOP = "com.onefishtwo.bbqtimer.ACTION_START_STOP";
    static final String ACTION_PAUSE      = "com.onefishtwo.bbqtimer.ACTION_PAUSE";
    static final String ACTION_CYCLE      = "com.onefishtwo.bbqtimer.ACTION_CYCLE";

    // Synchronized on the class.
    private static String secondaryTextCache;

    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, TimerAppWidgetProvider.class);
    }

    /** Returns the formatted date (or other secondary text string). */
    static synchronized String secondaryText() {
        if (secondaryTextCache == null) {
            long now = System.currentTimeMillis();
            secondaryTextCache = DateUtils.formatDateTime(null, now,
                    DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_SHOW_WEEKDAY | DateUtils.FORMAT_ABBREV_WEEKDAY);
        }
        return secondaryTextCache;
    }

    static synchronized void clearSecondaryTextCache() {
        secondaryTextCache = null;
    }

    /**
     * Updates the given app widgets' contents to the Timer state.</p>
     *
     * Unfortunately, a Chronometer view can't accurately set its value when paused. You can set it
     * to (now - desiredTime) but even setting all widgets at once ends up with different displays
     * due to propagation delays. *SO* when the timer is paused, switch to a TextView.</p>
     *
     * NOTE: A simpler implementation would stop the Chronometer then use it as the paused TextView,
     * but that relies on it not updating its text. Anyway, this code switches between the
     * Chronometer and two different TextViews to select the right ColorStateList for user feedback
     * since RemoteViews can't do {@code TextView#setTextColor(ColorStateList)}.</p>
     *
     * NOTE: setDisplayedChild() requires minSdkVersion 12 (in build.gradle).</p>
     *
     * TODO: Preload the button image Bitmaps? Flip between Buttons?
     */
    private static void updateWidgets(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds, ApplicationState state) {
        TimeCounter timer = state.getTimeCounter();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);
        PendingIntent startStopIntent = makeActionIntent(context, ACTION_START_STOP);
        PendingIntent cycleIntent     = makeActionIntent(context, ACTION_CYCLE);

        if (timer.isRunning()) {
            views.setDisplayedChild(R.id.viewFlipper, RUNNING_CHRONOMETER_CHILD);
            views.setImageViewResource(R.id.remoteStartStopButton, R.drawable.ic_pause);
            views.setChronometer(R.id.chronometer, timer.getStartTime(), null, true);
        } else {
            long elapsedTime = timer.getElapsedTime();
            int child = timer.isReset() ? RESET_CHRONOMETER_CHILD : PAUSED_CHRONOMETER_CHILD;
            int textViewId = child == RESET_CHRONOMETER_CHILD ? R.id.resetChronometerText
                    : R.id.pausedChronometerText;

            views.setDisplayedChild(R.id.viewFlipper, child);
            views.setTextViewText(textViewId, TimeCounter.formatHhMmSs(elapsedTime));
            views.setImageViewResource(R.id.remoteStartStopButton, R.drawable.ic_play);
            // Stop the Chronometer in case it'd use battery power even when not displayed.
            views.setChronometer(R.id.chronometer, 0, null, false);
        }

        views.setTextViewText(R.id.secondaryText, secondaryText());
        views.setOnClickPendingIntent(R.id.remoteStartStopButton, startStopIntent);
        views.setOnClickPendingIntent(R.id.viewFlipper, cycleIntent);

        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    /**
     * Updates the given app widgets' contents. Called when an app widget instance was added to a
     * widget host (e.g. home screen or lock screen) and periodically at updatePeriodMillis.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ApplicationState state = ApplicationState.sharedInstance(context);

        updateWidgets(context, appWidgetManager, appWidgetIds, state);
    }

    /** Updates the contents of all of this provider's app widgets. */
    static void updateAllWidgets(Context context, ApplicationState state) {
        ComponentName componentName = getComponentName(context);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        // Get the array of widget IDs, and if it's not empty, call updateWidgets() to update them.
        if (appWidgetManager != null) {
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

            if (appWidgetIds.length > 0) {
                updateWidgets(context, appWidgetManager, appWidgetIds, state);
            }
        }
    }

    /** Constructs a PendingIntent for the widget to send an action event to this Receiver. */
    static PendingIntent makeActionIntent(Context context, String action) {
        Intent intent = new Intent(context, TimerAppWidgetProvider.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /** Receives an Intent, including those from the app widget buttons and notification actions. */
    //
    // TODO: Due to Android OS bug https://code.google.com/p/android/issues/detail?id=2880 , after
    // the clock gets set backwards, the OS won't send ACTION_DATE_CHANGED until the clock catches
    // up to what was going to be the next day.
    //
    // Cf http://stackoverflow.com/questions/21758246/android-action-date-changed-broadcast which
    // suggests a workaround by implementing a similar alarm.
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        String action          = intent.getAction();
        ApplicationState state = ApplicationState.sharedInstance(context);
        TimeCounter timer      = state.getTimeCounter();

        if (ACTION_START_STOP.equals(action)) {
            // The user tapped a widget's start/stop button.
            timer.toggleRunning();
            state.save(context);

            updateAllWidgets(context, state);
            AlarmReceiver.updateNotifications(context);
        } else if (ACTION_PAUSE.equals(action)) {
            // The user tapped a notification's pause button.
            timer.pause();
            state.save(context);

            updateAllWidgets(context, state);
            AlarmReceiver.updateNotifications(context);
        } else if (ACTION_CYCLE.equals(action)) {
            // The user tapped a widget's time text. Cycle Start/Pause/Reset.
            timer.cycle();
            state.save(context);

            updateAllWidgets(context, state);
            AlarmReceiver.updateNotifications(context);
        } else if (Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            // The clock ticked over to the next day or it was adjusted. Update the date display.
            clearSecondaryTextCache();
            updateAllWidgets(context, state);
        }
    }
}
