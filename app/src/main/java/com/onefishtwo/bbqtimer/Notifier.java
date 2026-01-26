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
import android.text.SpannedString;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationCompat.WearableExtender;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.onefishtwo.bbqtimer.state.ApplicationState;

/**
 * Manages the app's Android Notifications. One-shot use to build and show a notification.
 */
public class Notifier {
    private static final String TAG = "Notifier";
    private static final int NOTIFICATION_ID = 7;

    private static final Spanned EMPTY_SPAN = new SpannedString("");

    // Vibration pattern: ms off, on, off, ...
    // Match the notification sound to the degree feasible.
    // Workaround: Start with a tiny pulse for when Android drops the initial "off" interval.
    private static final long[] VIBRATE_PATTERN = {0, 1,  280, 40,  220, 80,  440, 45,  265, 55};

    private static final int[][] ACTION_INDICES = {{}, {0}, {0, 1}, {0, 1, 2}};

    static final String ALARM_NOTIFICATION_CHANNEL_ID = "alarmChannel";
    // Channel IDs in previous releases will remain immutable on devices while the app is installed:
    // "controlsChannel"

    // --- R.id.countUpViewFlipper and R.id.countdownViewFlipper child indexes.
    private static final int RUNNING_FLIPPER_CHILD = 0;
    private static final int PAUSED_FLIPPER_CHILD = 1;
    public static final int REMINDER_STREAM = AudioManager.STREAM_ALARM;

    /** A listener for UI notifications posted, for testing. */
    public interface NotificationListener {
        void onNotificationPosted();
    }

    private static NotificationListener notificationListener = null;

    private static boolean builtNotificationChannels = false;

    @NonNull
    private final Context context;
    @NonNull
    private final NotificationManager notificationManager;
    @NonNull
    private final NotificationManagerCompat notificationManagerCompat;
    private final int notificationLightColor;

    private boolean soundAlarm = false; // whether the next notification should sound an alarm
    private int numActions; // the number of action buttons added to the notification being built

    /** Sets a listener for when the Notifier posts UI notifications, for testing. */
    public static void setNotificationListener(NotificationListener listener) {
        notificationListener = listener;
    }

    public Notifier(@NonNull Context _context) {
        this.context = _context;
        notificationManager =
                (NotificationManager) _context.getSystemService(Context.NOTIFICATION_SERVICE);
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

    /** @noinspection SameParameterValue*/
    private Uri getSoundUri(@RawRes int soundId) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + soundId);
    }

    /** Constructs a PendingIntent to use as a Notification Action. */
    private PendingIntent makeActionIntent(String action) {
        return TimerAppWidgetProvider.makeActionIntent(context, action);
    }

    /**
     * Adds an action button to the given Notification Builder and to the
     * {@link #setMediaStyleActionsInCompactView} list.
     */
    private void addAction(@NonNull NotificationCompat.Builder builder, @DrawableRes int iconId,
            @StringRes int titleId, PendingIntent intent) {
        builder.addAction(iconId, context.getString(titleId), intent);
        ++numActions;
    }

    /**
     * Sets the notification style to DecoratedCustomViewStyle to include {@link #addAction} actions.
     * <p/>
     * Experimentally, MediaStyle blocks Wear OS bridging, with or without a MediaSession. That's a
     * bummer since DecoratedCustomViewStyle doesn't show actions in the compact view.
     *<p/>
     * Calls builder.setColor() on some versions of Android to cope with OS vagaries.
     * <p/>
     * TODO: Try to restore the icon buttons on the Phone and Wearable.
     * TODO: Try to compensate for no actions visible in compressed view, e.g. default to expanded
     *  view somehow.
     * TODO: Or use MediaStyle if there's no connected Wearable.
     */
    private void setMediaStyleActionsInCompactView(@NonNull NotificationCompat.Builder builder) {
//        int num = Math.min(numActions, ACTION_INDICES.length - 1);
//
//        if (num < 1) {
//            return;
//        }

        // DecoratedCustomViewStyle wraps the custom view with standard chrome (actions, icon).
        NotificationCompat.Style style = new NotificationCompat.DecoratedCustomViewStyle();

//        style.setShowActionsInCompactView(ACTION_INDICES[num]);
        builder.setStyle(style);

        // === MediaStyle setColor() [the "accent color"] vs. Android API levels ===
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            @ColorInt int iconBackgroundColor = context.getColor(R.color.dark_orange_red);
            builder.setColor(iconBackgroundColor);
        }
    }

    /**
     * Returns a localized description of the timer's run state: "Running", "Paused 00:12.3"
     * (optionally including the time value), or "Stopped".
     */
    @NonNull
    String timerRunState(@NonNull TimeCounter timer, boolean includeTime) {
        if (timer.isRunning()) {
            return context.getString(R.string.timer_running);
        } else if (timer.isPaused()) {
            Spanned pauseTime = includeTime ? timer.formatHhMmSsFraction() : EMPTY_SPAN;
            return context.getString(R.string.timer_paused, pauseTime);
        } else {
            return context.getString(R.string.timer_stopped);
        }
    }

    /**
     * Returns a localized description of the periodic alarms.
     *
     * @param state the ApplicationState.
     * @return a localized string like "Alarm every 2 minutes", or "" for no periodic alarms.
     */
    @NonNull
    String describePeriodicAlarms(@NonNull ApplicationState state) {
        // Synthesize a "quantity" to select the right pluralization rule.
        String intervalMmSs = state.formatIntervalTimeHhMmSs();

        return state.isEnableReminders()
                ? context.getResources().getString(R.string.notification_body, intervalMmSs)
                : "";
    }

    /**
     * <em>(Re)Opens</em> this app's notification with content depending on {@code state} and
     * {@link #setAlarm(boolean)},
     * <em>or cancels</em> the app's notification if there's nothing to show or sound.
     *
     * @param state the ApplicationState state to display.
     */
    public void openOrCancel(@NonNull ApplicationState state) {
        TimeCounter timer = state.getTimeCounter();

        if (!timer.isStopped() || soundAlarm) {
            Notification notification = buildNotification(state);
            try {
                notificationManagerCompat.notify(NOTIFICATION_ID, notification);
                if (notificationListener != null) {
                    notificationListener.onNotificationPosted();
                }
            } catch (SecurityException e) { // ≈API 33+: The app should've requested permission already.
                Log.e(TAG, "Need POST_NOTIFICATIONS permission", e);
            }
        } else {
            cancelAll();
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
                    //.setLegacyStreamType(REMINDER_STREAM)
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

    /** Creates notification channel(s) lazily (or updates the text) on Android O+. */
    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT < 26 || builtNotificationChannels) {
            return;
        }

        // TODO: Skip if getNotificationChannel(ALARM_NOTIFICATION_CHANNEL_ID) != null?

        createNotificationChannelV26();
        builtNotificationChannels = true;
    }

    /** Update the notification channel (not the notification) for a UI Locale change. */
    void onLocaleChange() {
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel channel = notificationManager.getNotificationChannel(
                    ALARM_NOTIFICATION_CHANNEL_ID);

            //noinspection VariableNotUsedInsideIf
            if (channel != null) {
                builtNotificationChannels = false;
                createNotificationChannels();
            }
        }
    }

    /**
     * Make and initialize the RemoteViews for a Custom Notification.
     *<p/>
     * Workaround: A paused Chronometer doesn't show a stable value, e.g. switching light/dark theme
     * can change it, and it ignores its format string thus ruling out some workarounds.
     * *SO* when the timer is paused, flip to a TextView.
     *
     * @param layoutId the layout resource ID for the RemoteViews.
     * @param state the ApplicationState to show.
     * @param countUpMessage the message to show next to the count-up (stopwatch) Chronometer. This
     *                       Chronometer and its message are GONE if there are no periodic alarms.
     * @param countDownMessage the message to show next to the count-down (timer) Chronometer.
     * @return RemoteViews
     * @noinspection SameParameterValue
     */
    @NonNull
    private RemoteViews makeRemoteViews(
            @LayoutRes int layoutId, @NonNull ApplicationState state, @NonNull CharSequence countUpMessage,
            @NonNull CharSequence countDownMessage) {
        TimeCounter timer = state.getTimeCounter();
        long elapsedTime = timer.getElapsedTime();
        boolean isRunning = timer.isRunning();
        long rt = SystemClock.elapsedRealtime();
        long countUpBase = rt - elapsedTime;
        @IdRes int childId = isRunning ? RUNNING_FLIPPER_CHILD : PAUSED_FLIPPER_CHILD;
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), layoutId);

        // Count-up time and status
        if (!isRunning) {
            remoteViews.setTextViewText(R.id.pausedCountUp, timer.formatHhMmSs());
        }
        remoteViews.setDisplayedChild(R.id.countUpViewFlipper, childId);
        remoteViews.setChronometer(
                R.id.countUpChronometer, countUpBase, null, isRunning);
        remoteViews.setTextViewText(R.id.countUpMessage, countUpMessage);

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
            remoteViews.setTextViewText(R.id.countdownMessage, countDownMessage);
        } else {
            remoteViews.setChronometer(
                    R.id.countdownChronometer, 0, null, false);
            remoteViews.setViewVisibility(R.id.countdownChronometer, View.GONE);
            remoteViews.setViewVisibility(R.id.countdownMessage, View.GONE);
        }

        return remoteViews;
    }

    /**
     * Builds a notification. Its alarm sound, vibration, and LED light flashing are switched on/off
     * by {@link #setAlarm(boolean)}.
     * <p/>
     * TODO: Get the notification bridged to Wearables, esp. so that the Watch app's
     *  "Mute notifications on phone" setting won't totally mute the app's notifications.
     *  <a href="https://developer.android.com/training/wearables/notifications/bridger#non-bridged">When
     *  notifications aren't bridged</a>.
     * <p/>
     * Various chats with Gemini claim/hypothesize
     * - it has to be from a Foreground service -- no,
     * - it can't be CATEGORY_ALARM -- no,
     * - it can't be silent -- ?,
     * - it can't use a custom app sound resource -- no,
     * - it must have IMPORTANCE_HIGH -- ?,
     * - it can't be MediaStyle with or without a MediaSession -- YES,
     * - it must explicitly setLocalOnly(false) -- ?,
     * - custom remote views -- no,
     * - the channel ID ALARM_NOTIFICATION_CHANNEL_ID ("alarmChannel") might be heuristic-matched by
     *   Wear OS to block -- no,
     * - Chronometers in the notification's RemoteViews -- no,
     * - AudioAttributes.USAGE_ALARM (vs. USAGE_NOTIFICATION_EVENT) -- no,
     * ...
     * <p/>
     * MediaStyle notifications were a blocker. Haven't yet tested if we can revert other changes
     * like CATEGORY_REMINDER, and AudioAttributes.USAGE_NOTIFICATION_EVENT (which changed the volume
     * control channel and required changing the notification channel).
     * <p/>
     * TODO: Try to get the action buttons to use icons on Phone and Watch.
     * TODO: Try to get the action buttons to appear w/o the user having to expand it.
     *  *OR* Use MediaStyle if there's no connected Wearable?
     * TODO: Rename the "Start" action to "Run".
     * TODO: (Try to play a custom sound on the watch.)
     */
    @NonNull
    protected Notification buildNotification(@NonNull ApplicationState state) {
        createNotificationChannels();

        int pri = soundAlarm ? NotificationCompat.PRIORITY_MAX : NotificationCompat.PRIORITY_LOW;
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, ALARM_NOTIFICATION_CHANNEL_ID)
                .setPriority(pri) // for API < 26
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setLocalOnly(false) // just in case
                .setOnlyAlertOnce(!soundAlarm) // running -> paused, Activity onStart() won't ding a Wearable
                .extend(new WearableExtender()); // TODO: needed? adjustable?

        {  // Construct the visible notification contents.
            TimeCounter timer = state.getTimeCounter();
            boolean isRunning = timer.isRunning();

            builder.setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle(context.getString(R.string.app_name));

            // The "when" field is redundant with the content text and would take space from the
            // action buttons (Reset, Start, Stop) in compact view.
            builder.setShowWhen(false);

            String alarmEvery = describePeriodicAlarms(state);

            if (isRunning && state.isEnableReminders()) {
                builder.setContentText(alarmEvery);
            } else {
                String runPauseStop = timerRunState(timer, true); // Running/Paused 00:12.3/Stopped
                builder.setContentText(runPauseStop);
                if (timer.isPaused()) {
                    builder.setSubText(context.getString(R.string.dismiss_tip));
                } else if (isRunning) {
                    builder.setSubText(context.getString(R.string.no_reminders_tip));
                }
            }

            String countUpMessage = timerRunState(timer, false); // Running/Paused/Stopped
            RemoteViews notificationView = makeRemoteViews(
                    R.layout.custom_notification, state, countUpMessage, alarmEvery);

            builder.setCustomContentView(notificationView);
            builder.setCustomHeadsUpContentView(notificationView);
            builder.setCustomBigContentView(notificationView);
            numActions = 0;

            {
                PendingIntent activityPendingIntent = MainActivity.makePendingIntent(context);
                builder.setContentIntent(activityPendingIntent);

                // Action button to reset the timer.
                if (timer.isPaused() && !timer.isPausedAt0()) {
                    PendingIntent resetIntent =
                            makeActionIntent(TimerAppWidgetProvider.ACTION_RESET);
                    addAction(builder, R.drawable.ic_action_replay, R.string.reset, resetIntent);
                }

                // Action button to run (start) the timer.
                if (!isRunning) {
                    PendingIntent runIntent = makeActionIntent(TimerAppWidgetProvider.ACTION_RUN);
                    addAction(builder, R.drawable.ic_action_play, R.string.start, runIntent);
                }

                // Action button to pause the timer.
                if (!timer.isPaused()) {
                    PendingIntent pauseIntent
                            = makeActionIntent(TimerAppWidgetProvider.ACTION_PAUSE);
                    addAction(builder, R.drawable.ic_action_pause, R.string.pause, pauseIntent);
                }

                // Action button to stop the timer.
                PendingIntent stopIntent = makeActionIntent(TimerAppWidgetProvider.ACTION_STOP);
                if (!timer.isStopped()) {
                    addAction(builder, R.drawable.ic_action_stop, R.string.stop, stopIntent);
                }

                // Swiping away the notification will Stop the timer unless "ongoing" blocks swiping.
                // TODO: "Ongoing notifications can't be swiped away on locked devices," but
                //  apparently only on API < 34 and not only on locked devices.
                //  Is builder.setOngoing(isRunning) needed on older API versions?
                builder.setDeleteIntent(stopIntent);

                setMediaStyleActionsInCompactView(builder);
            }
        }

        if (soundAlarm) {
            builder.setSound(getSoundUri(R.raw.cowbell4), REMINDER_STREAM);
            builder.setVibrate(VIBRATE_PATTERN);
            builder.setLights(notificationLightColor, 1000, 2000);
        } else {
            builder.setSilent(true); // TODO: Fix: re-showing the activity when paused still sounds on wearable
        }

        return builder.build();
    }

    /** Cancels all of this app's notifications. */
    public void cancelAll() {
        notificationManagerCompat.cancelAll();
    }
}
