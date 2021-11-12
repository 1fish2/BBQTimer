// The MIT License (MIT)
//
// Copyright (c) 2014 Jerry Morrison
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
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.onefishtwo.bbqtimer.state.ApplicationState;

import java.lang.ref.WeakReference;

import androidx.annotation.ColorRes;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

/**
 * The BBQ Timer's main activity.
 */
public class MainActivity extends AppCompatActivity implements NumberPicker.OnValueChangeListener {
    private final String TAG = "Main";

    private static final MinutesChoices minutesChoices = new MinutesChoices();

    private static final int SHORTCUT_NONE = 0;
    private static final int SHORTCUT_PAUSE = 1;
    private static final int SHORTCUT_START = 2;
    private int shortcutAction = SHORTCUT_NONE;

    private int viewConfiguration = -1; // optimization: don't reset all the views every 100 msec

    private Notifier notifier;

    /** (Re)makes all locale-dependent strings. */
    private static void makeLocaleStrings() {
        minutesChoices.updateLocale();
    }

    /** Converts seconds/alarm to a minutesPicker choice string. */
    public static String secondsToMinuteChoiceString(int seconds) {
        return minutesChoices.secondsToMinuteChoiceString(seconds);
    }

    /**
     * A Handler for periodic display updates.
     *
     * <p/>Queued Messages refer to the Handler which refers to the Activity. Since
     * Activity#onStop() calls #endScheduledUpdates(), there's no memory leak but using a
     * WeakReference protects that from future changes and appeases Lint.
     */
    private static class UpdateHandler extends Handler {
        private static final int MSG_UPDATE = 1;
        private static final long UPDATE_INTERVAL = 100; // msec
        @NonNull
        private final WeakReference<MainActivity> weakActivity;

        private UpdateHandler(MainActivity activity) {
            super(Looper.getMainLooper());
            weakActivity = new WeakReference<>(activity);
        }

        /** Handles a message to periodically update the views. */
        @Override
        public void handleMessage(@NonNull Message msg) {
            MainActivity activity = weakActivity.get();

            super.handleMessage(msg);
            if (msg.what == MSG_UPDATE) {
                if (activity != null) {
                    activity.updateViews();
                }
                scheduleNextUpdate();
            }
        }

        /** Schedules the next Activity display update if the timer is running. */
        private void scheduleNextUpdate() {
            MainActivity activity = weakActivity.get();

            if (activity != null && !activity.timer.isStopped()) {
                sendEmptyMessageDelayed(MSG_UPDATE, UPDATE_INTERVAL);
            }
        }

        /** Ends any scheduled updated messages. */
        void endScheduledUpdates() {
            removeMessages(MSG_UPDATE);
        }

        /** Begins the periodic display update messages if the timer is running. */
        void beginScheduledUpdate() {
            endScheduledUpdates();
            scheduleNextUpdate();
        }
    }

    private final UpdateHandler updateHandler = new UpdateHandler(this);
    private ApplicationState state;
    private TimeCounter timer;

    private Button resetButton;
    private Button startStopButton;
    private Button stopButton;
    private TextView displayView;
    private CompoundButton enableRemindersToggle;
    private NumberPicker minutesPicker;

    @MainThread
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewConfiguration = -1;
        notifier = new Notifier(this);

        makeLocaleStrings();
        setContentView(R.layout.activity_main);

        resetButton           = findViewById(R.id.resetButton);
        startStopButton       = findViewById(R.id.startStopButton);
        stopButton            = findViewById(R.id.stopButton);
        displayView           = findViewById(R.id.display);
        enableRemindersToggle = findViewById(R.id.enableReminders);
        minutesPicker         = findViewById(R.id.minutesPicker);

        resetButton.setOnClickListener(this::onClickReset);
        startStopButton.setOnClickListener(this::onClickStartStop);
        stopButton.setOnClickListener(this::onClickStop);
        displayView.setOnClickListener(this::onClickTimerText);
        enableRemindersToggle.setOnClickListener(this::onClickEnableRemindersToggle);

        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(minutesChoices.choices.length - 1);
        minutesPicker.setDisplayedValues(minutesChoices.choices);
        minutesPicker.setWrapSelectorWheel(false);
        minutesPicker.setOnValueChangedListener(this);
        minutesPicker.setFocusableInTouchMode(true);

        // AutoSizeText works with android:maxLines="1" but not with android:singleLine="true".
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(displayView, 16,
                1000, 1, TypedValue.COMPLEX_UNIT_DIP);

        setVolumeControlStream(AudioManager.STREAM_ALARM);

        shortcutAction = SHORTCUT_NONE;
        Intent callingIntent = getIntent();
        if (callingIntent != null) {
            String action = callingIntent.getAction(); // null action occurred in multi-window testing
            if (Intent.ACTION_QUICK_CLOCK.equals(action)) { // App Shortcut: Pause @ 00:00
                shortcutAction = SHORTCUT_PAUSE;
                // Modify the Intent so a configuration change like enter/exit multi-window mode
                // won't repeat the shortcut action when it re-creates the Activity.
                callingIntent.setAction(Intent.ACTION_MAIN);
            } else if (Intent.ACTION_RUN.equals(action)) { // App Shortcut: Start @ 00:00
                shortcutAction = SHORTCUT_START;
                callingIntent.setAction(Intent.ACTION_MAIN);
            }
        }

        logTheConfiguration(getResources().getConfiguration());
    }

    private void logTheConfiguration(@NonNull Configuration config) {
        Log.v(TAG,
            String.format("Config densityDpi: %d, size DPI: %dx%d, orientation: %d",
                    config.densityDpi, config.screenWidthDp, config.screenHeightDp,
                    config.orientation));
    }

    @UiThread
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Inflate the menu; this adds items to the action bar if it is present.
        // TODO: Inflate the menu once it has useful items:
        // getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /** The Activity is now visible. */
    @MainThread
    @Override
    protected void onStart() {
        super.onStart();
        viewConfiguration = -1;

        // Load persistent state.
        state = ApplicationState.sharedInstance(this);
        timer = state.getTimeCounter();
        state.setMainActivityIsVisible(true);

        // Apply the app shortcut action, if any, once.
        switch (shortcutAction) {
            case SHORTCUT_PAUSE: // App Shortcut: Pause @ 00:00
                timer.reset();
                break;
            case SHORTCUT_START: // App Shortcut: Start @ 00:00
                timer.reset();
                timer.start();
                break;
        }
        shortcutAction = SHORTCUT_NONE;

        state.save(this);

        updateUI();

        updateHandler.beginScheduledUpdate();
    }

    @MainThread
    @Override
    protected void onResume() {
        super.onResume();

        // Take focus from minutesPicker's EditText child.
        minutesPicker.requestFocus();

        informIfNotificationAlarmsMuted();
    }

    /** The Activity is no longer visible. */
    @MainThread
    @Override
    protected void onStop() {
        updateHandler.endScheduledUpdates();

        // Update persistent state.
        state.setMainActivityIsVisible(false);
        state.save(this);

        AlarmReceiver.updateNotifications(this); // after setMainActivityIsVisible()

        super.onStop();
    }

    /**
     * Informs the user if the app's notifications are disabled (offering to open Settings to ENABLE
     * them) or else if reminders are enabled but the alarm channel is muted (offering to UNMUTE).
     * <p/>
     * TODO: How to detect if the app's notifications are visible but silenced? Silencing kills the
     * audio and heads-up notification shades.
     * <p/>
     * NOTE: The notification channel misconfigured test only works on API 26+.
     */
    @UiThread
    private void informIfNotificationAlarmsMuted() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        // Check for disabled notifications.
        // NOTE: If notifications are disabled, so are Toasts.
        if (!notificationManager.areNotificationsEnabled()) {
            Snackbar snackbar = makeSnackbar(R.string.notifications_disabled);
            setSnackbarAction(snackbar, R.string.notifications_enable,
                    view -> openNotificationSettingsForApp());
            snackbar.show();
            return;
        }

        // Warn about inaudible alarms only when reminders are enabled, not when running silently.
        if (!state.isEnableReminders()) {
            return;
        }

        final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            int volume = am.getStreamVolume(AudioManager.STREAM_ALARM);

            // Check for muted alarms.
            if (volume <= 0) {
                Snackbar snackbar = makeSnackbar(R.string.alarm_muted);
                setSnackbarAction(snackbar, R.string.alarm_unmute,
                        view -> am.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_RAISE,
                                0));
                snackbar.show();
                return;
            }
        }

        // Check for misconfigured Alarm notification channel.
        if (!notifier.isAlarmChannelOK()) {
            Snackbar snackbar = makeSnackbar(R.string.notifications_misconfigured);
            if (android.os.Build.VERSION.SDK_INT >= 26) { // Where this Settings Intent works.
                setSnackbarAction(snackbar, R.string.notifications_configure,
                        view -> openNotificationChannelSettings(
                                Notifier.ALARM_NOTIFICATION_CHANNEL_ID));
            }
            snackbar.show();
        }
    }

    /** Constructs a Snackbar with the app's dark orange red color. */
    @UiThread
    @NonNull
    private Snackbar makeSnackbar(@StringRes int stringResId) {
        Snackbar snackbar = Snackbar.make(findViewById(R.id.main_container), stringResId,
                BaseTransientBottomBar.LENGTH_LONG);
        snackbar.getView().setBackgroundColor(
                ContextCompat.getColor(this, R.color.dark_orange_red));
        return snackbar;
    }

    /** Sets the Snackbar's action. */
    @UiThread
    private void setSnackbarAction(@NonNull Snackbar snackbar, @StringRes int resId,
            View.OnClickListener listener) {
        snackbar.setAction(resId, listener)
                .setActionTextColor(ContextCompat.getColor(this, R.color.contrasting_text));
    }

    @UiThread
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        logTheConfiguration(newConfig);
    }

    /** The user tapped the Run/Pause button (named "StartStop"). */
    // TODO: Use listeners to update the Activity UI and app widgets.
    // A Proguard rule keeps all Activity *(View) methods.
    @UiThread
    @SuppressWarnings("UnusedParameters")
    public void onClickStartStop(View v) {
        timer.toggleRunPause();
        updateHandler.beginScheduledUpdate();
        updateUI();

        if (timer.isRunning()) {
            informIfNotificationAlarmsMuted();
        }
    }

    /** The user tapped the Reset button; go to Paused at 0:00. */
    @UiThread
    @SuppressWarnings("UnusedParameters")
    public void onClickReset(View v) {
        boolean wasStopped = timer.isStopped();

        timer.reset();
        updateHandler.beginScheduledUpdate();
        updateUI();

        if (wasStopped) {
            informIfNotificationAlarmsMuted();
        }
    }

    /** The user tapped the Stop button. */
    @UiThread
    @SuppressWarnings("UnusedParameters")
    public void onClickStop(View v) {
        timer.stop();
        updateHandler.endScheduledUpdates();
        updateUI();
    }

    /** The user tapped the time text: Cycle Stopped | Reset -> Running -> Paused -> Stopped. */
    @UiThread
    @SuppressWarnings("UnusedParameters")
    public void onClickTimerText(View v) {
        timer.cycle();
        updateHandler.beginScheduledUpdate();
        updateUI();

        if (timer.isRunning()) {
            informIfNotificationAlarmsMuted();
        }
    }

    /** The user clicked the enable/disable periodic-reminders toggle switch/checkbox. */
    @UiThread
    @SuppressWarnings("UnusedParameters")
    public void onClickEnableRemindersToggle(View v) {
        state.setEnableReminders(enableRemindersToggle.isChecked());
        state.save(this);
        AlarmReceiver.updateNotifications(this);
        updateUI();

        if (state.isEnableReminders()) {
            informIfNotificationAlarmsMuted();
        }
    }

    /** A NumberPicker value changed. */
    @UiThread
    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        if (picker == minutesPicker) {
            state.setSecondsPerReminder(MinutesChoices.pickerChoiceToSeconds(newVal));
            state.save(this);
            AlarmReceiver.updateNotifications(this);
        }
    }

    /** @return a ColorStateList resource ID; time-dependent for blinking. */
    @UiThread
    @ColorRes
    private int pausedTimerColors() {
        TimeCounter timeCounter = state.getTimeCounter();
        long millis = timeCounter.elapsedRealtimeClock() - timeCounter.getPauseTime();
        long seconds = millis / 1000L;

        return (seconds & 1) == 0 ? R.color.paused_alternate_timer_colors
                : R.color.paused_timer_colors;
    }

    /** Updates the display of the elapsed time. */
    @UiThread
    private void displayTime() {
        Spanned formatted         = timer.formatHhMmSsFraction();
        @ColorRes int textColorsId =
                timer.isRunning() ? R.color.running_timer_colors
                : timer.isPaused() ? pausedTimerColors()
                : R.color.reset_timer_colors;
        ColorStateList textColors = ContextCompat.getColorStateList(this, textColorsId);

        displayView.setText(formatted);
        displayView.setTextColor(textColors);
    }

    /** Updates the Activity's views for the current state. */
    @UiThread
    private void updateViews() {
        boolean isRunning = timer.isRunning();
        boolean isStopped = timer.isStopped();
        boolean isPausedAt0 = timer.isPausedAt0();
        boolean areRemindersEnabled = state.isEnableReminders();
        int newConfiguration = (isRunning ? 1 : 0) | (isStopped ? 2 : 0) | (isPausedAt0 ? 4 : 0)
                | (areRemindersEnabled ? 8 : 0);

        displayTime();

        if (viewConfiguration != newConfiguration) { // optimize out the nearly-always no-op case
            viewConfiguration = newConfiguration;

            resetButton.setCompoundDrawablesWithIntrinsicBounds(
                    isStopped ? R.drawable.ic_pause : R.drawable.ic_replay, 0, 0, 0);
            resetButton.setVisibility(isRunning || isPausedAt0 ? View.INVISIBLE : View.VISIBLE);
            startStopButton.setCompoundDrawablesWithIntrinsicBounds(
                    isRunning ? R.drawable.ic_pause : R.drawable.ic_play, 0, 0, 0);
            stopButton.setVisibility(isStopped ? View.INVISIBLE : View.VISIBLE);
            enableRemindersToggle.setChecked(areRemindersEnabled);
            minutesPicker.setValue(MinutesChoices.secondsToPickerChoice(
                    state.getSecondsPerReminder()));
            minutesPicker.setEnabled(areRemindersEnabled);
        }
    }

    /** Updates the whole UI for the current state: Activity, Notifications, alarms, and widgets. */
    @UiThread
    private void updateUI() {
        updateViews();

        AlarmReceiver.updateNotifications(this);

        TimerAppWidgetProvider.updateAllWidgets(this, state);
    }

    /**
     * Helper method for the SnackBar action: This opens the Settings screen where the user can
     * re-enable the application's notifications.
     * (From an example program for Android Wearable notifications.)
     *<p/>
     * NOTE: Call this only if the user asked to do it.
     */
    private void openNotificationSettingsForApp() {
        // Links to this app's notification settings
        Intent intent = new Intent();

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            intent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        } else {
            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
            intent.putExtra("app_package", getPackageName());
            intent.putExtra("app_uid", getApplicationInfo().uid);
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Couldn't open notification Settings: " + e.toString());
            // TODO: Open another SnackBar? (Toasts are disabled along with notifications.)
        }
    }

    /**
     * Helper method for the SnackBar action: This opens the Settings screen where the user can
     * reconfigure one of the application's notification channels.
     *<p/>
     * NOTE: Call this only if the user asked to do it.
     */
    @TargetApi(26)
    private void openNotificationChannelSettings(
            @SuppressWarnings("SameParameterValue") String channelId) {
        Intent intent = new Intent();

        intent.setAction(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        intent.putExtra(Settings.EXTRA_CHANNEL_ID, channelId);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Couldn't open notification channel Settings: " + e.toString());
            // TODO: Open a toast?
        }
    }

}
