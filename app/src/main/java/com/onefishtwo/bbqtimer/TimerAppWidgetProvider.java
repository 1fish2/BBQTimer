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

package com.onefishtwo.bbqtimer;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SizeF;
import android.view.View;
import android.widget.RemoteViews;

import com.onefishtwo.bbqtimer.state.ApplicationState;

import java.util.Map;

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

/**
 * The BBQ Timer app widget for the home and lock screens.
 */
public class TimerAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "AppWidgetProvider";

    // --- R.id.viewFlipper child indexes.
    private static final int PAUSED_CHRONOMETER_CHILD  = 0;
    private static final int RUNNING_CHRONOMETER_CHILD = 1;
    private static final int RESET_CHRONOMETER_CHILD   = 2;

    static final String ACTION_RUN_PAUSE  = "com.onefishtwo.bbqtimer.ACTION_RUN_PAUSE";
    static final String ACTION_RUN        = "com.onefishtwo.bbqtimer.ACTION_RUN";
    static final String ACTION_PAUSE      = "com.onefishtwo.bbqtimer.ACTION_PAUSE";
    static final String ACTION_RESET      = "com.onefishtwo.bbqtimer.ACTION_RESET";
    static final String ACTION_STOP       = "com.onefishtwo.bbqtimer.ACTION_STOP";
    static final String ACTION_CYCLE      = "com.onefishtwo.bbqtimer.ACTION_CYCLE";

    private static final int FLAG_IMMUTABLE =
            android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;

    @NonNull
    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, TimerAppWidgetProvider.class);
    }

    /**
     * Updates the given widgets' contents to the Timer state and time.<p/>
     *
     * Workaround: A paused Chronometer doesn't show a stable value. Multiple widgets might show
     * different values, switching light/dark theme might change it, etc. Furthermore, a paused
     * Chronometer ignores its format string thus ruling out some workarounds.
     * *SO* when the timer is paused, flip to a TextView.<p/>
     *
     * This code switches between each Chronometer and two alternate TextViews to select the right
     * ColorStateList for user feedback since RemoteViews.setColor() is only in API 31+.<p/>
     */
    private static void updateWidgets(@NonNull Context context,
            @NonNull AppWidgetManager appWidgetManager,
            int[] appWidgetIds, @NonNull ApplicationState state) {
        TimeCounter timer = state.getTimeCounter();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);
        PendingIntent runPauseIntent  = makeActionIntent(context, ACTION_RUN_PAUSE);
        PendingIntent cycleIntent     = makeActionIntent(context, ACTION_CYCLE);
        PendingIntent activityIntent  = MainActivity.makePendingIntent(context);
        boolean visibleCountdown = false;

        // Show a periodic reminder time if enabled and countdown Chronometers are supported.
        // TODO: Hide it if the layout is narrow?
        if (Build.VERSION.SDK_INT >= 24 && state.isEnableReminders()) {
            visibleCountdown = true;
            views.setViewVisibility(R.id.countdownFlipper, View.VISIBLE);
            views.setChronometerCountDown(R.id.countdownChronometer, true);
        } else {
            views.setViewVisibility(R.id.countdownFlipper, View.GONE);
        }

        if (timer.isRunning()) {
            views.setDisplayedChild(R.id.viewFlipper, RUNNING_CHRONOMETER_CHILD);
            views.setImageViewResource(R.id.remoteStartStopButton, R.drawable.ic_action_pause);
            views.setChronometer(R.id.chronometer, timer.getStartTime(), null, true);

            if (visibleCountdown) {
                long rt = SystemClock.elapsedRealtime();
                long countdownToNextAlarm = state.getMillisecondsToNextAlarm();
                long countdownBase = rt + countdownToNextAlarm;

                views.setDisplayedChild(R.id.countdownFlipper, RUNNING_CHRONOMETER_CHILD);
                views.setChronometer(R.id.countdownChronometer, countdownBase,
                        null, true);
            }
        } else {
            int child = timer.isStopped() ? RESET_CHRONOMETER_CHILD : PAUSED_CHRONOMETER_CHILD;
            @IdRes int textViewId = child == RESET_CHRONOMETER_CHILD
                    ? R.id.resetChronometerText
                    : R.id.pausedChronometerText;
            @IdRes int countdownTextViewId = child == RESET_CHRONOMETER_CHILD
                    ? R.id.countdownResetChronometerText
                    : R.id.countdownPausedChronometerText;
            @DrawableRes int ic_pause_or_play = child == RESET_CHRONOMETER_CHILD
                    ? R.drawable.ic_action_pause : R.drawable.ic_action_play;

            views.setDisplayedChild(R.id.viewFlipper, child);
            views.setTextViewText(textViewId, timer.formatHhMmSs());
            views.setImageViewResource(R.id.remoteStartStopButton, ic_pause_or_play);
            // Stop the Chronometer in case it'd use battery power even when not displayed.
            views.setChronometer(R.id.chronometer, 0, null, false);

            if (visibleCountdown) {
                long countdownToNextAlarm = state.getMillisecondsToNextAlarm();

                views.setDisplayedChild(R.id.countdownFlipper, child);
                views.setTextViewText(countdownTextViewId,
                        TimeCounter.formatHhMmSs(countdownToNextAlarm));
                views.setChronometer(R.id.countdownChronometer, 0, null, false);
            }
        }

        views.setOnClickPendingIntent(R.id.remoteStartStopButton, runPauseIntent);
        views.setOnClickPendingIntent(R.id.viewFlipper, cycleIntent);
        views.setOnClickPendingIntent(R.id.background, activityIntent);

        // Use a view mapping to make the widget layout responsive to ever smaller sizes by first
        // hiding the countdown view, then hiding the count-up view.
        //
        // The viewMapping and layout managers react to actual sizes so this works well on
        // Android 12+. onAppWidgetOptionsChanged() only gets the size range [minWidth x maxHeight]
        // (for portrait) to [maxWidth x minHeight] (for landscape) and rarely gets notified when
        // the screen rotates, so that can contribute to a fallback on older Android versions.
        //
        // TODO: Ideas for earlier API versions:
        //  * Set each widget to the small/medium/large view that fits its minWidth (conservative).
        //  * Use landscape/portrait resources to make the text smaller.
        //  * Use a portrait resource to arrange the two view flippers vertically.
        if (Build.VERSION.SDK_INT >= 31) {
            RemoteViews largeView  = views;
            RemoteViews mediumView = new RemoteViews(views);

            mediumView.setViewVisibility(R.id.countdownFlipper, View.GONE);
            mediumView.setChronometer(R.id.countdownChronometer, 0, null, false);

            // Workaround: When "hiding" the count-up view, don't make it GONE/INVISIBLE since that
            // can get stuck hidden (at least on Android 31 on Nexus 7, until rotating the screen).
            // (Is this because the widget or view shortens vertically? Attempts to maintain the
            // height didn't fix it.) So instead, reduce the count-up view to an empty text field
            // and make its click action act like the background's. [Setting its width to 0 or 0.1
            // defeats the stuck-hidden workaround. setFocusable(false) does nothing.
            // setClickable(false) makes smallView fail to load.]
            RemoteViews smallView = new RemoteViews(mediumView);
            smallView.setTextViewText(R.id.pausedChronometerText, "");
            smallView.setDisplayedChild(R.id.viewFlipper, PAUSED_CHRONOMETER_CHILD);
            smallView.setOnClickPendingIntent(R.id.viewFlipper, activityIntent);
            smallView.setChronometer(R.id.chronometer, 0, null, false);

            Map<SizeF, RemoteViews> viewMapping = new ArrayMap<>();
            viewMapping.put(new SizeF( 40, 40), smallView);
            viewMapping.put(new SizeF(160, 40), mediumView);
            viewMapping.put(new SizeF(240, 40), largeView);

            views = new RemoteViews(viewMapping);
        }

        appWidgetManager.updateAppWidget(appWidgetIds, views);
    }

    /**
     * Updates the given app widgets' contents. Called when an app widget instance was added to a
     * widget host (e.g. home screen) (including after onRestored(), when widgets get restored from
     * backup) and periodically at updatePeriodMillis (if it's not 0).
     */
    @Override
    public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        ApplicationState state = ApplicationState.sharedInstance(context);

        updateWidgets(context, appWidgetManager, appWidgetIds, state);
    }

    /** Updates the contents of all of this provider's app widgets. */
    static void updateAllWidgets(@NonNull Context context, @NonNull ApplicationState state) {
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

    /**
     * Responds to a single widget instance getting resized. We can use this to help make the layout
     * responsive.
     *<p/>
     * NOTE: newOptions provides size range, reportedly minWidth x maxHeight for portrait
     * orientation, maxWidth x minHeight for landscape orientation.
     *<p/>
     * NOTE: This does not usually get called when the screen rotates landscape/portrait.
     */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        newOptions.keySet(); // reify the Bundle's contents so .toString() will format them
        Log.i(TAG, "WidgetOptionsChanged: " + newOptions);

        // onUpdate(context, appWidgetManager, new int[]{appWidgetId});
    }

    /** Constructs a PendingIntent for the widget to send an action event to this Receiver. */
    static PendingIntent makeActionIntent(Context context, String action) {
        Intent intent = new Intent(context, TimerAppWidgetProvider.class);
        intent.setAction(action).addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        return PendingIntent.getBroadcast(context, 0, intent, FLAG_IMMUTABLE);
    }

    /**
     * Receives an Intent, including those from the app widget buttons, notification actions,
     * and system clock changes.
     */
    // TODO: Due to Android OS bug https://code.google.com/p/android/issues/detail?id=2880 , after
    // the clock gets set backwards, the OS won't send ACTION_DATE_CHANGED until the clock catches
    // up to what was going to be the next day.
    //
    // Cf http://stackoverflow.com/questions/21758246/android-action-date-changed-broadcast which
    // suggests a workaround by implementing a similar alarm.
    //
    // Also see https://developer.android.com/about/versions/oreo/background.html#broadcasts on how
    // manifest registration for ACTION_DATE_CHANGED implicit broadcasts doesn't work on Android O+
    // but this class only needs them on K-.
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        super.onReceive(context, intent);

        String action          = intent.getAction();
        ApplicationState state = ApplicationState.sharedInstance(context);
        TimeCounter timer      = state.getTimeCounter();

        Log.v(TAG, action);

        if (ACTION_RUN_PAUSE.equals(action)) { // The user tapped a Run/Pause button.
            timer.togglePauseRun();
            saveStateAndUpdateUI(context, state);
        } else if (ACTION_RUN.equals(action)) { // The user tapped a Run (aka Play) button.
            timer.start();
            saveStateAndUpdateUI(context, state);
        } else if (ACTION_PAUSE.equals(action)) { // The user tapped a Pause button.
            timer.pause();
            saveStateAndUpdateUI(context, state);
        } else if (ACTION_RESET.equals(action)) { // The user tapped a Reset button.
            timer.reset();
            saveStateAndUpdateUI(context, state);
        } else if (ACTION_STOP.equals(action)) { // tapped a Stop button or swiped the notification.
            timer.stop();
            saveStateAndUpdateUI(context, state);
        } else if (ACTION_CYCLE.equals(action)) { // The user tapped the time text.
            timer.cycle();
            saveStateAndUpdateUI(context, state);
        }
    }

    /** Saves app state then updates the Widgets and Notifications. */
    private void saveStateAndUpdateUI(@NonNull Context context, @NonNull ApplicationState state) {
        state.save(context);

        updateAllWidgets(context, state);
        AlarmReceiver.updateNotifications(context);
    }
}
