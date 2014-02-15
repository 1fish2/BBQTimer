/*
 * Copyright (c) 2014 Jerry Morrison.
 */

package com.onefishtwo.bbqtimer;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.RemoteViews;

/**
 * The BBQ Timer app widget for the home and lock screens.
 */
public class TimerAppWidgetProvider extends AppWidgetProvider {

    /** Updates the given app widgets' contents to the Timer state. */
    private static void updateWidgets(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds, TimeCounter timer) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);

        views.setChronometer(R.id.chronometer, timer.chronometerTimeBase(),
                context.getString(R.string.widget_format), timer.isRunning());
        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    /**
     * Updates the given app widgets' contents. Called when an app widget instance was added to a
     * widget host (e.g. home screen or lock screen) and periodically at updatePeriodMillis.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        // Load persistent state.
        // TODO: Share the persistent I/O code with MainActivity, or use a ContentProvider, or ...
        // TODO: Ensure this gets up-to-date data.
        SharedPreferences prefs = context.getSharedPreferences(MainActivity.TIMER_PREF_FILE,
                Context.MODE_PRIVATE);
        TimeCounter timer = new TimeCounter();
        timer.load(prefs);

        updateWidgets(context, appWidgetManager, appWidgetIds, timer);
    }

    /** Updates the contents of all of this provider's app widgets. */
    static void updateAllWidgets(Context context, TimeCounter timer) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName componentName = new ComponentName(context, TimerAppWidgetProvider.class);
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(componentName);

        updateWidgets(context, appWidgetManager, appWidgetIds, timer);
    }
}
