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
     * Updates all the given widget instances' layout and contents.
     * <p/>
     * TODO: API 31+ could reuse the same RemoteViews instances for all widget instances.
     * TODO: onAppWidgetOptionsChanged() could pass in newOptions.
     */
    private static void updateWidgets(@NonNull Context context,
            @NonNull AppWidgetManager appWidgetManager,
            int[] appWidgetIds, @NonNull ApplicationState state) {
        for (int id: appWidgetIds) {
            updateWidget(context, appWidgetManager, id, state);
        }
    }

    /**
     * Updates a widget instance's layout to its size range and contents to the Timer state & time.
     * <p/>
     * Workaround: A paused Chronometer doesn't show a stable value. Multiple widgets might show
     * different values, switching light/dark theme might change it, etc. So construct its time
     * text. Furthermore, a paused Chronometer ignores its format string, so flip to a TextView.
     * <p/>
     * This code switches between each Chronometer and two alternate TextViews to select the right
     * ColorStateList for user feedback since RemoteViews.setColor() is only in API 31+.
     * <p/>
     * A ViewFlipper is larger than its displayed child due to margins, the LinearLayout's size
     * adjustments, and maybe more factors. So make its child view and the Pause/Play button react
     * to taps and let the rest of the widget be the "background" where tapping opens the Activity.
     */
    private static void updateWidget(@NonNull Context context,
            @NonNull AppWidgetManager appWidgetManager,
            int appWidgetId, @NonNull ApplicationState state) {
        TimeCounter timer = state.getTimeCounter();
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);
        PendingIntent runPauseIntent  = makeActionIntent(context, ACTION_RUN_PAUSE);
        PendingIntent cycleIntent     = makeActionIntent(context, ACTION_CYCLE);
        PendingIntent activityIntent  = MainActivity.makePendingIntent(context);
        boolean visibleCountdown = false;

        // Show a periodic reminder time if enabled and countdown Chronometers are supported.
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
            views.setOnClickPendingIntent(R.id.chronometer, cycleIntent);

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
            views.setOnClickPendingIntent(textViewId, cycleIntent);

            if (visibleCountdown) {
                long countdownToNextAlarm = state.getMillisecondsToNextAlarm();

                views.setDisplayedChild(R.id.countdownFlipper, child);
                views.setTextViewText(countdownTextViewId,
                        TimeCounter.formatHhMmSs(countdownToNextAlarm));
                views.setChronometer(R.id.countdownChronometer, 0, null, false);
            }
        }

        views.setOnClickPendingIntent(R.id.remoteStartStopButton, runPauseIntent);
        views.setOnClickPendingIntent(android.R.id.background, activityIntent);

        // Make the widget layout responsive to ever-smaller sizes by first hiding the countdown
        // view, then hiding the count-up view.
        //
        // Android 12+ supports view mappings to switch between layouts without waking the app.
        // View mappings and layout managers react to actual sizes.
        //
        // For Android < 12, use the onAppWidgetOptionsChanged() hook for when the user resizes a
        // widget, but it only receives the size range [minWidth x maxHeight] (for portrait) to
        // [maxWidth x minHeight] (for landscape), and it rarely gets called when the screen
        // rotates, so it can only set a layout for the size range. Any finer responsiveness must be
        // implemented by the layout managers and density/size/orientation specific resources.
        //
        // TODO: Improve the results on earlier Android versions:
        //  * Shrink the portrait-specific text dimens then adjust the layout thresholds?
        //  * Use a portrait-specific resource to arrange the two view flippers vertically?
        //
        // NOTES:
        //  * autoSizeText (API 26+) doesn't work well. The text shrinks very small then won't grow
        //    back when the space grows.
        //  * Word wrapping is ugly.
        //  * SingleLine is uglier, forcing ellipses that hide more digits.
        if (Build.VERSION.SDK_INT >= 31) {
            RemoteViews mediumViews = new RemoteViews(views);
            trimToMediumLayout(mediumViews);

            RemoteViews smallViews = new RemoteViews(mediumViews);
            trimMediumToSmallLayout(smallViews);

            Map<SizeF, RemoteViews> viewMapping = new ArrayMap<>();
            viewMapping.put(new SizeF( 40, 40), smallViews);
            viewMapping.put(new SizeF(160, 40), mediumViews);
            viewMapping.put(new SizeF(240, 40), views);

            views = new RemoteViews(viewMapping);
        } else {
            Bundle widgetOptions = appWidgetManager.getAppWidgetOptions(appWidgetId);
            int minWidth = widgetOptions.getInt(
                    AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 160);

            if (minWidth < 240) {
                trimToMediumLayout(views);
            }
            if (minWidth < 160) {
                trimMediumToSmallLayout(views);
            }
        }

        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /** Trim the original (large) layout down to the medium layout. */
    // Unfortunately, the RemoteViews copy constructor requires API 28+.
    private static void trimToMediumLayout(RemoteViews rv) {
        rv.setViewVisibility(R.id.countdownFlipper, View.GONE);
        rv.setChronometer(R.id.countdownChronometer, 0, null, false);
    }

    /** Trim the medium layout down to the small layout. */
    // Workaround: When "hiding" the count-up view, don't make it GONE/INVISIBLE since that can get
    // stuck hidden (at least on Android 31 on Nexus 7, until rotating the screen). (Is this because
    // the widget or view shortens vertically? Attempts to hold the height didn't fix it.) So shrink
    // the count-up view to an empty text field. [Setting the text view's width to 0 or 0.1 defeats
    // the stuck-hidden workaround.]
    private static void trimMediumToSmallLayout(RemoteViews rv) {
        rv.setTextViewText(R.id.pausedChronometerText, "");
        rv.setDisplayedChild(R.id.viewFlipper, PAUSED_CHRONOMETER_CHILD);
        rv.setChronometer(R.id.chronometer, 0, null, false);
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
     * Responds to a single widget instance resized by the user. Use this to adjust the layout,
     * except on API >= 31 where the viewMapping handles it without waking the app.
     *<p/>
     * NOTE: newOptions provides a size range as minWidth x maxHeight for portrait orientation;
     * maxWidth x minHeight for landscape orientation.
     *<p/>
     * NOTE: This does not usually get called when the screen rotates landscape/portrait.
     *<p/>
     * TODO: Passing newOptions to updateWidget() might save a little time.
     */
    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager,
            int appWidgetId, Bundle newOptions) {
        ApplicationState state = ApplicationState.sharedInstance(context);

        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);

        newOptions.keySet(); // reify the Bundle's contents so .toString() will format them
        Log.i(TAG, "WidgetOptionsChanged: " + newOptions);

        if (Build.VERSION.SDK_INT < 31) {
            updateWidget(context, appWidgetManager, appWidgetId, state);
        }
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
