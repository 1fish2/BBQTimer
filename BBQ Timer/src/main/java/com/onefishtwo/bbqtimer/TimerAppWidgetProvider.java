/*
 * Copyright (c) 2014 Jerry Morrison.
 */

package com.onefishtwo.bbqtimer;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.os.SystemClock;
import android.widget.RemoteViews;

/**
 * The BBQ Timer app widget for the home and lock screens.</p>
 *
 * TODO: Override onAppWidgetOptionsChanged() to handle widget resizing.
 */
public class TimerAppWidgetProvider extends AppWidgetProvider {

    /**
     * Updates the app widgets' contents. Called when an app widget instance was added to a widget
     * host (e.g. home screen or lock screen) and periodically at updatePeriodMillis.
     */
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        long now = SystemClock.elapsedRealtime(); // TODO: ...
        boolean started = true; // TODO ...
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);

        views.setChronometer(R.id.chronometer, now, context.getString(R.string.widget_format),
                started);
        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }
}
