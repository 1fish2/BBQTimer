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
import android.app.Activity;
import android.app.KeyguardManager;
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
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.IntDef;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.PopupMenu;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.splashscreen.SplashScreen;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.widget.NestedScrollView;
import androidx.core.widget.TextViewCompat;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputLayout;
import com.onefishtwo.bbqtimer.state.ApplicationState;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * The BBQ Timer's main activity.
 */
@SuppressWarnings("OverlyComplexClass")
public class MainActivity extends AppCompatActivity
        implements RecipeEditorDialogFragment.RecipeEditorDialogFragmentListener {
    private static final String TAG = "Main";

    /** Enable edge-to-edge display? It's required on API 35+. Its problems on API < 29 might be
     *  fixed now but there's no strong need to test and debug it thoroughly on Android < Pie. */
    private static final boolean EDGE_TO_EDGE = Build.VERSION.SDK_INT >= 29;
    public static final int REMINDER_STREAM = AudioManager.STREAM_ALARM;

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
    private ArrayList<SpannableString> styledRecipes; // the output from styleTheRecipes()
    private PopupMenu popupMenu;
    private MenuItem lockStatusItem; // the "phone is locked" action bar item
    private int notificationRequestCount;

    private NestedScrollView mainContainer;
    private Button resetButton;
    private Button pauseResumeButton;
    private Button stopButton;
    private TextView countUpDisplay, countdownDisplay;
    private EditText2 alarmPeriod;
    private CheckBox enableReminders;
    private SpringAnimation springX, springY;

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
        // Implement the (backward-compatible) Splash Screen, and keep it open until the
        // ApplicationState finishes loading.
        // ASSUMES: BBQTimerApplication initiated loading the ApplicationState.
        SplashScreen splashScreen = SplashScreen.installSplashScreen(this);
        splashScreen.setKeepOnScreenCondition(() -> !ApplicationState.isLoaded());

        if (EDGE_TO_EDGE) {
            EdgeToEdge.enable(this);
        }
        super.onCreate(savedInstanceState);

        viewConfiguration = -1;
        notifier = new Notifier(this);
        lastRecipes = "";
        styledRecipes = new ArrayList<>(20);
        popupMenu = null;
        notificationRequestCount = 0;

        // View Binding has potential, but it makes project inspections create a lot of spurious
        // warnings about unused resource IDs, methods, and method arguments.
        //ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater())
        //mainContainer = binding.getRoot()
        //setContentView(mainContainer)
        setContentView(R.layout.activity_main);
        mainContainer = findViewById(R.id.main_container);

        resetButton = findViewByIdAndSetTooltip(R.id.resetButton);
        pauseResumeButton = findViewByIdAndSetTooltip(R.id.pauseResumeButton);
        stopButton = findViewByIdAndSetTooltip(R.id.stopButton);
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

        setEdgeToEdgeWindowInsetsListener(mainContainer);

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

        setVolumeControlStream(REMINDER_STREAM);

        // Trigger any App Shortcut action but only in the initial launch, not after screen
        // rotation, a theme change, enter/exit multi-window mode, etc.
        shortcutAction = SHORTCUT_NONE;
        if (savedInstanceState == null) {
            Intent callingIntent = getIntent();
            if (callingIntent != null) {
                String action = callingIntent.getAction(); // null action occurred in multi-window testing
                if (Intent.ACTION_QUICK_CLOCK.equals(action)) { // App Shortcut: Pause @ 00:00
                    shortcutAction = SHORTCUT_PAUSE;
                } else if (Intent.ACTION_RUN.equals(action)) { // App Shortcut: Start @ 00:00
                    shortcutAction = SHORTCUT_START;
                }
                Log.i(TAG, "Shortcut Action " + shortcutAction + ", Intent: " + callingIntent);
                // ACTION_MAIN from a Widget or Notification
                // ACTION_EDIT from AlarmManager.AlarmClockInfo()
                // whatever with category.LAUNCHER
            }
        }

        logTheConfiguration(getResources().getConfiguration());
    }

    /** Finds a View and sets its Tooltip to match its ContentDescription. */
    <T extends View> T findViewByIdAndSetTooltip(@IdRes int id) {
        T view = super.findViewById(id);

        if (view != null) {
            TooltipCompat.setTooltipText(view, view.getContentDescription());
        }
        return view;
    }

    private void setContentDescriptionAndTooltip(View view, @StringRes int resId) {
        CharSequence desc = getText(resId);

        view.setContentDescription(desc);
        TooltipCompat.setTooltipText(view, desc);
    }

    /**
     * Sets a WindowInsetsListener on the root View when in edge-to-edge mode to adjust its margins
     * to accommodate system bars, display cutouts, and the IME.
     *
     * @param rootView The layout's root {@link View}.
     */
    static void setEdgeToEdgeWindowInsetsListener(@NonNull View rootView) {
        // EdgeToEdge.enable(this) is already called in onCreate()
        if (EDGE_TO_EDGE) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView,
                    MainActivity::mainWindowInsetsListener);
        }
    }

    /**
     * Set the window insets policy for edge-to-edge display, as done in
     * developer.android.com/develop/ui/views/layout/edge-to-edge#system-bars-insets
     * </p>
     * @noinspection SameReturnValue
     */
    static @NonNull WindowInsetsCompat mainWindowInsetsListener(
            @NonNull View view, @NonNull WindowInsetsCompat windowInsets) {
        @NonNull Insets insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() // status, caption, & nav bars
                        | WindowInsetsCompat.Type.displayCutout()
                        | WindowInsetsCompat.Type.ime());
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

        getMenuInflater().inflate(R.menu.main, menu);
        lockStatusItem = menu.findItem(R.id.action_lock_status);
        updateLockStatus();
        return true;
    }

    /**
     * Shows/hides a "phone is (still) locked" icon in the action bar so the user doesn't worry
     * about security. It's surprising that the app can open from the lock screen notification or
     * widget without unlocking, and that turning the screen off then on shows the app without
     * unlocking the phone. Android doesn't reveal that the app is now "on top of the lock screen."
     */
    @UiThread
    private void updateLockStatus() {
        if (lockStatusItem == null) {
            return;
        }

        KeyguardManager myKM = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        boolean isLocked = myKM != null && myKM.isKeyguardLocked();

        lockStatusItem.setVisible(isLocked);
    }

    /**
     * For the pop-up menu, convert state.getRecipes() into SpannableStrings in styledRecipes.
     * This is idempotent and fast if the input hasn't changed.
     * <p/>
     * INPUTS: state.getRecipes().<p/>
     * OUTPUTS: the styledRecipes List.
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

            ss.setSpan(new StyleSpan(Typeface.ITALIC), tokenLength, recipeLength,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
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
        if (shortcutAction != SHORTCUT_NONE) {
            Log.i(TAG, "Applying App Shortcut Action " + shortcutAction);
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
            state.save(this);
            shortcutAction = SHORTCUT_NONE;
        }

        updateUI();
        updateLockStatus();

        updateHandler.beginScheduledUpdate();
    }

    @MainThread
    @Override
    protected void onResume() {
        super.onResume();

        updateLockStatus();

        // WORKAROUND: On API ≤ 27, the alarmPeriod EditText autofocuses when the Activity starts
        // in landscape mode or is rotated to landscape. That's annoying.
        // (A previous workaround set android:focusable="true",  android:focusableInTouchMode="true"
        // on an empty or outer layout view, but the empty one no longer works in API 25-27 and the
        // outer one no longer works in API 25 and 27.)
        if (Build.VERSION.SDK_INT <= 27) {
            defocusTextField(alarmPeriod);
        }

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
        cancelPauseResumeAnimations();

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
     * might still be needed if the app gets a Foreground Service.
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
     * help. BUT this does nothing if the app needs Notifications permission (in which case the
     * channel configuration doesn't matter and probably can't be fixed) or if periodic reminder
     * alarms are turned off.
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
            int volume = am.getStreamVolume(REMINDER_STREAM);

            // Check for muted alarms.
            if (volume <= 0) {
                Snackbar snackbar = makeSnackbar(R.string.alarm_muted);

                Log.w(TAG, "App Notifications sounds are muted");
                setSnackbarAction(snackbar, R.string.alarm_unmute,
                        view -> {
                            try {
                                am.adjustStreamVolume(REMINDER_STREAM,
                                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                            } catch (SecurityException e) {
                                Log.w(TAG, "Couldn't unmute directly: " + e);
                                openVolumeSettings();
                            }
                        });
                snackbar.show();
                return;
            }
        }

        // Check for misconfigured Alarm notification channel. This works only on API 26+.
        if (!notifier.isAlarmChannelOK()) {
            Snackbar snackbar = makeSnackbar(R.string.notifications_misconfigured);

            Log.w(TAG, "The app's Notifications channel is misconfigured");
            if (Build.VERSION.SDK_INT >= 26) { // Where this Intent works.
                setSnackbarAction(snackbar, R.string.notifications_configure,
                        view -> openNotificationChannelSettings(
                                Notifier.ALARM_NOTIFICATION_CHANNEL_ID));
            }
            snackbar.show();
        }
    }

    /**
     * Opens the Volume Settings panel (API 29+) or the system sound settings (API < 29).
     */
    private void openVolumeSettings() {
        Intent intent = new Intent();

        if (Build.VERSION.SDK_INT >= 29) {
            intent.setAction(Settings.Panel.ACTION_VOLUME);
        } else {
            intent.setAction(Settings.ACTION_SOUND_SETTINGS);
        }

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(TAG, "Couldn't open volume/sound Settings: " + e);
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
     * be settable in Theme.App but why bother?
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

    /** Called when the Activity's Window gains or loses focus. */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        updateLockStatus();
    }

    /** The user tapped a Run/Pause action. */
    @UiThread
    @SuppressWarnings("UnusedParameters")
    public void onClickPauseResume(View v) {
        defocusTextField(alarmPeriod);

        timer.toggleRunPause();
        updateHandler.beginScheduledUpdate();
        saveStateAndUpdateUI();

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
        saveStateAndUpdateUI();

        informIfAlarmsDeniedOrMuted();
    }

    /** The user tapped the Stop button. */
    @UiThread
    @SuppressWarnings("UnusedParameters")
    public void onClickStop(View v) {
        defocusTextField(alarmPeriod);

        timer.stop();
        updateHandler.endScheduledUpdates();
        saveStateAndUpdateUI();
    }

    /** The user tapped the time text: Cycle Stopped | Reset -> Running -> Paused -> Stopped. */
    @UiThread
    @SuppressWarnings("UnusedParameters")
    public void onClickTimerText(View v) {
        defocusTextField(alarmPeriod);

        timer.cycle();
        updateHandler.beginScheduledUpdate();
        saveStateAndUpdateUI();

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
        saveStateAndUpdateUI();

        if (state.isEnableReminders()) {
            informIfAlarmsDeniedOrMuted();
        }
    }

    /** The user clicked the button to open the "recipes" menu of alarm periods. */
    @UiThread
    public void onClickRecipeMenuButton(View v) {
        // Workaround: Gravity.END places the PopupMenu out of the right side system gesture and
        // cutout areas. It might still extend into the left side system gesture area in
        // portrait mode, but at least it won't be hidden by a cutout in landscape mode.
        popupMenu = new PopupMenu(this, v, Gravity.END, 0, R.style.PopupMenu);
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

        // Submit the input whether the text field has focus or not.
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
            saveStateAndUpdateUI(); // update countdownDisplay, notifications, and widgets
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
        Spanned formatted = timer.formatHhMmSsFraction();
        @ColorRes int textColorsId = switch (timer.getState()) {
            case RUNNING -> R.color.running_timer_colors;
            case PAUSED -> pausedTimerColors();
            default -> R.color.reset_timer_colors;
        };
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
            boolean isFirstConfiguration = viewConfiguration == -1;
            viewConfiguration = newConfiguration;

            if (isRunning || isPausedAt0) {
                resetButton.setVisibility(View.INVISIBLE);
            } else {
                resetButton.setVisibility(View.VISIBLE);
                setDrawableRes(resetButton, isStopped ? R.drawable.ic_pause : R.drawable.ic_replay);
                setContentDescriptionAndTooltip(resetButton, isStopped ? R.string.pause : R.string.reset);
            }

            int pauseResumeIconId = isRunning ? R.drawable.ic_pause : R.drawable.ic_play;
            int pauseResumeDescId = isRunning ? R.string.pause : R.string.start;
            // NOTE: This changes the ContentDescription, leaving the Tooltip = "Run/Pause", which
            // should be more helpful with a stable description but changes on screen readers.
            // TODO: Reconsider. The Pause/Reset button is handled differently, above.
            pauseResumeButton.setContentDescription(getString(pauseResumeDescId));

            if (isFirstConfiguration) {
                setDrawableRes(pauseResumeButton, pauseResumeIconId);
            } else {
                animatePauseResumeIcon(pauseResumeIconId);
            }

            setDrawableRes(stopButton, R.drawable.ic_stop);
            stopButton.setVisibility(isStopped ? View.INVISIBLE : View.VISIBLE);
            countdownDisplay.setVisibility(areRemindersEnabled ? View.VISIBLE : View.INVISIBLE);
            enableReminders.setChecked(areRemindersEnabled);
            displayAlarmPeriod();
        }
    }

    /**
     * Checks if "Reduced Motion" is enabled in system settings.
     */
    private boolean isReducedMotionEnabled() {
        try {
            float animatorScale = Settings.Global.getFloat(
                    getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f);
            return animatorScale <= 0.0f;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Animates the pause/resume button icon change using a SpringAnimation.
     */
    private void animatePauseResumeIcon(@DrawableRes int resId) {
        Object currentTag = pauseResumeButton.getTag();
        boolean sameIcon = currentTag instanceof Integer && (Integer) currentTag == resId;

        if (sameIcon) {
            return;
        }

        cancelPauseResumeAnimations();

        // Fallback to static change if reduced motion is enabled.
        if (isReducedMotionEnabled()) {
            setDrawableRes(pauseResumeButton, resId);
            return;
        }

        // Expressive animation: Scale down slightly, change icon, then spring back with overshoot.
        pauseResumeButton.animate()
                .scaleX(0.7f)
                .scaleY(0.7f)
                .setDuration(80)
                .withEndAction(() -> {
                    setDrawableRes(pauseResumeButton, resId);

                    springX = new SpringAnimation(pauseResumeButton,
                            DynamicAnimation.SCALE_X, 1.0f);
                    springY = new SpringAnimation(pauseResumeButton,
                            DynamicAnimation.SCALE_Y, 1.0f);

                    SpringForce springForce = new SpringForce(1.0f)
                            .setDampingRatio(SpringForce.DAMPING_RATIO_MEDIUM_BOUNCY)
                            .setStiffness(SpringForce.STIFFNESS_MEDIUM);

                    springX.setSpring(springForce);
                    springY.setSpring(springForce);

                    // Prevent sub-pixel background frame updates.
                    springX.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);
                    springY.setMinimumVisibleChange(DynamicAnimation.MIN_VISIBLE_CHANGE_SCALE);

                    springX.start();
                    springY.start();
                })
                .start();
    }

    /**
     * Cancels any running pause/resume button animations to prevent leaks and glitches.
     */
    private void cancelPauseResumeAnimations() {
        pauseResumeButton.animate().cancel();
        if (springX != null) {
            springX.cancel();
        }
        if (springY != null) {
            springY.cancel();
        }

        // Ensure button scale is restored if animation was interrupted mid-flight
        pauseResumeButton.setScaleX(1.0f);
        pauseResumeButton.setScaleY(1.0f);
    }

    /**
     * Set the left drawable of a Button (or any TextView); tag it with the resId for testing and to
     * avoid redundant animations.
     */
    private static void setDrawableRes(@NonNull TextView view, @DrawableRes int resId) {
        view.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0);
        view.setTag(resId);
    }

    /** Updates the whole UI for the current state: Notifications, alarms, and widgets. */
    @UiThread
    private void updateUI() {
        updateViews();

        AlarmReceiver.updateNotifications(this);

        TimerAppWidgetProvider.updateAllWidgets(this, state);
    }

    /**
     * Saves app state then updates the UI.
     * </p>
     * TODO: Do all the load()/save() work in a background thread. Meanwhile, update the display
     * first since save() might take a couple hundred ms.
     */
    @UiThread
    private void saveStateAndUpdateUI() {
        state.save(this);
        updateUI();
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
    @RequiresApi(26)
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
