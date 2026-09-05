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

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetProviderInfo;
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

import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.onefishtwo.bbqtimer.state.ApplicationState;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The BBQ Timer app widget for the home and lock screens.
 * <p>
 * BUG: A lock screen widget can fail to refresh on screen after tapping a button. The bug also
 * affects the Clock app's Stopwatch widget. Each opening of the lock screen widget ("communal")
 * screen, all taps on both apps work but ≈1/3 of the time they don't update the widget display.
 * Gemini, after a lot of debugging experiments: "This is definitively a SystemUI Keyguard
 * AppWidgetHostView draw/dispatch suppression bug."
 */
public class TimerAppWidgetProvider extends AppWidgetProvider {
    private static final String TAG = "AppWidgetProvider";

    private static final AtomicInteger requestCodeCounter =
            new AtomicInteger((int) SystemClock.uptimeMillis());

    // --- R.id.viewFlipper child indexes.
    private static final int PAUSED_CHRONOMETER_CHILD  = 0;
    private static final int RUNNING_CHRONOMETER_CHILD = 1;
    private static final int RESET_CHRONOMETER_CHILD   = 2;
    // + parallel "small" children.

    /** viewFlipper's Regular and Small banks of child res IDs [paused, Chronometer, reset]. */
    @IdRes private static final int[] CHILD_IDS = {
            R.id.pausedChronometerText,
            R.id.chronometer,
            R.id.resetChronometerText,
            R.id.smallPausedChronometerText,
            R.id.smallChronometer,
            R.id.smallResetChronometerText,
    };

    // Widget viewMapping sizes.
    private static final SizeF SIZE_SMALL = new SizeF(40, 40);
    private static final SizeF SIZE_MEDIUM = new SizeF(180, 40);
    private static final SizeF SIZE_LARGE = new SizeF(274, 40);

    // Actions for internal, explicit Intents. They should not be listed in an intent-filter.
    static final String ACTION_RUN_PAUSE  = "com.onefishtwo.bbqtimer.ACTION_RUN_PAUSE";
    static final String ACTION_RUN        = "com.onefishtwo.bbqtimer.ACTION_RUN";
    static final String ACTION_PAUSE      = "com.onefishtwo.bbqtimer.ACTION_PAUSE";
    static final String ACTION_RESET      = "com.onefishtwo.bbqtimer.ACTION_RESET";
    static final String ACTION_STOP       = "com.onefishtwo.bbqtimer.ACTION_STOP";
    static final String ACTION_CYCLE      = "com.onefishtwo.bbqtimer.ACTION_CYCLE";

    @androidx.annotation.VisibleForTesting
    public static final String PREFS_TESTING = "Testing_Prefs";
    @androidx.annotation.VisibleForTesting
    public static final String PREF_LAST_ACTION = "lastActionForTesting";

    /**
     * Saves the given Intent action (or null for none) in persistent storage so a test can check
     * that the Intent was received.
     * <p>
     * NOTE: This commits the storage update synchronously so it's reliably accessible to a test in
     * another process, but that delays the main thread and causes StrictMode disk I/O policy
     * violations, so don't call this for production Intents.
     */
    @androidx.annotation.VisibleForTesting
    @SuppressLint("ApplySharedPref")
    public static void saveActionForTesting(@NonNull Context context, @Nullable String action) {
        context.getSharedPreferences(PREFS_TESTING, Context.MODE_PRIVATE).edit()
                .putString(PREF_LAST_ACTION, action).commit();
    }

    /**
     * If the given Intent has FLAG_DEBUG_LOG_RESOLUTION, saves its action in persistent storage so
     * a test can check that the Intent was received. Only test code should set
     * FLAG_DEBUG_LOG_RESOLUTION, so ordinary production Intents won't cause StrictMode disk I/O
     * policy violations.
     * <p>
     * NOTE: This commits the storage update synchronously so the test can reliably retrieve it from
     * another process, but that delays the main thread, so don't call this for production Intents.
     */
    @androidx.annotation.VisibleForTesting
    public static void saveIntentActionForTesting(@NonNull Context context, @NonNull Intent intent) {
        int flags = intent.getFlags();

        if ((flags & Intent.FLAG_DEBUG_LOG_RESOLUTION) != 0) {
            saveActionForTesting(context, intent.getAction());
        }
    }

    @NonNull
    static ComponentName getComponentName(Context context) {
        return new ComponentName(context, TimerAppWidgetProvider.class);
    }

    /**
     * Updates the contents of all of this provider's app widgets, for the cases where the widgets
     * exist and the content changed, e.g. start/pause/stop. It does partial updates on lock screen
     * widgets for efficiency and to try to avoid the lock screen widget refresh bug; full updates
     * on home screen widgets to use responsive layouts.
     */
    static void updateAllWidgets(@NonNull Context context, @NonNull ApplicationState state) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        if (appWidgetManager == null) {
            return;
        }

        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(getComponentName(context));
        if (appWidgetIds == null || appWidgetIds.length == 0) {
            return;
        }

        if (Build.VERSION.SDK_INT >= 31) {
            // Update home screen widgets with a responsive RemoteViews instance, and lock screen
            // widgets with a partial update.
            RemoteViews responsiveViews = buildResponsiveRemoteViews(context, state, null);
            RemoteViews partialViews = buildPartialRemoteViews(context, state);

            for (int id : appWidgetIds) {
                Bundle options = appWidgetManager.getAppWidgetOptions(id);

                if (isLockScreenWidget(options)) {
                    // Log.d(TAG, "Partially updating LOCK_SCREEN widget id=" + id);
                    appWidgetManager.partiallyUpdateAppWidget(id, partialViews);
                } else {
                    // TODO: Accumulate an array of these IDs, then update them all at once for
                    //  speed, but it might rarely help.
                    // Log.d(TAG, "Fully updating HOME_SCREEN widget id=" + id);
                    appWidgetManager.updateAppWidget(id, responsiveViews);
                }
            }
        } else {
            // Update each widget with a custom size RemoteViews instance.
            updateWidgets(context, appWidgetManager, appWidgetIds, state);
        }
    }

    /**
     * Updates all the given widget instances' layout and contents.
     */
    private static void updateWidgets(@NonNull Context context,
            @NonNull AppWidgetManager appWidgetManager,
            int[] appWidgetIds, @NonNull ApplicationState state) {
        for (int id: appWidgetIds) {
            updateWidget(context, appWidgetManager, id, state);
        }
    }

    /**
     * Updates a single widget instance cleanly to a responsive or flat layout.
     * <p>
     * Updates a widget instance's layout to its size range, contents to the Timer state & time, and
     * sets its PendingIntents.
     * <p>
     * Workaround: A paused Chronometer doesn't show a stable value. Multiple widgets might show
     * different values, switching light/dark theme might change it, etc. So construct its time
     * text. Furthermore, a paused Chronometer ignores its format string, so flip to a TextView.
     * <p>
     * This code switches between each Chronometer and two alternate TextViews to select the right
     * ColorStateList for user feedback since RemoteViews.setColor() is only in API 31+.
     * <p>
     * A ViewFlipper is larger than its displayed child due to margins, the LinearLayout's size
     * adjustments, and maybe more factors. So make its child view and the Pause/Play button react
     * to taps and let the rest of the widget be the "background" where tapping opens the Activity.
     * <p>
     * Make the widget layout responsive to ever-smaller sizes by first hiding the countdown view,
     * then shrinking the count-up view (if viable), then hiding the count-up view. The "shrunken"
     * size is to fit in the narrow portrait mode width or landscape mode height.
     * <p>
     * API 31+ supports view mappings to switch between layouts without waking the app.
     * View mappings and layout managers react to ACTUAL SIZES. The layout size must be less than
     * the view size. The layout sizes in this mapping need not match the minWidth thresholds used
     * for API < 31.
     * <p>
     * For API < 31, the onAppWidgetOptionsChanged() hook responds when the user resizes a home
     * screen widget. It only receives the SIZE RANGE [minWidth x maxHeight] (portrait) TO
     * [maxWidth x minHeight] (landscape), and it rarely gets called when the screen rotates, so it
     * can only set a layout for the size range. Any finer responsiveness must be implemented by the
     * layout managers and density/size/orientation-specific resources.
     * <p>
     * Stop hidden Chronometers in case they'd use battery power.
     * <p>
     * NOTES:
     *  * autoSizeText (API 26+) doesn't work well. The text shrinks very small then won't grow
     *    back when the space grows.
     *  * Reducing the text size via rv.setTextViewTextSize() sticks even after constructing new
     *    RemoteViews and still has side effects after explicitly restoring the size. It confounds
     *    multiple widgets and the portrait/landscape sizes.
     *  * Word wrapping is ugly.
     *  * SingleLine is uglier, forcing ellipses that hide more digits.
     */
    private static void updateWidget(@NonNull Context context,
            @NonNull AppWidgetManager appWidgetManager,
            int appWidgetId, @NonNull ApplicationState state) {
        Bundle options = appWidgetManager.getAppWidgetOptions(appWidgetId);
        boolean isLockScreen = isLockScreenWidget(options);

        RemoteViews views = isLockScreen
                ? buildFlatRemoteViews(context, state, options)
                : buildResponsiveRemoteViews(context, state, options);

        // Always perform a full update on lifecycle/options changes.
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    /**
     * Builds a full, flat {@link RemoteViews} with all click handlers and dynamic states.
     * <p>
     * TODO: Use Kotlin's `apply { ... }` or `also { ... }` scoping functions for fluent builder
     * patterns when configuring RemoteViews, sharing more code with buildPartialRemoteViews().
     */
    @NonNull
    private static RemoteViews buildFlatRemoteViews(@NonNull Context context,
            @NonNull ApplicationState state, @Nullable Bundle options) {
        TimeCounter timer = state.getTimeCounter();
        long countUpBase = timer.getStartTime();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);

        PendingIntent runPauseIntent  = makeActionIntent(context, ACTION_RUN_PAUSE);
        PendingIntent cycleIntent     = makeActionIntent(context, ACTION_CYCLE);
        PendingIntent activityIntent  = MainActivity.makePendingIntent(context);

        int minWidth = options != null
                ? options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 180)
                : 180;

        int childIndex = getFlipperChildIndex(timer);
        int bankOffset = Build.VERSION.SDK_INT < 31 && minWidth >= 117 && minWidth < 184 ? 3 : 0;
        int extendedChildIndex = childIndex + bankOffset;
        @IdRes int extendedChildId = CHILD_IDS[extendedChildIndex];
        @DrawableRes int actionButton = childIndex == PAUSED_CHRONOMETER_CHILD
                ? R.drawable.ic_action_play
                : R.drawable.ic_action_pause;

        views.setChronometer(R.id.chronometer, 0, null, false);
        views.setChronometer(R.id.smallChronometer, 0, null, false);
        views.setChronometer(R.id.countdownChronometer, 0, null, false);
        views.setChronometerCountDown(R.id.countdownChronometer, true);

        if (state.isEnableReminders()) {
            views.setViewVisibility(R.id.countdownFlipper, View.VISIBLE);
            views.setDisplayedChild(R.id.countdownFlipper, childIndex);
        } else {
            views.setViewVisibility(R.id.countdownFlipper, View.GONE);
        }

        if (timer.isRunning()) {
            views.setChronometer(extendedChildId, countUpBase, null, true);

            if (state.isEnableReminders()) {
                long rt = SystemClock.elapsedRealtime();
                long countdownToNextAlarm = state.getMillisecondsToNextAlarm();
                long countdownBase = rt + countdownToNextAlarm;

                views.setChronometer(R.id.countdownChronometer, countdownBase, null, true);
            }
        } else {
            views.setTextViewText(extendedChildId, timer.formatHhMmSs());

            if (state.isEnableReminders()) {
                @IdRes int countdownTextViewId = childIndex == RESET_CHRONOMETER_CHILD
                        ? R.id.countdownResetChronometerText
                        : R.id.countdownPausedChronometerText;
                long countdownToNextAlarm = state.getMillisecondsToNextAlarm();

                views.setTextViewText(countdownTextViewId,
                        TimeCounter.formatHhMmSs(countdownToNextAlarm));
            }
        }

        views.setImageViewResource(R.id.remoteStartStopButton, actionButton);
        views.setDisplayedChild(R.id.viewFlipper, extendedChildIndex);

        setOnClickHandler(views, R.id.remoteStartStopButton, runPauseIntent);
        setOnClickHandler(views, android.R.id.background, activityIntent);
        for (int i = 0; i < 3; ++i) {
            setOnClickHandler(views, CHILD_IDS[bankOffset + i], cycleIntent);
        }

        if (Build.VERSION.SDK_INT < 31) {
            // ≥ 5 cells on API 29 Nexus 5, else ≥ 4 cells ==> Show all views.
            if (minWidth < 274) { hideTheCountdown(views); } // < 5 cells, else < 4 cells
            if (minWidth < 117) { hideTheCountUp(views); } // < 3 cells, else < 2 cells
        }

        return views;
    }

    /** Builds a responsive RemoteViews (using viewMapping on API 31+) for Home Screen widgets. */
    @NonNull
    private static RemoteViews buildResponsiveRemoteViews(@NonNull Context context,
            @NonNull ApplicationState state, @Nullable Bundle options) {
        RemoteViews views = buildFlatRemoteViews(context, state, options);

        // Define responsive widget layouts by hiding the countdown view, then shrinking the
        // count-up view (if viable), then hiding the count-up view.
        if (Build.VERSION.SDK_INT >= 31) {
            RemoteViews mediumViews = new RemoteViews(views);
            hideTheCountdown(mediumViews);

            RemoteViews smallViews = new RemoteViews(mediumViews);
            hideTheCountUp(smallViews);

            Map<SizeF, RemoteViews> viewMapping = new ArrayMap<>(3);
            viewMapping.put(SIZE_SMALL, smallViews);
            viewMapping.put(SIZE_MEDIUM, mediumViews);
            viewMapping.put(SIZE_LARGE, views);

            views = new RemoteViews(viewMapping);
        }

        return views;
    }

    /**
     * Builds a flat {@link RemoteViews} with only dynamic property changes for
     * {@link AppWidgetManager#partiallyUpdateAppWidget(int, RemoteViews)}.
     */
    @NonNull
    @RequiresApi(31)
    private static RemoteViews buildPartialRemoteViews(@NonNull Context context,
            @NonNull ApplicationState state) {
        TimeCounter timer = state.getTimeCounter();
        long countUpBase = timer.getStartTime();

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.app_widget);

        int childIndex = getFlipperChildIndex(timer);
        @IdRes int extendedChildId = CHILD_IDS[childIndex];
        @DrawableRes int actionButton = childIndex == PAUSED_CHRONOMETER_CHILD
                ? R.drawable.ic_action_play
                : R.drawable.ic_action_pause;

        views.setChronometer(R.id.countdownChronometer, 0, null, false);

        if (state.isEnableReminders()) {
            views.setViewVisibility(R.id.countdownFlipper, View.VISIBLE);
            views.setDisplayedChild(R.id.countdownFlipper, childIndex);
        } else {
            views.setViewVisibility(R.id.countdownFlipper, View.GONE);
        }

        if (timer.isRunning()) {
            views.setChronometer(R.id.chronometer, countUpBase, null, true);

            if (state.isEnableReminders()) {
                long rt = SystemClock.elapsedRealtime();
                long countdownToNextAlarm = state.getMillisecondsToNextAlarm();
                long countdownBase = rt + countdownToNextAlarm;

                views.setChronometer(R.id.countdownChronometer, countdownBase, null, true);
            }
        } else {
            views.setChronometer(R.id.chronometer, 0, null, false);
            views.setTextViewText(extendedChildId, timer.formatHhMmSs());

            if (state.isEnableReminders()) {
                @IdRes int countdownTextViewId = childIndex == RESET_CHRONOMETER_CHILD
                        ? R.id.countdownResetChronometerText
                        : R.id.countdownPausedChronometerText;
                long countdownToNextAlarm = state.getMillisecondsToNextAlarm();

                views.setTextViewText(countdownTextViewId,
                        TimeCounter.formatHhMmSs(countdownToNextAlarm));
            }
        }

        views.setImageViewResource(R.id.remoteStartStopButton, actionButton);
        views.setDisplayedChild(R.id.viewFlipper, childIndex);

        return views;
    }

    /**
     * Returns R.id.viewFlipper's childIndex per run/stopped/paused timer. Smallish widgets on
     * API < 31 use a second bank (see extendedChildIndex) to select smaller text.
     */
    private static int getFlipperChildIndex(TimeCounter timer) {
        return switch (timer.getState()) {
            case RUNNING -> RUNNING_CHRONOMETER_CHILD;
            case STOPPED -> RESET_CHRONOMETER_CHILD;
            default -> PAUSED_CHRONOMETER_CHILD;
        };
    }

    private static void setOnClickHandler(@NonNull RemoteViews views, @IdRes int viewId,
                                          @Nullable PendingIntent pendingIntent) {
        if (pendingIntent != null) {
            if (Build.VERSION.SDK_INT >= 31) {
                // Use the newer RemoteResponse API for better feedback and transitions.
                views.setOnClickResponse(viewId,
                        RemoteViews.RemoteResponse.fromPendingIntent(pendingIntent));
            } else {
                views.setOnClickPendingIntent(viewId, pendingIntent);
            }
        }
    }

    /** Hide the countdown from the remote views for a smaller layout. */
    // Unfortunately, the RemoteViews copy constructor requires API 28+.
    private static void hideTheCountdown(@NonNull RemoteViews rv) {
        rv.setViewVisibility(R.id.countdownFlipper, View.GONE);
        rv.setChronometer(R.id.countdownChronometer, 0, null, false);
    }

    /** Hide the count-up from the remote views for a smaller layout. */
    // Workaround: When hiding the count-up view, just making the ViewFlipper GONE/INVISIBLE can get
    // it stuck hidden (at least on Android 31 on Nexus 7, until rotating the screen), so show a
    // full-size but empty TextView in the ViewFlipper. (Alternative: Show a GONE TextView inside
    // the ViewFlipper and an empty TextView outside it.)
    private static void hideTheCountUp(@NonNull RemoteViews rv) {
        rv.setTextViewText(R.id.pausedChronometerText, "");
        rv.setDisplayedChild(R.id.viewFlipper, PAUSED_CHRONOMETER_CHILD);
        rv.setChronometer(R.id.chronometer, 0, null, false);
        rv.setChronometer(R.id.smallChronometer, 0, null, false);
    }

    /**
     * Updates the given app widgets' contents. Called when an app widget instance was added to a
     * widget host (e.g. home screen) (including after onRestored(), when widgets get restored from
     * backup) and periodically at updatePeriodMillis (if it's not 0; every 30 minutes or longer).
     */
    @Override
    public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        ApplicationState state = ApplicationState.sharedInstance(context);

        updateWidgets(context, appWidgetManager, appWidgetIds, state);
    }

    /**
     * Returns true if the given widget is definitely a lock screen widget; false if not, e.g. if
     * options == null.
     */
    private static boolean isLockScreenWidget(@Nullable Bundle options) {
        return options != null
                && options.getInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, -1)
                == AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD;
    }

    /**
     * Responds to a single widget instance initial placement, resized by the user, orientation
     * change, or re-binding. If this gets called, adjust the layout, even on API >= 31 where the
     * viewMapping should handle home screen widget resizing without waking the app.
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
        Log.d(TAG, "Widget[" + appWidgetId + "] newOptions: " + newOptions);

        updateWidget(context, appWidgetManager, appWidgetId, state);
    }

    @NonNull
    static PendingIntent makeActionIntent(Context context, String action) {
        return makeActionIntent(context, action, false);
    }

    /**
     * Constructs a PendingIntent for the widget to send an action event to this Receiver.
     * <p>
     * @param useWorkaround If true, use a unique requestCode value to make the PendingIntent !=
     *                      previous Intents, to work around a bug on Wear OS 4.0, Android 13.0
     *                      Tiramisu API 33 where notifications don't update the smartwatch. That
     *                      bug does not appear on Wear OS 6.0, Android 16.0 Baklava API 36.0.
     *                      If false, use a stable requestCode (action hashCode) for stability on
     *                      the Lock Screen. This gives the OS fewer PendingIntents to manage than
     *                      the unique requestCode, hoping to reduce the missed widget updates.
     * <p>
     * Test case: In the Watch display of a BBQ Timer notification, tap Pause, Reset, then Run. The
     * Pause -> Reset -> Run sequence fails to deliver the second ACTION_RUN Intent to
     * TimerAppWidgetProvider. Then onReceive() doesn't log an Intent or start the timer. But
     * repeated taps on Pause and Run work fine, and the Reset and Stop buttons work fine.
     * <p>
     * Unsuccessful workarounds: Change the Intent's action, add Intent flags or extras, reorder or
     * rename the action buttons in the Wear notification, ...
     * <p>
     * NOTE: After installing Pixel Watch app on a phone, go into Settings and turn on that app's
     * ability to send notifications and its special app access permission for notification access.
     */
    @NonNull
    static PendingIntent makeActionIntent(Context context, String action, boolean useWorkaround) {
        Intent intent = new Intent(context, TimerAppWidgetProvider.class);
        int requestCode = useWorkaround ? requestCodeCounter.incrementAndGet() : action.hashCode();

        intent.setAction(action).addFlags(Intent.FLAG_RECEIVER_FOREGROUND);
        return PendingIntent.getBroadcast(context, requestCode, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * Receives an Intent from an AppWidget or a Notification. Dispatches via super.onReceive() to
     * onUpdate(), onDeleted(), onEnabled(), or onDisabled(); and handles app-specific actions.
     */
    @Override
    public void onReceive(@NonNull Context context, @NonNull Intent intent) {
        String action          = intent.getAction();

        Log.i(TAG, "Intent: " + action);
        saveIntentActionForTesting(context, intent);

        super.onReceive(context, intent);

        if (action == null) {
            return;
        }

        ApplicationState state = ApplicationState.sharedInstance(context);
        TimeCounter timer      = state.getTimeCounter();

        switch (action) {
            case ACTION_RUN_PAUSE -> timer.togglePauseRun();
            case ACTION_RUN       -> timer.start();
            case ACTION_PAUSE     -> timer.pause();
            case ACTION_RESET     -> timer.reset();
            case ACTION_STOP      -> timer.stop(); // Stop button or swiped the notification
            case ACTION_CYCLE     -> timer.cycle(); // tapped the time text
            default               -> { return; }
        }

        saveStateAndUpdateUI(context, state);
    }

    /** Saves app state then updates the Notifications and Widgets. */
    private void saveStateAndUpdateUI(@NonNull Context context, @NonNull ApplicationState state) {
        state.save(context);

        AlarmReceiver.updateNotifications(context);
        updateAllWidgets(context, state);
    }
}
