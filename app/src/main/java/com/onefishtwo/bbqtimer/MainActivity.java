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

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.Editable;
import android.text.Spanned;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.view.textclassifier.TextClassifier;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.onefishtwo.bbqtimer.state.ApplicationState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;

import androidx.annotation.ColorRes;
import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

/**
 * The BBQ Timer's main activity.
 */
public class MainActivity extends AppCompatActivity {
    private final String TAG = "Main";

    static final int FLAG_IMMUTABLE =
            Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({SHORTCUT_NONE, SHORTCUT_PAUSE, SHORTCUT_START})
    public @interface ShortcutChoice {}

    private static final int SHORTCUT_NONE = 0;
    private static final int SHORTCUT_PAUSE = 1;
    private static final int SHORTCUT_START = 2;
    @ShortcutChoice
    private int shortcutAction = SHORTCUT_NONE;

    private int viewConfiguration = -1; // optimization: don't reset all the views every 100 msec

    private Notifier notifier;

    /**
     * Make a PendingIntent to launch the Activity, e.g. from the notification.
     * <p/>
     * Use TaskStackBuilder so navigating back from the Activity goes to the Home screen.
     *
     * @return a PendingIntent; "May return null only if PendingIntent.FLAG_NO_CREATE has been
     *      supplied", which it hasn't.
     */
    @Nullable
    static PendingIntent makePendingIntent(@NonNull Context context) {
        Intent activityIntent = new Intent(context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context)
                .addParentStack(MainActivity.class)
                .addNextIntent(activityIntent);
        return stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT + FLAG_IMMUTABLE);
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

    private ConstraintLayout mainContainer;
    private Button resetButton;
    private Button pauseResumeButton;
    private Button stopButton;
    private TextView countUpDisplay, countdownDisplay;
    private EditText2 alarmPeriod;
    private CheckBox enableReminders;

    @MainThread
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewConfiguration = -1;
        notifier = new Notifier(this);

        // View Binding has potential but it makes project inspections create a lot of spurious
        // warnings about unused resource IDs, methods, and method arguments.
        //ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater())
        //mainContainer = binding.getRoot()
        //setContentView(mainContainer)
        setContentView(R.layout.activity_main);
        mainContainer = findViewById(R.id.main_container);

        resetButton = findViewById(R.id.resetButton);
        pauseResumeButton = findViewById(R.id.pauseResumeButton);
        stopButton = findViewById(R.id.stopButton);
        countUpDisplay = findViewById(R.id.countUpDisplay);
        countdownDisplay = findViewById(R.id.countdownDisplay);
        alarmPeriod = findViewById(R.id.alarmPeriod);
        enableReminders = findViewById(R.id.enableReminders);

        mainContainer.setOnClickListener(this::onClickBackground);
        resetButton.setOnClickListener(this::onClickReset);
        pauseResumeButton.setOnClickListener(this::onClickPauseResume);
        countdownDisplay.setOnClickListener(this::onClickPauseResume);
        alarmPeriod.setOnEditorActionListener(this::onEditAction);
        alarmPeriod.setOnFocusChangeListener2(this::onEditTextFocusChange);
        //alarmPeriod.setSelectAllOnFocus(true); // TODO: Use this instead of the (X) clear button?
        stopButton.setOnClickListener(this::onClickStop);
        countUpDisplay.setOnClickListener(this::onClickTimerText);
        enableReminders.setOnClickListener(this::onClickEnableRemindersToggle);

        // Set the TextClassifier *THEN* enable the CLEAR_TEXT (X) endIcon.
        workaroundTextClassifier(alarmPeriod);
        TextInputLayout alarmPeriodLayout = findViewById(R.id.alarmPeriodLayout);
        alarmPeriodLayout.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);

        // AutoSizeText works with android:maxLines="1" but not with android:singleLine="true".
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(countUpDisplay, 16,
                1000, 1, TypedValue.COMPLEX_UNIT_DIP);
        TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(countdownDisplay, 14,
                56, 1, TypedValue.COMPLEX_UNIT_DIP);

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

    /**
     * API 27: Work around an Android bug where double-clicking the EditText field would cause these
     * log errors:
     *
     *   TextClassifierImpl: Error suggesting selection for text. No changes to selection suggested.
     *     java.io.FileNotFoundException: No file for null locale
     *         at android.view.textclassifier.TextClassifierImpl.getSmartSelection(TextClassifierImpl.java:208)
     *         ...
     *   TextClassifierImpl: Error getting assist info.
     *     java.io.FileNotFoundException: No file for null locale
     *         at android.view.textclassifier.TextClassifierImpl.getSmartSelection(TextClassifierImpl.java:208)
     *         ...
     *
     * API > 27: Work around an Android bug that calls the TextClassifier on the main thread
     * (UI thread) [e.g. when the user double-taps the EditText field, or long-presses it, or
     * dismisses the soft keyboard when there's a text selection], causing this log warning even
     * though no app code is on the call stack:
     *
     *   W/androidtc: TextClassifier called on main thread.
     *
     * To avoid the delay and potential ANR, just bypass the irrelevant TextClassifier. (This
     * problem might not occur on API 28 - 29, but it's safer to do this uniformly.)
     */
    @SuppressWarnings("SpellCheckingInspection")
    private static void workaroundTextClassifier(EditText editText) {
        if (Build.VERSION.SDK_INT >= 27) {
            editText.setTextClassifier(TextClassifier.NO_OP);
        }
    }

    private void logTheConfiguration(@NonNull Configuration config) {
        Log.i(TAG,
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
            case SHORTCUT_NONE:
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

        // Workaround: On Android SDK ≤ 27, the automatic focus in the text field is distracting and
        // annoying on Activity start, app switch, or screen rotation, esp. with the (X) endIcon.
        defocusTextField(alarmPeriod);

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

    /** Constructs a Snackbar. */
    @UiThread
    @NonNull
    private Snackbar makeSnackbar(@StringRes int stringResId) {
        return Snackbar.make(mainContainer, stringResId, BaseTransientBottomBar.LENGTH_LONG);
    }

    /**
     * Sets the Snackbar's action.
     * </p>
     * NOTE: This used to set the action's text color but a custom background color gets overridden
     * now in day or night theme, so the custom text color became low-contrast. The two colors might
     * be settable in AppTheme but why bother?
     */
    @UiThread
    private void setSnackbarAction(@NonNull Snackbar snackbar, @StringRes int resId,
            View.OnClickListener listener) {
        snackbar.setAction(resId, listener);
    }

    @UiThread
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        logTheConfiguration(newConfig);
    }

    /** The user tapped a Run/Pause action. */
    @UiThread
    @SuppressWarnings("UnusedParameters")
    public void onClickPauseResume(View v) {
        defocusTextField(alarmPeriod);

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

        defocusTextField(alarmPeriod);

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
        defocusTextField(alarmPeriod);

        timer.stop();
        updateHandler.endScheduledUpdates();
        updateUI();
    }

    /** The user tapped the time text: Cycle Stopped | Reset -> Running -> Paused -> Stopped. */
    @UiThread
    @SuppressWarnings("UnusedParameters")
    public void onClickTimerText(View v) {
        defocusTextField(alarmPeriod);

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
        defocusTextField(alarmPeriod);

        state.setEnableReminders(enableReminders.isChecked());
        state.save(this);
        updateUI();

        if (state.isEnableReminders()) {
            informIfNotificationAlarmsMuted();
        }
    }

    /** Hides the soft keyboard, if we're lucky. */
    // https://stackoverflow.com/a/17789187/1682419
    public void hideKeyboard(@Nullable View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if (v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }

        View focussed = getCurrentFocus();
        if (focussed != null) {
            imm.hideSoftInputFromWindow(focussed.getWindowToken(), 0);
        }
    }

    /**
     * Remove focus from the text field.
     * NOTE: The alarmPeriod has an OnFocusChangeListener. Defocussing it, if it had focus, will
     * (re)set its contents to the current state and close its soft keyboard.
     * <p/>
     * Workaround: Force it on older Android versions which grab and hold focus where newer Android
     * versions wouldn't. https://stackoverflow.com/a/11044709/1682419
     * <p/>
     * TODO: This workaround seems to break the ability to TAB or arrow into and out of the field.
     *  Maybe fix that by re-enabling focus on a relevant key event. Or maybe it's just an edge case
     *  for a dwindling number of Androids.
     */
    @UiThread
    @SuppressLint("ClickableViewAccessibility")
    private void defocusTextField(EditText textField) {
        if (Build.VERSION.SDK_INT <= 27) {
            textField.setFocusable(false);

            textField.setOnTouchListener((v, event) -> {
                v.setFocusable(true);
                v.setFocusableInTouchMode(true);
                return false; // don't consume the event; continue with normal processing
            });
        } else {
            textField.clearFocus();
        }
    }

    /**
     * Sets the alarmPeriod EditText contents, skipping the no-op case to maintain any selection and
     * minimize log warnings from InputConnectionWrapper.
     * <p/>
     * Also for reducing those log warnings, it might help to hideKeyboard() before setText(), but
     * that means passing a ResultReceiver to hideSoftInputFromWindow() to do the setText() after it
     * finishes animating away.
     * https://stackoverflow.com/a/29470242/1682419
     */
    @UiThread
    private void displayAlarmPeriod() {
        Editable text = alarmPeriod.getText();
        String newText = state.formatIntervalTimeHhMmSsCompact();

        if (text == null || !newText.equals(text.toString())) {
            alarmPeriod.setText(newText);
        }
    }

    /** Parse, bound, then adopt the alarmPeriod input text if valid, else revert it. */
    @UiThread
    private void processAlarmPeriodInput() {
        Editable text = alarmPeriod.getText();
        String input = text == null ? "" : text.toString();
        int newSeconds = TimeCounter.parseHhMmSs(input);

        if (newSeconds > 0 && newSeconds != state.getSecondsPerReminder()) {
            state.setSecondsPerReminder(newSeconds); // clips the value
            state.save(this);
            updateUI(); // update countdownDisplay, notifications, and widgets
        }

        defocusTextField(alarmPeriod);
    }

    /** The user tapped the background => Accept pending alarmPeriod text input. */
    @UiThread
    public void onClickBackground(View view) {
        View focussed = getCurrentFocus();

        if (focussed == alarmPeriod) {
            processAlarmPeriodInput();
        }

        view.clearFocus(); // defocus the background
    }

    /**
     * The TextEdit field's focus changed, e.g. by TAB, arrow keys, or a call to view.clearFocus().
     * If it lost focus, cancel any pending edits and hide the soft keyboard.
     * <p/>
     * NOTE: Without this code, tapping any other widget will reset the input text as part of taking
     * an action, but just moving focus wouldn't accept or cancel the input nor hide the keyboard.
     * <p/>
     * TODO: Is this UI intuitive? How else to support cancel (revert)?
     */
    @UiThread
    public void onEditTextFocusChange(View view, boolean nowHasFocus) {
        if (view == alarmPeriod && !nowHasFocus) {
            hideKeyboard(view);
            displayAlarmPeriod();
        }
    }

    /** The user tapped a TextEdit soft keyboard completion action or physical Enter key. */
    // TODO: After any physical keystrokes (action=ACTION_DOWN + keyCode=KEYCODE_ENTER, arrow keys,
    //  or digits), is there a way to keep it from focussing displayView? Maybe it's OK since there
    //  was a keyboard action, thus leaving "touch mode." Does this vary by Android version?
    @UiThread
    public boolean onEditAction(TextView view, int actionId, KeyEvent event) {
        if (view == alarmPeriod) {
            processAlarmPeriodInput();
            return true;
        }
        return false;
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

    /** Updates the elapsed time, alarm interval time, and the alarm count-down time displays. */
    @UiThread
    private void displayTime() {
        Spanned formatted         = timer.formatHhMmSsFraction();
        @ColorRes int textColorsId =
                timer.isRunning() ? R.color.running_timer_colors
                : timer.isPaused() ? pausedTimerColors()
                : R.color.reset_timer_colors;
        ColorStateList textColors = ContextCompat.getColorStateList(this, textColorsId);
        long countdownToNextAlarm = state.getMillisecondsToNextAlarm();

        countUpDisplay.setText(formatted);
        countUpDisplay.setTextColor(textColors);

        countdownDisplay.setText(TimeCounter.formatHhMmSs(countdownToNextAlarm));
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
            pauseResumeButton.setCompoundDrawablesWithIntrinsicBounds(
                    isRunning ? R.drawable.ic_pause : R.drawable.ic_play, 0, 0, 0);
            stopButton.setVisibility(isStopped ? View.INVISIBLE : View.VISIBLE);
            countdownDisplay.setVisibility(areRemindersEnabled ? View.VISIBLE : View.INVISIBLE);
            enableReminders.setChecked(areRemindersEnabled);
            displayAlarmPeriod();
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
            Log.e(TAG, "Couldn't open notification Settings: " + e);
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
            Log.e(TAG, "Couldn't open notification channel Settings: " + e);
            // TODO: Open a toast?
        }
    }

}
