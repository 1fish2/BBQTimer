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
import android.text.format.DateUtils;
import android.widget.RemoteViews;

/**
 * The BBQ Timer app widget for the home and lock screens.
 */
public class TimerAppWidgetProvider extends AppWidgetProvider {
    private static final int RUNNING_CHRONOMETER_CHILD = 0;
    private static final int PAUSED_CHRONOMETER_CHILD  = 1;

    static final String ACTION_START_STOP = "com.onefishtwo.bbqtimer.ACTION_START_STOP";
    static final String ACTION_CYCLE      = "com.onefishtwo.bbqtimer.ACTION_CYCLE";

    // Synchronized on the class.
    static String secondaryTextCache;

    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, TimerAppWidgetProvider.class);
    }

    /** Loads and returns the shared TimeCounter data. */
    // TODO: Share the persistent I/O code with MainActivity.
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
     * Unfortunately, a Chronometer view can't accurately set its value when paused. You can compute
     * (now - desiredTime) but even setting all widgets at once ends up with different displays due
     * to propagation delays. *SO* when the timer is paused, switch to a text view.
     */
    private static void updateWidgets(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds, TimeCounter timer) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);
        PendingIntent startStopIntent = makeActionIntent(context, ACTION_START_STOP);
        PendingIntent cycleIntent     = makeActionIntent(context, ACTION_CYCLE);

        if (timer.isRunning()) {
            // Note: setDisplayedChild() requires minSdkVersion 12 (in build.gradle).
            views.setDisplayedChild(R.id.viewFlipper, RUNNING_CHRONOMETER_CHILD);
            // TODO: Preload the button image Bitmap?
            views.setImageViewResource(R.id.remoteStartStopButton, R.drawable.ic_pause);
            views.setChronometer(R.id.chronometer, timer.getStartTime(), null, true);
        } else {
            long elapsedTime = timer.getElapsedTime();
            int textColorId  = timer.isReset() ? R.color.gray_text : R.color.orange_red_text;
            int textColor    = context.getResources().getColor(textColorId);

            views.setDisplayedChild(R.id.viewFlipper, PAUSED_CHRONOMETER_CHILD);
            views.setTextViewText(R.id.pausedChronometerText, TimeCounter.formatHhMmSs(elapsedTime));
            views.setTextColor(R.id.pausedChronometerText, textColor);
            // TODO: Preload the button image Bitmap?
            views.setImageViewResource(R.id.remoteStartStopButton, R.drawable.ic_play);
            // Stop the Chronometer in case it'd use battery power even when not displayed.
            // NOTE: A simpler implementation would stop the Chronometer then use it as the
            // paused TextView, but that relies on it not updating its text.
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
        TimeCounter timer = loadTimeCounter(context);

        updateWidgets(context, appWidgetManager, appWidgetIds, timer);
    }

    /** Updates the contents of all of this provider's app widgets. */
    static void updateAllWidgets(Context context, TimeCounter timer) {
        ComponentName componentName = getComponentName(context);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);

        // Get the array of widget IDs, and if it's not empty, call updateWidgets() to update them.
        if (appWidgetManager != null) {
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

            if (appWidgetIds.length > 0) {
                updateWidgets(context, appWidgetManager, appWidgetIds, timer);
            }
        }
    }

    /** Constructs a PendingIntent for the widget to send to open the main Activity. */
    static PendingIntent makeOpenMainActivityIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(context, 0, intent, 0);
    }

    /** Constructs a PendingIntent for the widget to send an action event to this Receiver. */
    static PendingIntent makeActionIntent(Context context, String action) {
        Intent intent = new Intent(context, TimerAppWidgetProvider.class);
        intent.setAction(action);
        return PendingIntent.getBroadcast(context, 0, intent, 0);
    }

    /** Receives an Intent, including one for the app widget's button clicks. */
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

        String action = intent.getAction();

        if (ACTION_START_STOP.equals(action)) {
            // The user tapped a widget's start/stop button.
            TimeCounter timer = loadTimeCounter(context);

            timer.toggleRunning();
            saveTimeCounter(context, timer);
            updateAllWidgets(context, timer);
        } else if (ACTION_CYCLE.equals(action)) {
            // The user tapped a widget's time text. Cycle Start/Pause/Reset.
            TimeCounter timer = loadTimeCounter(context);

            timer.cycle();
            saveTimeCounter(context, timer);
            updateAllWidgets(context, timer);
        } else if (Intent.ACTION_DATE_CHANGED.equals(action)
                || Intent.ACTION_TIME_CHANGED.equals(action)
                || Intent.ACTION_TIMEZONE_CHANGED.equals(action)) {
            // The clock ticked over to the next day or it was adjusted. Update the date display.
            TimeCounter timer = loadTimeCounter(context);

            clearSecondaryTextCache();
            updateAllWidgets(context, timer);
        }
    }
}
