/*
 * Copyright (c) 2014 Jerry Morrison.
 */

package com.onefishtwo.bbqtimer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

/**
 * The BBQ Timer app widget for the home and lock screens.
 */
public class TimerAppWidgetProvider extends AppWidgetProvider {
    private static final int RUNNING_CHRONOMETER_CHILD = 0;
    private static final int PAUSED_CHRONOMETER_CHILD  = 1;

    static final String ACTION_START_STOP = "com.onefishtwo.bbqtimer.ACTION_START_STOP";

    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, TimerAppWidgetProvider.class);
    }

    static TimeCounter loadTimeCounter(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.TIMER_PREF_FILE,
                Context.MODE_PRIVATE);
        TimeCounter timer = new TimeCounter();
        timer.load(prefs);
        return timer;
    }

    static void saveTimeCounter(Context context, TimeCounter timer) {
        SharedPreferences prefs =
                context.getSharedPreferences(MainActivity.TIMER_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        timer.save(prefsEditor);
        prefsEditor.commit();
    }

    /**
     * Updates the given app widgets' contents to the Timer state.</p>
     *
     * Unfortunately, a Chronometer view can't accurately set its value when paused. You can compute
     * (now - desiredTime) but even setting all widgets at once ends up with different displays due
     * to propagation delays. *SO* when the timer is paused, switch to a text view.
     */
    private static void updateWidgets(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds, TimeCounter timer) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);
        PendingIntent startStopIntent = makeStartStopIntent(context);
        PendingIntent openMainActivityIntent = makeOpenMainActivityIntent(context);

        if (timer.isRunning()) {
            // Note: setDisplayedChild() requires minSdkVersion 12 (in build.gradle).
            views.setDisplayedChild(R.id.viewFlipper, RUNNING_CHRONOMETER_CHILD);
            // TODO: Preload the button image Bitmap?
            views.setImageViewResource(R.id.remoteStartStopButton, R.drawable.ic_pause);
            views.setChronometer(R.id.chronometer, timer.getStartTime(), null, true);
        } else {
            long elapsedTime = timer.getElapsedTime();
            views.setDisplayedChild(R.id.viewFlipper, PAUSED_CHRONOMETER_CHILD);
            views.setTextViewText(R.id.pausedChronometerTextView,
                    TimeCounter.formatHhMmSs(elapsedTime));
            // TODO: Preload the button image Bitmap?
            views.setImageViewResource(R.id.remoteStartStopButton, R.drawable.ic_play);
            // Stop the Chronometer in case it'd use battery power even when not displayed.
            // NOTE: A simpler implementation would stop the Chronometer then use it as the
            // paused TextView, but that relies on it not updating its text.
            views.setChronometer(R.id.chronometer, 0, null, false);
        }

        views.setOnClickPendingIntent(R.id.remoteStartStopButton, startStopIntent);
        views.setOnClickPendingIntent(R.id.viewFlipper, openMainActivityIntent);

        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    /**
     * Updates the given app widgets' contents. Called when an app widget instance was added to a
     * widget host (e.g. home screen or lock screen) and periodically at updatePeriodMillis.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Load persistent state.
        // TODO: Share the persistent I/O code with MainActivity, or use a ContentProvider or a
        // bound Service or...
        // TODO: Ensure this gets up-to-date data.
        TimeCounter timer = loadTimeCounter(context);

        updateWidgets(context, appWidgetManager, appWidgetIds, timer);
    }

    /** Updates the contents of all of this provider's app widgets. */
    static void updateAllWidgets(Context context, TimeCounter timer) {
        ComponentName componentName = getComponentName(context);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        if (appWidgetManager != null) {
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

            if (appWidgetIds.length > 0) {
                updateWidgets(context, appWidgetManager, appWidgetIds, timer);
            }
        }
    }

    static PendingIntent makeOpenMainActivityIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    static PendingIntent makeStartStopIntent(Context context) {
        Intent intent = new Intent(context, TimerAppWidgetProvider.class);
        intent.setAction(ACTION_START_STOP);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /** Receives an Intent, including one for the app widget's button clicks. */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);

        if (ACTION_START_STOP.equals(intent.getAction())) {
            TimeCounter timer = loadTimeCounter(context);
            timer.toggleRunning();
            saveTimeCounter(context, timer);
            updateAllWidgets(context, timer);
        }
    }
}
