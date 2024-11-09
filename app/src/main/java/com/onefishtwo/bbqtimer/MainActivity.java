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

import static android.Manifest.permission.POST_NOTIFICATIONS;

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
import android.os.StrictMode;
import android.provider.Settings;
import android.text.Editable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.TextViewCompat;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.onefishtwo.bbqtimer.state.ApplicationState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.Vector;

/**
 * The BBQ Timer's main activity.
 */
public class MainActivity extends AppCompatActivity
        implements RecipeEditorDialogFragment.RecipeEditorDialogFragmentListener {
    private static final String TAG = "Main";

    /**
     * Enable edge-to-edge display? Required on API 35+ (with a temporary deferment option).
     * On API < 29, it breaks the system bar contrast. On API < 35 the system bar is ugly above the
     * title bar (fixable). The PopupMenu insets need investigation. No advantages for this app.
     */
    private static final boolean EDGE_TO_EDGE = Build.VERSION.SDK_INT >= 35;

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
        Intent activityIntent = new Intent(Intent.ACTION_MAIN, null, context, MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context)
                .addParentStack(MainActivity.class)
                .addNextIntent(activityIntent);
        return stackBuilder.getPendingIntent(0,
                PendingIntent.FLAG_UPDATE_CURRENT + PendingIntent.FLAG_IMMUTABLE);
    }

    /**
     * A Handler for periodic display updates.
     * <p>
     * Queued Messages refer to the Handler which refers to the Activity. Since
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
    private int notificationRequestCount;

    private ConstraintLayout mainContainer;
    private Button resetButton;
    private Button pauseResumeButton;
    private Button stopButton;
    private TextView countUpDisplay, countdownDisplay;
    private EditText2 alarmPeriod;
    private CheckBox enableReminders;

    // This callback handles the user's response to the system permissions dialog.
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    isGranted -> {
                        if (isGranted) {
                            Log.w(TAG, "Permission to Notify was granted");
                            AlarmReceiver.updateNotifications(this);
                        } else {
                            // The user tapped "Don't allow" *OR* dismissed the dialog.
                            Log.w(TAG, "Permission to Notify was denied");
                        }
                    });

    TimeCounter getTimer() {
        return timer;
    }

    @MainThread
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (EDGE_TO_EDGE) {
            EdgeToEdge.enable(this);
        }
        super.onCreate(savedInstanceState);

        if (BBQTimerApplication.STRICT_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build());
        }

        viewConfiguration = -1;
        notifier = new Notifier(this);
        lastRecipes = "";
        styledRecipes = new Vector<>(20);
        popupMenu = null;
        notificationRequestCount = 0;

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

        if (EDGE_TO_EDGE) {
            ViewCompat.setOnApplyWindowInsetsListener(mainContainer, this::mainWindowInsetsListener);
        }

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
            // ACTION_MAIN from a Widget or Notification
            // ACTION_EDIT from AlarmManager.AlarmClockInfo()
            // whatever with category.LAUNCHER
        }

        logTheConfiguration(getResources().getConfiguration());
    }

    /**
     * Set the window insets policy for edge-to-edge display, as done in
     * developer.android.com/develop/ui/views/layout/edge-to-edge#system-bars-insets
     * </p>
     * systemBars() includes statusBars, captionBar, and navigationBars, but not ime.
     * TODO: Include systemGestures()? displayCutout()?
     * </p>
     * @noinspection SameReturnValue
     */
    private WindowInsetsCompat mainWindowInsetsListener(
            @NonNull View view, @NonNull WindowInsetsCompat windowInsets) {
        @NonNull Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

        if (layoutParams != null) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) layoutParams;

            mlp.setMargins(insets.left, insets.top, insets.right, insets.bottom);
            view.setLayoutParams(mlp);
        }

        return WindowInsetsCompat.CONSUMED; // don't pass windowInsets to nested Views
    }

    private void logTheConfiguration(@NonNull Configuration config) {
        Log.i(TAG,
            String.format("Config densityDpi: %d, size DPI: %dx%d, orientation: %d",
                    config.densityDpi,
                    config.screenWidthDp, config.screenHeightDp, // Android 15+ includes system bars
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
        if (shortcutAction != SHORTCUT_NONE) {
            state.save(this);
        }
        shortcutAction = SHORTCUT_NONE;

        updateUI();

        updateHandler.beginScheduledUpdate();
    }

    @MainThread
    @Override
    protected void onResume() {
        super.onResume();

        // Warn if the Alarm is now muted. Don't check Notifications permission because (1) the
        // API 33 Allow/Deny dialog can loop returning !isGranted and re-resuming, and (2) Android's
        // new UI model is to wait for a user action before checking its needed permissions.
        informIfAlarmsMuted();
    }

    /** The Activity is no longer visible. */
    @MainThread
    @Override
    protected void onStop() {
        updateHandler.endScheduledUpdates();

        dismissPopupMenu();

        super.onStop();
    }

    /** Opens the OS UI to request user permission to post pull-down notifications. */
    @UiThread
    private void openOsNotificationsPermissionRequest() {
        if (Build.VERSION.SDK_INT < 33) {
            openNotificationSettingsForApp();
        } else {
            // NOTE: After two user denials, this OS dialog will auto-deny further requests.
            requestPermissionLauncher.launch(POST_NOTIFICATIONS);
        }
    }

    /**
     * Requests permission to post pull-down notifications to the user. Either show rationale of why
     * notifications are needed and offer to help, or show a short message and offer to help, or
     * open the OS UI straightaway, or give up and stop pestering.
     *<p/>
     * NOTE: Even w/o permission the app creates notifications, in which case they're hidden but
     * maybe still needed for a Foreground Service.
     *<p/>
     * NOTE: If notifications are disabled, so are Toasts.
     */
    @UiThread
    private void requestNotificationsPermission() {
        String logMessage;
        @StringRes int resId;

        if (Build.VERSION.SDK_INT < 33
                || shouldShowRequestPermissionRationale(POST_NOTIFICATIONS)) {
            if (notificationRequestCount < 3) {
                logMessage = "explain why needed and offer to enable";
                resId = R.string.notifications_permission_needed;
            } else if (notificationRequestCount < 6) {
                logMessage = "offer to enable";
                resId = R.string.notifications_disabled;
            } else {
                Log.w(TAG, "Notifications are disabled; keep quiet");
                return;
            }
        } else {
            Log.w(TAG, "Ask permission to Notify");
            openOsNotificationsPermissionRequest();
            return;
        }

        Snackbar snackbar = makeSnackbar(resId);

        Log.w(TAG, "Notifications are disabled; " + logMessage);
        setSnackbarAction(snackbar, R.string.notifications_enable,
                view -> openOsNotificationsPermissionRequest());
        snackbar.show();
        ++notificationRequestCount;
    }

    /**
     * Informs the user if the app needs Notifications permission or if the Alarm channel is muted
     * or misconfigured, and offers to help. (Without Notifications permission, checking the channel
     * would probably be confusing and unhelpful.) The app uses Notifications for pull-down
     * controls, lock screen controls, and periodic alarms.
     */
    @UiThread
    private void informIfAlarmsDeniedOrMuted() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (notificationManager.areNotificationsEnabled()) {
            notificationRequestCount = 0;

            informIfAlarmsMuted();
        } else {
            requestNotificationsPermission();
        }
    }

    /**
     * Informs the user if the Alarm notification channel is muted or misconfigured and offers to
     * help, BUT does nothing if the app needs Notifications permission (in which case the channel
     * configuration doesn't matter and probably can't be fixed) or if periodic reminder alarms are
     * turned off.
     * <p/>
     * TODO: How to detect if the app's notifications are visible but "silenced"? Silencing kills
     * the audio and heads-up notification shades.
     */
    @UiThread
    private void informIfAlarmsMuted() {
        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);

        if (!notificationManager.areNotificationsEnabled() || !state.isEnableReminders()) {
            return;
        }

        final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (am != null) {
            int volume = am.getStreamVolume(AudioManager.STREAM_ALARM);

            // Check for muted alarms.
            if (volume <= 0) {
                Snackbar snackbar = makeSnackbar(R.string.alarm_muted);

                Log.w(TAG, "App Notifications sounds are muted");
                setSnackbarAction(snackbar, R.string.alarm_unmute,
                        view -> am.adjustStreamVolume(AudioManager.STREAM_ALARM,
                                    AudioManager.ADJUST_RAISE, 0));
                snackbar.show();
                return;
            }
        }

        // Check for misconfigured Alarm notification channel. This works only on API 26+.
        if (!notifier.isAlarmChannelOK()) {
            Snackbar snackbar = makeSnackbar(R.string.notifications_misconfigured);

            Log.w(TAG, "The app's Notifications channel is misconfigured");
            if (Build.VERSION.SDK_INT >= 26) { // Where this Settings Intent works.
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
            informIfAlarmsDeniedOrMuted();
        }
    }

    /** The user tapped the Reset button; go to Paused at 0:00. */
    @UiThread
    @SuppressWarnings("UnusedParameters")
    public void onClickReset(View v) {
        defocusTextField(alarmPeriod);

        timer.reset();
        updateHandler.beginScheduledUpdate();
        updateUI();

        informIfAlarmsDeniedOrMuted();
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
            informIfAlarmsDeniedOrMuted();
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
            informIfAlarmsDeniedOrMuted();
        }
    }

    /** The user clicked the button to open the "recipes" menu of alarm periods. */
    @UiThread
    public void onClickRecipeMenuButton(View v) {
        popupMenu = new PopupMenu(this, v, Gravity.CENTER, 0, R.style.PopupMenu);
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

    /** Dismiss any popup menu.
     * </p>
     * ISSUE: Rotating the screen with a popup menu open throws
     * "android.view.WindowLeaked leaked window android.widget.PopupWindow$PopupDecorView".
     * It doesn't seem fixable short of reimplementing PopupMenu or handling screen rotations
     * manually. But it doesn't seem to matter other than logging an error.
     */
    @UiThread
    private void dismissPopupMenu() {
        if (popupMenu != null) {
            popupMenu.dismiss();
            popupMenu = null;
        }
    }

    @SuppressWarnings("unused")
    @UiThread
    public void onDismissRecipeMenu(PopupMenu menu) {
        if (menu != null) {
            menu.setOnMenuItemClickListener(null);
            menu.setOnDismissListener(null);
        }
        popupMenu = null;
    }

    @SuppressWarnings("SameReturnValue")
    @UiThread
    public boolean onRecipeMenuItemClick(MenuItem item) {
        if (item.getItemId() == R.id.edit_recipes) {
            showRecipeEditor();
            return true;
        }

        CharSequence titleChars = item.getTitle();
        String itemTitle = titleChars == null ? "" : titleChars.toString();
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
     * <a href="https://stackoverflow.com/a/11044709/1682419">Stack Overflow</a>
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
     * <a href="https://stackoverflow.com/a/29470242/1682419">Stack Overflow</a>
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

            setDrawableRes(resetButton, isStopped ? R.drawable.ic_pause : R.drawable.ic_replay);
            resetButton.setVisibility(isRunning || isPausedAt0 ? View.INVISIBLE : View.VISIBLE);
            setDrawableRes(pauseResumeButton, isRunning ? R.drawable.ic_pause : R.drawable.ic_play);
            setDrawableRes(stopButton, R.drawable.ic_stop);
            stopButton.setVisibility(isStopped ? View.INVISIBLE : View.VISIBLE);
            countdownDisplay.setVisibility(areRemindersEnabled ? View.VISIBLE : View.INVISIBLE);
            enableReminders.setChecked(areRemindersEnabled);
            displayAlarmPeriod();
        }
    }

    /** Set the left drawable of a Button (or any TextView). Tag it with the resId for testing. */
    private static void setDrawableRes(@NonNull TextView view, @DrawableRes int resId) {
        view.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0);
        view.setTag(resId);
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

        if (Build.VERSION.SDK_INT >= 26) {
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
        Intent intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS);

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
