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

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.text.Spanned;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.WearableExtender;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.wearable.Wearable;
import com.onefishtwo.bbqtimer.state.ApplicationState;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the app's Android Notifications. One-shot use to build and show a notification.
 */
public class Notifier {
    private static final String TAG = "Notifier";
    private static final int NOTIFICATION_ID = 7;

    /**
     * Whether a Wearable device (like a SmartWatch) is connected, used to conditionally modify
     * notifications to aid Wearables.
     */
    private static final AtomicBoolean lastKnownWatchConnected = new AtomicBoolean(false);

    // Vibration pattern: ms off, on, off, ...
    // Match the notification sound to the degree feasible.
    // Workaround: Start with a tiny pulse for when Android drops the initial "off" interval.
    private static final long[] VIBRATE_PATTERN = { 0, 1, 280, 40, 220, 80, 440, 45, 265, 55 };

    static final String ALARM_NOTIFICATION_CHANNEL_ID = "alarmChannel";
    // Channel IDs in previous releases will remain immutable on devices while the app is installed:
    // "controlsChannel"

    // --- R.id.countUpViewFlipper and R.id.countdownViewFlipper child indexes.
    private static final int RUNNING_FLIPPER_CHILD = 0;
    private static final int PAUSED_FLIPPER_CHILD = 1;
    public static final int REMINDER_STREAM = AudioManager.STREAM_ALARM;

    /** A listener for UI notifications posted, for testing. */
    @SuppressWarnings("unused")
    public interface NotificationListener {
        void onNotificationPosted();
    }

    @Nullable
    private static NotificationListener notificationListener = null;

    private static volatile boolean builtNotificationChannels = false;

    private static volatile boolean checkedForGooglePlayServices = false;
    private static volatile boolean hasWearOSCompanionApp = false;

    @NonNull
    private final Context context;
    @NonNull
    private final NotificationManager notificationManager;
    @NonNull
    private final NotificationManagerCompat notificationManagerCompat;
    private final int notificationLightColor;

    private boolean soundAlarm = false; // whether the next notification should sound an alarm

    /**
     * Sets a listener for when the Notifier posts UI notifications, for testing.
     */
    @SuppressWarnings("unused")
    @RestrictTo(RestrictTo.Scope.TESTS)
    public static void setNotificationListener(@Nullable NotificationListener listener) {
        notificationListener = listener;
    }

    public Notifier(@NonNull Context _context) {
        this.context = _context;
        notificationManager = (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManagerCompat = NotificationManagerCompat.from(_context);
        notificationLightColor = ContextCompat.getColor(_context, R.color.notification_light_color);
    }

    /**
     * Builder-style setter: Whether to make the next notification sound an alarm, vibrate, and
     * flash the device LED. Initially set to false.
     */
    @NonNull
    public Notifier setAlarm(boolean _soundAlarm) {
        this.soundAlarm = _soundAlarm;
        return this;
    }

    /** @noinspection SameParameterValue */
    @NonNull
    private Uri getSoundUri(@RawRes int soundId) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + soundId);
    }

    /** Constructs a PendingIntent to use as a Notification Action. */
    @NonNull
    private PendingIntent makeActionIntent(String action) {
        return TimerAppWidgetProvider.makeActionIntent(context, action, true);
    }

    /**
     * Adds an action button to the given WearableExtender.
     * Gemini says the Wearable's notification bridge might choose the action's title over its icon.
     * Why isn't that documented?
     * </p>
     * TODO: Experiment with setAvailableOffline().
     */
    private void addAction(@NonNull WearableExtender extender, @DrawableRes int iconId,
            @StringRes int titleId, PendingIntent intent) {
        NotificationCompat.Action action =
                new NotificationCompat.Action.Builder(iconId, context.getString(titleId), intent)
                .extend(new NotificationCompat.Action.WearableExtender().setAvailableOffline(true))
                .build();
        extender.addAction(action);
    }

    /**
     * Adds an action button to the given WearableExtender to invoke a named
     * TimerAppWidgetProvider.ACTION_*.
     */
    private void addAction(@NonNull WearableExtender extender, @DrawableRes int iconId,
            @StringRes int titleId, @NonNull String action) {
        PendingIntent intent = makeActionIntent(action);
        addAction(extender, iconId, titleId, intent);
    }

    /**
     * Sets the notification style to DecoratedCustomViewStyle, which wraps a custom view with
     * standard chrome (actions, icon).
     * Calls builder.setColor() on some versions of Android to cope with OS vagaries.
     * <p/>
     * NOTE: DecoratedMediaCustomViewStyle was nice (showing actions in the compact view) but it
     * blocks the bridge to Wear OS, with or without a MediaSession.
     */
    private void setNotificationStyle(@NonNull NotificationCompat.Builder builder) {
        builder.setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        // === MediaStyle setColor() [the "accent color"] vs. Android API levels ===
        // [Irrelevant with DecoratedCustomViewStyle?]
        // API 21 L - 22 L1: colors the notification area background needlessly. By default,
        //   * Heads-up notifications: Medium gray text on light white background.
        //   * Pull-down notifications: Light white text on dark gray background.
        // API 23 M: colors the notification area background; needed to work around low contrast:
        //   * Heads-up notifications: Medium gray text.
        //   * Pull-down notifications: Light white text on the SAME background.
        //   The color setting carries over from a notification to its replacement so there's no
        //   way to get consistently different background colors for the two cases.
        //   http://stackoverflow.com/q/38415467/1682419
        // [minSdk obviates the above adaptations.]
        // API 24 N - API 27 O1: colors the small icon, action button, and app title color. Garish.
        // API 28 P - API 30 R: colors the small icon and action button. Garish.
        // API 31 S+: colors the small icon's circular background.
        // setColorized(false) didn't change any of these results.
        //
        // TODO: Retest setColorized(true) with DecoratedCustomViewStyle. It might noop unless it's
        //  a foreground service notification. For a high priority ongoing task's notification, it
        //  might affect the notification's ranking. Ensure the XML backgrounds are transparent.
        //  Because setColorized() forces a dark or highly saturated tint, standard black or white
        //  text may be unreadable. In the custom XML, use the compatible text appearances so text
        //  contrast automatically flips based on the background:
        //  @style/TextAppearance.Compat.Notification.Title,
        //  @style/TextAppearance.Compat.Notification (for body & content text).
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @ColorInt
            int iconBackgroundColor = ContextCompat.getColor(context, R.color.dark_orange_red);
            builder.setColor(iconBackgroundColor);
        }
    }

    /**
     * Returns a localized description of the timer's run state: "Running", "Paused", or "Stopped".
     */
    @NonNull
    String timerRunState(@NonNull TimeCounter timer) {
        return switch (timer.getState()) {
            case RUNNING -> context.getString(R.string.timer_running);
            case PAUSED -> context.getString(R.string.timer_paused);
            default -> context.getString(R.string.timer_stopped);
        };
    }

    /**
     * Returns a localized description of the timer's run state with the current time value for
     * Wearable notifications. Deal with the lack of Chronometers and Images:
     * "Running 00:15➚︎"|"Running ≥00:15"|"Running >00:15", "Paused 00:15.1", "Stopped".
     * The arrow conveys that the value is going up from the snapshot number.
     * <p>
     * Gemini: "Emojis like ⏱️ (Stopwatch), ⏲️ (Timer Clock), and 🔔 (Bell) are widely supported
     * across all Wear OS versions."
     */
    @NonNull
    String timerRunStateValue(@NonNull TimeCounter timer) {
        return switch (timer.getState()) {
            case RUNNING -> {
                String timerHhMmSs = timer.formatHhMmSs();
                yield context.getString(R.string.running_up_at, timerHhMmSs);
            }
            case PAUSED -> {
                Spanned pauseTime = timer.formatHhMmSsFraction();
                yield context.getString(R.string.timer_paused_at, pauseTime);
            }
            default -> context.getString(R.string.timer_stopped);
        };
    }

    /**
     * Returns a localized description of the time until the next alarm for Wearable notifications
     * which don't support Chronometers or Images:
     * "Next ♫ in 00:15➘"|"Next ♫ in ≤00:15"|"Next ♫ in <00:15".
     * The arrow is visible if the timer is running to convey that the value is going down from the
     * snapshot number. The ♫ stands for "alarm", fits on a smartwatch, and doesn't have distracting
     * emoji colors.
     */
    @NonNull
    String nextAlarmValue(@NonNull ApplicationState state) {
        if (state.isEnableReminders()) {
            @NonNull
            TimeCounter timer = state.getTimeCounter();
            long countdownToNextAlarm = state.getMillisecondsToNextAlarm();
            String countDownHhMmSs = TimeCounter.formatHhMmSs(countdownToNextAlarm);

            return context.getString(
                    timer.isPaused() ? R.string.count_down_at : R.string.counting_down_at,
                    countDownHhMmSs);
        }

        return "";
    }

    /**
     * Returns a localized description of the periodic alarms.
     *
     * @param state the ApplicationState.
     * @return a localized string like "Every 2 minutes" or "No periodic alarms".
     */
    @NonNull
    String describePeriodicAlarms(@NonNull ApplicationState state) {
        // Synthesize a "quantity" to select the right pluralization rule.
        String intervalMmSs = state.formatIntervalTimeHhMmSs();

        return state.isEnableReminders()
                ? context.getString(R.string.notification_body, intervalMmSs)
                : context.getString(R.string.no_reminders_tip);
    }

    /**
     * <em>(Re)Opens</em> this app's notification with content depending on {@code state} and
     * {@link #setAlarm(boolean)},
     * <em>or cancels</em> the app's notification if there's nothing to show or sound.
     *
     * @param state the ApplicationState state to display.
     */
    public void openOrCancel(@NonNull ApplicationState state) {
        buildAndNotify(state);

        // Check and update for wearables.
        updateWatchConnectedAsync(state);
    }

    /**
     * Builds and posts or cancels the notification.
     */
    private void buildAndNotify(@NonNull ApplicationState state) {
        TimeCounter timer = state.getTimeCounter();

        if (!timer.isStopped() || soundAlarm) {
            Notification notification = buildNotification(state);
            notify(NOTIFICATION_ID, notification);
        } else {
            cancelAll();
        }
    }

    /**
     * Asynchronously checks if any Wearable devices are connected, updates
     * {@link #lastKnownWatchConnected}, and updates the current notification <u>unless</u> that
     * would interrupt an alarm sound (and replace a PRIORITY_MAX heads-up notification with a
     * PRIORITY_LOW notification).
     * This gets called when opening or canceling a notification, including Activity onStart() and
     * BOOT_COMPLETED.
     */
    private void updateWatchConnectedAsync(@NonNull ApplicationState state) {
        // Fast, synchronous check for Google APIs and Google Play Services.
        // Just once, unless the service is currently updating.
        if (!checkedForGooglePlayServices) {
            GoogleApiAvailability availability = GoogleApiAvailability.getInstance();
            int resultCode = availability.isGooglePlayServicesAvailable(context);
            // ^^^ Logs "W  com.onefishtwo.bbqtimer requires the Google Play Store, but it is missing."

            // SUCCESS (0): OK, but maybe a stub implementation on GrapheneOS, MicroG, or missing
            //   Wear OS companion app. getConnectedNodes() might log
            //   `Wearable.API is not available on this device`.
            // SERVICE_MISSING (1), SERVICE_VERSION_UPDATE_REQUIRED (2): The device supports
            //   Google APIs but needs Play Store or an update, e.g. on Google APIs System Image,
            //   GrapheneOS, MicroG.
            // SERVICE_INVALID (9): Google Play Services cannot run, e.g. on AOSP OS.
            //   getConnectedNodes() will fail with scary logs.
            Log.i(TAG, "isGooglePlayServicesAvailable: "
                    + (resultCode == ConnectionResult.SUCCESS ? "SUCCESS" : resultCode));
            hasWearOSCompanionApp = resultCode == ConnectionResult.SUCCESS; // maybe...
            checkedForGooglePlayServices = resultCode != ConnectionResult.SERVICE_UPDATING;
        }

        if (!hasWearOSCompanionApp) {
            return;
        }

        Wearable.getNodeClient(context).getConnectedNodes()
                .addOnSuccessListener(nodes -> {
                    int numberOfNodes = nodes == null ? 0 : nodes.size();
                    boolean connected = numberOfNodes > 0;

                    if (lastKnownWatchConnected.getAndSet(connected) != connected) {
                        Log.i(TAG, "WearOS connected devices: " + numberOfNodes);
                        if (!soundAlarm) {
                            buildAndNotify(state);
                        }
                    }
                })
                .addOnFailureListener(exception -> {
                    hasWearOSCompanionApp = false; // needs the Wear OS companion app
                    Log.i(TAG, "Couldn't count connected Wearables: " + exception);
                });
    }

    /**
     * Posts a notification to the system and the {@link #notificationListener} if any.
     *
     * @param id           the notification ID.
     * @param notification the notification to post.
     */
    private void notify(@SuppressWarnings("SameParameterValue") int id, Notification notification) {
        try {
            notificationManagerCompat.notify(id, notification);
            if (notificationListener != null) {
                notificationListener.onNotificationPosted();
            }
        } catch (SecurityException e) { // ≈API 33+: The app should've requested permission already.
            Log.e(TAG, "Need POST_NOTIFICATIONS permission", e);
        }
    }

    /**
     * Returns true if the Alarm channel is configured On with enough Importance to hear alarms.
     * After createNotificationChannelV26() creates the channels, the user can reconfigure them and
     * the app can only set their names and descriptions. If users configure the Alarm channel to be
     * silent but request Periodic Alarms, they'll think the app is broken.
     */
    boolean isAlarmChannelOK() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = notificationManager.getNotificationChannel(
                    ALARM_NOTIFICATION_CHANNEL_ID);

            if (channel == null) { // The channel hasn't been created so it can't be broken.
                return true;
            }

            int importance = channel.getImportance();
            return importance >= NotificationManager.IMPORTANCE_DEFAULT;
        }

        return true;
    }

    /**
     * Creates a notification channel that matches what #buildNotification() needs. The "Alarm"
     * channel sounds an alarm at High importance for a
     * <a href="https://developer.android.com/develop/ui/views/notifications#Heads-up">Heads-up
     * notification</a> although the user can adjust the channel's importance to not "Pop on screen"
     * and make it silent or play a different ringtone.
     * API < 26 doesn't have notification channels; heads-up pop on screen is triggered by
     * high priority and a ringtone or vibration.
     */
    @RequiresApi(26)
    private void createNotificationChannelV26() {
        String name = context.getString(R.string.notification_alarm_channel_name);
        String description = context.getString(R.string.notification_alarm_channel_description);
        NotificationChannel channel = new NotificationChannel(
                ALARM_NOTIFICATION_CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);

        channel.setDescription(description);

        channel.enableLights(true);
        channel.setLightColor(notificationLightColor);

        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

        channel.enableVibration(true);
        channel.setVibrationPattern(VIBRATE_PATTERN);

        {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    // .setLegacyStreamType(REMINDER_STREAM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    // .setFlags(): Nope. See https://stackoverflow.com/q/44855786 and
                    // https://issuetracker.google.com/issues/37033371
                    .build();
            channel.setSound(getSoundUri(R.raw.cowbell4), audioAttributes);
        }

        channel.setShowBadge(false);

        // Did not setBypassDnd(), setGroup().

        notificationManager.createNotificationChannel(channel); // goofy naming
    }

    /**
     * Creates notification channel(s) lazily (or updates the text) on Android O+.
     */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < 26 || builtNotificationChannels) {
            return;
        }

        createNotificationChannelV26();
        builtNotificationChannels = true;
    }

    /**
     * Update the notification channel (not the notification) for a UI Locale change.
     */
    void onLocaleChange() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = notificationManager.getNotificationChannel(
                    ALARM_NOTIFICATION_CHANNEL_ID);

            // noinspection VariableNotUsedInsideIf
            if (channel != null) {
                builtNotificationChannels = false;
                createNotificationChannels();
            }
        }
    }

    /**
     * Make and initialize RemoteViews for a Custom Notification.
     * <p/>
     * Workaround: A paused Chronometer doesn't show a stable value, e.g. switching light/dark theme
     * can change it, and it ignores its format string thus ruling out some workarounds.
     * *SO* when the timer is paused, flip to a TextView.
     * <p/>
     * TODO: Switch the action buttons to higher contrast R.color.notification_text if
     *  isHighContrastEnabled(context)?
     *  Use ContextCompat.getColor(context, R.color.md_theme_tertiary) which responds to day/night
     *  themes and might respond to the high contrast setting?
     *      int highColor = ContextCompat.getColor(context, R.color.notification_text);
     *      remoteViews.setInt(R.id.btnStart, "setColorFilter", primaryColor);
     *
     * @param layoutId          the layout resource ID for the RemoteViews.
     * @param state             the ApplicationState to show.
     * @param timerStateMessage the Running/Paused/Stopped state message.
     * @return RemoteViews
     */
    @NonNull
    private RemoteViews makeRemoteViews(
            @LayoutRes int layoutId, @NonNull ApplicationState state,
            @NonNull CharSequence timerStateMessage) {
        TimeCounter timer = state.getTimeCounter();
        long elapsedTime = timer.getElapsedTime();
        boolean isRunning = timer.isRunning();
        long rt = SystemClock.elapsedRealtime();
        long countUpBase = rt - elapsedTime;
        @IdRes
        int childId = isRunning ? RUNNING_FLIPPER_CHILD : PAUSED_FLIPPER_CHILD;
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), layoutId);

        // Count-up time and status
        if (!isRunning) {
            remoteViews.setTextViewText(R.id.pausedCountUp, timer.formatHhMmSs());
        }
        remoteViews.setDisplayedChild(R.id.countUpViewFlipper, childId);
        remoteViews.setChronometer(
                R.id.countUpChronometer, countUpBase, null, isRunning);
        remoteViews.setTextViewText(R.id.timerStateMessage, timerStateMessage);

        // Count-down time and status
        if (state.isEnableReminders()) {
            long countdownToNextAlarm = state.getMillisecondsToNextAlarm();
            long countdownBase = rt + countdownToNextAlarm;

            if (!isRunning) {
                remoteViews.setTextViewText(
                        R.id.pausedCountdown, TimeCounter.formatHhMmSs(countdownToNextAlarm));
            }
            remoteViews.setDisplayedChild(R.id.countdownViewFlipper, childId);
            remoteViews.setChronometer(
                    R.id.countdownChronometer, countdownBase, null, isRunning);
        } else {
            remoteViews.setChronometer(
                    R.id.countdownChronometer, 0, null, false);
            remoteViews.setViewVisibility(R.id.alarmIcon, View.GONE);
            remoteViews.setViewVisibility(R.id.countdownViewFlipper, View.GONE);
        }

        // Configure custom buttons logic
        // TODO: Refactor to share code with WearableExtender addAction().
        PendingIntent resetIntent = makeActionIntent(TimerAppWidgetProvider.ACTION_RESET);
        remoteViews.setOnClickPendingIntent(R.id.btnReset, resetIntent);
        remoteViews.setViewVisibility(R.id.btnReset,
                timer.isPaused() && !timer.isPausedAt0() ? View.VISIBLE : View.INVISIBLE);

        PendingIntent runPauseIntent = makeActionIntent(TimerAppWidgetProvider.ACTION_RUN_PAUSE);
        remoteViews.setOnClickPendingIntent(R.id.btnStart, runPauseIntent);
        remoteViews.setOnClickPendingIntent(R.id.btnPause, runPauseIntent);
        remoteViews.setViewVisibility(R.id.btnStart, isRunning ? View.GONE : View.VISIBLE);
        remoteViews.setViewVisibility(R.id.btnPause, timer.isPaused() ? View.GONE : View.VISIBLE);

        PendingIntent stopIntent = makeActionIntent(TimerAppWidgetProvider.ACTION_STOP);
        remoteViews.setOnClickPendingIntent(R.id.btnStop, stopIntent);
        remoteViews.setViewVisibility(R.id.btnStop, timer.isStopped() ? View.INVISIBLE : View.VISIBLE);

        return remoteViews;
    }

    /**
     * Builds a notification that also gets bridged to Wearable smartwatches. (So in the Watch App,
     * setting "Notifications > Mute notifications on phone" won't totally mute this app's
     * notifications.) The notification alarm sound, vibration, and LED light flashing are switched
     * on/off by {@link #setAlarm(boolean)}.
     * <p>
     * REQUIRES: Use NotificationManagerCompat to notify() the returned Notification.
     * <p>
     * Hypotheses on why bridging was failing:
     * - it must not setOngoing(true) -- yes,
     * - it must not set Notification.FLAG_ONGOING_EVENT -- ?,
     * - it has to be from a Foreground service -- no,
     * - it must not be CATEGORY_ALARM -- no,
     * - it must not be silent -- ?,
     * - it must not use a custom app sound resource -- no,
     * - it must have IMPORTANCE_HIGH -- ?,
     * - it must not be MediaStyle, with or without a MediaSession -- **YES**,
     * - it must explicitly setLocalOnly(false) -- ?,
     * - it must not setLocalOnly(true) -- presumably?,
     * - it must not set Notification.FLAG_NO_CLEAR (non-clearable notification) -- ?
     * - it must not have custom remote views -- no,
     * - the channel ID ("alarmChannel") must not heuristically match the string "alarm" -- no,
     * - it must not have Chronometers in the notification RemoteViews -- no,
     * - it can use USAGE_NOTIFICATION_EVENT, not AudioAttributes.USAGE_ALARM -- no,
     * - the Wearable app must have the app's (or all apps') notifications enabled -- ?
     * <p/>
     * TODO: Add a Wear OS app to show full Chronometers and play a custom sound.
     */
    @NonNull
    protected Notification buildNotification(@NonNull ApplicationState state) {
        createNotificationChannels();

        WearableExtender wearableExtender = new WearableExtender().clearActions();
        int pri = soundAlarm ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_LOW;
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, ALARM_NOTIFICATION_CHANNEL_ID)
                .setPriority(pri) // for API < 26
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(false) // just in case
                .setOnlyAlertOnce(!soundAlarm); // so running -> paused and Activity#onStart() won't ding a wearable

        { // Construct the visible notification contents.
            TimeCounter timer = state.getTimeCounter();
            boolean isRunning = timer.isRunning();
            String alarms = describePeriodicAlarms(state);

            builder.setSmallIcon(R.drawable.notification_icon);

            // Show the top line "native" notification chronometer if a SmartWatch is connected.
            // It's redundant with the phone's RemoteViews content, but it makes the SmartWatch
            // notification's chronometer show the correct running elapsed timer instead of time
            // since the last notify().
            //
            // The native chronometer crowds the "Every 00:15" subtext. On a modern phone
            // screen the crowding matters if the Display Size or Font Size is larger than default.
            // Changing the subtext to "Every 00:15" reduced the crowding.
            //
            // The phone can display minutes "1m" since notify() [setShowWhen(true)], a native
            // chronometer "01:10" [setShowWhen(true).setUsesChronometer(true)], or no time
            // [setShowWhen(false)].
            //
            // The SmartWatch always displays a time, either a given chronometer or time since
            // notify(). It doesn't look like a chronometer since it truncates to whole minutes like
            // "now", "1m", "2m", ... and it won't update while visible.
            if (lastKnownWatchConnected.get() && isRunning) {
                long elapsedTime = timer.getElapsedTime();
                long countUpBase = System.currentTimeMillis() - elapsedTime; // NOT elapsedRealtime()

                builder.setWhen(countUpBase).setUsesChronometer(true).setShowWhen(true);
            } else {
                builder.setShowWhen(false);
            }

            // Phone collapsed: "🔔 00:07  ⏱ 00:08  Running / [buttons]"
            // Phone expanded: "BBQ Timer • Every 00:15 / 🔔 00:07 / ⏱ 00:08  Running / [buttons]"
            //                 AppName • SubText / RemoteViews
            //
            // Wearable popup:     "Running >00:15"
            // Wearable collapsed: "BBQ Timer / Running >00:15 / Next ♫ in ≤00:30"
            // Wearable expanded:  "BBQ Timer / Every 00:15 / Running >00:15 / Next ♫ in ≤00:30"
            //                     AppName / SubText / ContentText / ContentTitle
            //
            // Note: ⏱ stopwatch character might render as a simple circular outline. ⏲ is like a
            // kitchen timer. Emoji clocks 🕐 1:00, 🕒 3:00, … every hour and half-hour
            // (U+1F550 through U+1F567). Emojis are supposed to be forceable into monochrome by
            // appending the Variation Selector-15 (U+FE0E) immediately after the character, but it
            // doesn't work on WearOS. I didn't test Android since the notification area supports
            // image views.
            builder.setSubText(alarms) // small text, "Every 00:15"
                    .setContentTitle(timerRunStateValue(timer)) // bold colored text, "Running >00:15"
                    .setContentText(nextAlarmValue(state)); // plain text, "Next ♫ in <00:30"

            {
                String timerStateMessage = timerRunState(timer); // Running/Paused/Stopped
                RemoteViews compactNotificationView = makeRemoteViews(
                        R.layout.custom_notification, state, timerStateMessage);
                RemoteViews tallNotificationView = makeRemoteViews(
                        R.layout.custom_notification_tall, state, timerStateMessage);

                builder.setCustomContentView(compactNotificationView)
                        .setCustomHeadsUpContentView(tallNotificationView)
                        .setCustomBigContentView(tallNotificationView);
            }

            {
                PendingIntent activityPendingIntent = MainActivity.makePendingIntent(context);
                builder.setContentIntent(activityPendingIntent);

                // Action button to run (start or resume) the timer.
                if (!isRunning) {
                    addAction(wearableExtender, R.drawable.ic_action_play, R.string.start,
                            TimerAppWidgetProvider.ACTION_RUN);
                }

                // Action button to pause the timer.
                if (!timer.isPaused()) {
                    addAction(wearableExtender, R.drawable.ic_action_pause, R.string.pause,
                            TimerAppWidgetProvider.ACTION_PAUSE);
                }

                // Action button to stop the timer.
                PendingIntent stopIntent = makeActionIntent(TimerAppWidgetProvider.ACTION_STOP);
                if (!timer.isStopped()) {
                    addAction(wearableExtender, R.drawable.ic_action_stop, R.string.stop, stopIntent);
                }

                // Action button to reset the timer to 0:00.
                if (timer.isPaused() && !timer.isPausedAt0()) {
                    addAction(wearableExtender, R.drawable.ic_action_replay, R.string.reset,
                            TimerAppWidgetProvider.ACTION_RESET);
                }

                // Swiping away the notification will Stop the timer (unless "ongoing" blocks
                // swiping, but "ongoing" also blocks bridging to Wearables).
                builder.setDeleteIntent(stopIntent);

                setNotificationStyle(builder);
            }
        }

        builder.extend(wearableExtender);

        if (soundAlarm) {
            if (Build.VERSION.SDK_INT < 26) {
                builder.setSound(getSoundUri(R.raw.cowbell4), REMINDER_STREAM);
                builder.setVibrate(VIBRATE_PATTERN);
                builder.setLights(notificationLightColor, 1000, 2000);
            }
        } else {
            builder.setSilent(true); // for phone and wearable
        }

        return builder.build();
    }

    /** Cancels all of this app's notifications. */
    public void cancelAll() {
        notificationManagerCompat.cancelAll();
    }
}
