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
import android.app.Activity;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.Settings;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.onefishtwo.bbqtimer.state.ApplicationState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Vector;

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
public class MainActivity extends AppCompatActivity
        implements RecipeEditorDialogFragment.RecipeEditorDialogFragmentListener {
    private static final String TAG = "Main";

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

        UpdateHandler(MainActivity activity) {
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

            if (activity != null && !activity.getTimer().isStopped()) {
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
    private String lastRecipes; // the last input to styleTheRecipes()
    private Vector<SpannableString> styledRecipes; // the output from styleTheRecipes()
    private PopupMenu popupMenu;

    private ConstraintLayout mainContainer;
    private Button resetButton;
    private Button pauseResumeButton;
    private Button stopButton;
    private TextView countUpDisplay, countdownDisplay;
    private EditText2 alarmPeriod;
    private CheckBox enableReminders;

    TimeCounter getTimer() {
        return timer;
    }

    @MainThread
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewConfiguration = -1;
        notifier = new Notifier(this);
        lastRecipes = "";
        styledRecipes = new Vector<>(20);
        popupMenu = null;

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
        stopButton.setOnClickListener(this::onClickStop);
        countUpDisplay.setOnClickListener(this::onClickTimerText);
        enableReminders.setOnClickListener(this::onClickEnableRemindersToggle);

        // Set the TextClassifier *THEN* enable the CLEAR_TEXT (X) endIcon.
        RecipeEditorDialogFragment.workaroundTextClassifier(alarmPeriod);
        TextInputLayout alarmPeriodLayout = findViewById(R.id.alarmPeriodLayout);
        alarmPeriodLayout.setEndIconMode(TextInputLayout.END_ICON_CLEAR_TEXT);
        alarmPeriodLayout.setStartIconOnClickListener(this::onClickRecipeMenuButton);
        alarmPeriodLayout.setStartIconOnLongClickListener(v -> {
            showRecipeEditor();
            return true;
        });

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

    private void logTheConfiguration(@NonNull Configuration config) {
        Log.i(TAG,
            String.format("Config densityDpi: %d, size DPI: %dx%d, orientation: %d",
                    config.densityDpi, config.screenWidthDp, config.screenHeightDp,
                    config.orientation));
    }

    @UiThread
    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Inflate the menu; this adds items to the action bar if it is present.
        // TODO: Inflate the menu once it has useful items:
        // getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * For the pop-up menu, convert state.getRecipes() into SpannableStrings in styledRecipes.
     * This is idempotent and fast if the input hasn't changed.
     * <p/>
     * INPUTS: state.getRecipes().<p/>
     * OUTPUTS: the styledRecipes Vector.
     */
    void styleTheRecipes() {
        String recipes = state.getRecipes();

        if (recipes.equals(lastRecipes)) {
            return;
        }
        lastRecipes = recipes;

        String[] lines = recipes.split("\n");

        styledRecipes.clear();
        styledRecipes.ensureCapacity(lines.length);

        for (String r : lines) { // Italicize the notes that follow each recipe's leading token.
            String recipe = r.trim();
            int recipeLength = recipe.length();
            int tokenLength = TimeCounter.lengthOfLeadingIntervalTime(recipe);
            SpannableString ss = new SpannableString(recipe);

            ss.setSpan(new StyleSpan(Typeface.ITALIC), tokenLength, recipeLength, 0);
            styledRecipes.add(ss);
        }
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

        if (popupMenu != null) {
            popupMenu.dismiss(); // avoid android.view.WindowLeaked PopupWindow$PopupViewContainer
            // TODO: It still throws WindowLeaked PopupWindow$PopupDecorView on API 32.
            popupMenu = null;
        }

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

    /** The user clicked the button to open the "recipes" menu of alarm periods. */
    @UiThread
    public void onClickRecipeMenuButton(View v) {
        popupMenu = new PopupMenu(this, v);
        Menu menu = popupMenu.getMenu();

        popupMenu.getMenuInflater().inflate(R.menu.recipe_menu, menu);
        popupMenu.setOnMenuItemClickListener(this::onRecipeMenuItemClick);
        popupMenu.setOnDismissListener(this::onDismissRecipeMenu);

        MenuItem item = menu.findItem(R.id.edit_recipes); // depends on the current locale
        if (item != null) {
            CharSequence title = item.getTitle();
            SpannableString ss = new SpannableString(title);

            ss.setSpan(new StyleSpan(Typeface.BOLD), 0, ss.length(), 0);
            item.setTitle(ss);
        }

        styleTheRecipes();

        for (SpannableString recipe : styledRecipes) {
            menu.add(recipe);
        }

        popupMenu.show();
    }

    /** Opens the recipe list editor dialog. */
    @UiThread
    void showRecipeEditor() {
        String recipeLines = state.getRecipes();
        RecipeEditorDialogFragment dialog = RecipeEditorDialogFragment.newInstance(recipeLines);

        alarmPeriod.setSelection(0); // workaround unedited EditText w/a selection somehow getting
            // focus & selection when the dialog closes
        defocusTextField(alarmPeriod); // remove the caret
        dialog.show(getSupportFragmentManager(), RecipeEditorDialogFragment.TAG);
    }

    @Override
    @UiThread
    public void onEditorDialogPositiveClick(DialogInterface dialog, @NonNull String text) {
        if (!text.equals(state.getRecipes())) { // optimize the no-change case
            state.setRecipes(text);
            state.save(this);
        }
    }

    @Override
    @UiThread
    public void onEditorDialogNegativeClick(DialogInterface dialog) {
    }

    @SuppressWarnings("unused")
    @UiThread
    public void onDismissRecipeMenu(PopupMenu menu) {
        popupMenu = null;
    }

    @SuppressWarnings("SameReturnValue")
    @UiThread
    public boolean onRecipeMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.edit_recipes) {
            showRecipeEditor();
            return true;
        }

        String itemTitle = item.getTitle().toString();
        int tokenLength = TimeCounter.lengthOfLeadingIntervalTime(itemTitle);
        String token = itemTitle.substring(0, tokenLength);

        alarmPeriod.setText(token);

        // Submit the input whether or not the text field has focus.
        processAlarmPeriodInput();
        return true;
    }

    /** Hides the soft keyboard -- best efforts. */
    // https://stackoverflow.com/a/17789187/1682419
    public static void hideKeyboard(@NonNull Activity activity, @Nullable View v) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);

        if (v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }

        View focussed = activity.getCurrentFocus();
        // TODO: Another fallback: if (focussed == null) focussed = new View(activity);
        if (focussed != null) {
            imm.hideSoftInputFromWindow(focussed.getWindowToken(), 0);
        }
    }

    /** Hides the soft keyboard, if we're lucky. */
    // https://stackoverflow.com/a/17789187/1682419
    public void hideKeyboard(@Nullable View v) {
        hideKeyboard(this, v);
    }

    /**
     * Remove focus from the text field.
     * <p/>
     * NOTE: The alarmPeriod text field has an OnFocusChangeListener. Defocus will trigger the
     * listener to (re)set the field's contents from the current state and close the soft keyboard.
     * <p/>
     * Workaround: Older Android versions grab and hold focus or immediately refocus. So force the
     * text field to defocus by temporarily making it not-focusable.
     * https://stackoverflow.com/a/11044709/1682419
     * <p/>
     * Re-enable focusability after another short delay rather than via `setOnTouchListener()` [in
     * the stackoverflow post] so TAB & arrow keys can still enter/exit the text field.
     * <p/>
     * Setting a caret in the text field, then using the popup menu to set & confirm new contents
     * might make Android log warnings such as:
     *    `W/IInputConnectionWrapper: requestCursorAnchorInfo on inactive InputConnection`
     * Delaying setFocusable(false) by 50ms would reduce those. Is it a net win?
     * <p/>
     * NOTE: The UI test method delayForDefocusTextFieldWorkaround() must wait for this delay.
     */
    @UiThread
    @SuppressLint("ClickableViewAccessibility")
    private void defocusTextField(EditText textField) {
        if (Build.VERSION.SDK_INT <= 27) {
            textField.setFocusable(false);

            textField.postDelayed(() -> {
                textField.setFocusable(true);
                textField.setFocusableInTouchMode(true);
            }, 50);
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

        // Save the state change.
        if (newSeconds > 0 && newSeconds != state.getSecondsPerReminder()) {
            state.setSecondsPerReminder(newSeconds); // clips the value
            state.save(this);
            updateUI(); // update countdownDisplay, notifications, and widgets
        }

        // Normalize the interval time text.
        // [updateUI() does this only if the configuration changed.]
        displayAlarmPeriod();

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
    @SuppressWarnings("unused")
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

    /** Updates the count-up (elapsed) time and alarm count-down time displays. */
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
    void updateViews() {
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
