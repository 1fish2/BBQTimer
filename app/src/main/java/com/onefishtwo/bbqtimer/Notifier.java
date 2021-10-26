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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;

import com.onefishtwo.bbqtimer.state.ApplicationState;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;
import androidx.annotation.StringRes;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.media.app.NotificationCompat.MediaStyle;

/**
 * Manages the app's Android Notifications.
 */
public class Notifier {
    private static final int NOTIFICATION_ID = 7;

    private static final int FLAG_IMMUTABLE =
            android.os.Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;

    private static final long[] VIBRATE_PATTERN = {150, 82, 180, 96}; // ms off, ms on, ms off, ...
    private static final int[][] ACTION_INDICES = {{}, {0}, {0, 1}, {0, 1, 2}};

    static final String ALARM_NOTIFICATION_CHANNEL_ID = "alarmChannel";

    // Was in release "2.5" (v15) and will remain immutable on devices while the app is installed:
    // static final String CONTROLS_NOTIFICATION_CHANNEL_ID = "controlsChannel";

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

    private Uri getSoundUri(@RawRes int soundId) {
        return Uri.parse("android.resource://" + context.getPackageName() + "/" + soundId);
    }

    /** Constructs a PendingIntent to use as a Notification Action. */
    private PendingIntent makeActionIntent(String action) {
        return TimerAppWidgetProvider.makeActionIntent(context, action);
    }

    /**
     * Adds an action button to the given NotificationBuilder and to the
     * {@link #setMediaStyleActionsInCompactView} list.
     */
    private void addAction(@NonNull NotificationCompat.Builder builder, @DrawableRes int iconId,
            @StringRes int titleId, PendingIntent intent) {
        builder.addAction(iconId, context.getString(titleId), intent);
        ++numActions;
    }

    /**
     * Makes the first 3 added {@link #addAction} actions appear in a MediaStyle notification
     * view. The Media template should fit 3 actions in its collapsed view, 6 actions in expanded
     * view, or 5 actions in expanded view with a large image but I haven't tested that.
     *<p/>
     * Calls builder.setColor() on some versions of Android to cope with OS vagaries.
     */
    private void setMediaStyleActionsInCompactView(@NonNull NotificationCompat.Builder builder) {
        int num = Math.min(numActions, ACTION_INDICES.length - 1);

        if (num < 1) {
            return;
        }

        MediaStyle style = new MediaStyle().setShowActionsInCompactView(ACTION_INDICES[num]);
        builder.setStyle(style);

        // === MediaStyle setColor() [the "accent color"] vs. Android API levels ===
        // API 21 L - 22 L1: colors the notification are background needlessly.
        // API 23 M: See below.
        // API 24 N - API 27 O1: colors the small icon, action button, and app title color. Garish.
        // API 28 P - API 30 R: colors the small icon and action button. Garish.
        // API 21 S: See below.
        // setColorized(false) didn't change any of these results.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.M) {
            // Android 6 M, API 23: setColor() colors the notification are background and avoids a
            // low contrast dark gray text on darker gray background in the the heads-up case.
            // setColor() also changes pull-down notifications and carries over from one
            // notification to its replacement. http://stackoverflow.com/q/38415467/1682419
            int workaroundColor = context.getColor(R.color.gray_text);
            builder.setColor(workaroundColor);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) { // Android 12, API 31
            // Android 12 S, API 31: setColor() colors the background circle behind the small icon.
            int iconBackgroundColor = context.getColor(R.color.dark_orange_red);
            builder.setColor(iconBackgroundColor);
        }
    }

    /** Returns a localized description of the timer's run state, e.g. "Paused 00:12.3". */
    @NonNull
    String timerRunState(@NonNull TimeCounter timer) {
        if (timer.isRunning()) {
            return context.getString(R.string.timer_running);
        } else if (timer.isPaused()) {
            return context.getString(R.string.timer_paused, timer.formatHhMmSsFraction());
        } else {
            return context.getString(R.string.timer_stopped);
        }
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
            notificationManagerCompat.notify(NOTIFICATION_ID, notification);
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
        if (android.os.Build.VERSION.SDK_INT >= 26) {
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
     * Creates a notification channel that matches the parameters #buildNotification() uses. The
     * "Alarm" channel sounds an alarm at heads-up High importance.
     */
    @TargetApi(26)
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
                    //.setLegacyStreamType(AudioManager.STREAM_ALARM)
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

        createNotificationChannelV26();
        builtNotificationChannels = true;
    }

    /** Update state (e.g. notification channel text) for a UI Locale change. */
    void onLocaleChange() {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
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
     * Builds a notification. Its alarm sound, vibration, and LED light flashing are switched on/off
     * by {@link #setAlarm(boolean)}.
     */
    @NonNull
    protected Notification buildNotification(@NonNull ApplicationState state) {
        createNotificationChannels();

        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(context, ALARM_NOTIFICATION_CHANNEL_ID)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC);

        {  // Construct the visible notification contents.
            TimeCounter timer = state.getTimeCounter();
            boolean isRunning = timer.isRunning();

            builder.setSmallIcon(R.drawable.notification_icon)
                    .setContentTitle(context.getString(R.string.app_name));

            // Android API < 24 stretches the small icon into a fuzzy large icon, so add a large
            // icon. On API 24+, adopt the UI guideline, thus making the notification fit more in
            // the compact form, which could help when the lock screen is set to
            // "Show sensitive content only when unlocked".
            if (Build.VERSION.SDK_INT < 24) {
                Bitmap largeIcon = BitmapFactory.decodeResource(context.getResources(),
                        R.drawable.ic_large_notification);
                builder.setLargeIcon(largeIcon);
            }

            if (isRunning) {
                builder.setWhen(System.currentTimeMillis() - timer.getElapsedTime())
                        .setUsesChronometer(true);
            } else {
                // Hide the "when" field, which isn't useful while Paused, so it doesn't take space
                // in the compact view along with 3 action buttons (Reset, Start, Stop).
                builder.setShowWhen(false);
            }

            // Set the notification body text to explain the run state.
            if (isRunning && state.isEnableReminders()) {
                int reminderSecs   = state.getSecondsPerReminder();

                // Synthesize a "quantity" to select the right pluralization rule.
                int quantity = MinutesChoices.secondsToMinutesPluralizationQuantity(reminderSecs);
                String minutes     = MainActivity.secondsToMinuteChoiceString(reminderSecs);
                String contentText = context.getResources()
                        .getQuantityString(R.plurals.notification_body, quantity, minutes);

                builder.setContentText(contentText);
            } else {
                builder.setContentText(timerRunState(timer));
                if (timer.isPaused()) {
                    builder.setSubText(context.getString(R.string.dismiss_tip));
                } else if (isRunning) {
                    builder.setSubText(context.getString(R.string.no_reminders_tip));
                }
            }

            numActions = 0;

            {
                // Make an Intent to launch the Activity from the notification.
                Intent activityIntent = new Intent(context, MainActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                // So navigating back from the Activity goes from the app to the Home screen.
                TaskStackBuilder stackBuilder = TaskStackBuilder.create(context)
                        .addParentStack(MainActivity.class)
                        .addNextIntent(activityIntent);
                PendingIntent activityPendingIntent =
                        stackBuilder.getPendingIntent(0,
                                PendingIntent.FLAG_UPDATE_CURRENT + FLAG_IMMUTABLE);
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

                // Allow stopping via dismissing the notification (unless it's "ongoing").
                builder.setDeleteIntent(stopIntent);

                setMediaStyleActionsInCompactView(builder);
            }

            if (isRunning) {
                builder.setOngoing(true);
            }
        }

        if (soundAlarm) {
            builder.setSound(getSoundUri(R.raw.cowbell4), AudioManager.STREAM_ALARM);
            builder.setVibrate(VIBRATE_PATTERN);
            builder.setLights(notificationLightColor, 1000, 2000);
        } else {
            builder.setSilent(true);
        }

        return builder.build();
    }

    /** Cancels all of this app's notifications. */
    public void cancelAll() {
        notificationManagerCompat.cancelAll();
    }
}
