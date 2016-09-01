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

import android.content.Context;
import android.content.res.ColorStateList;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.ColorRes;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Spanned;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.TextView;

import com.onefishtwo.bbqtimer.state.ApplicationState;

import java.lang.ref.WeakReference;

/**
 * The BBQ Timer's main activity.
 */
public class MainActivity extends AppCompatActivity implements NumberPicker.OnValueChangeListener {
    /**
     * Hide the Reset feature (Pause @ 0:00) if the app doesn't show notifications while paused
     * (that's on Android versions without lock screen notifications). Just use Stop.
     */
    private static final boolean HIDE_RESET_FEATURE = !Notifier.PAUSEABLE_NOTIFICATIONS;

    private static final MinutesChoices minutesChoices = new MinutesChoices();

    /** (Re)makes all locale-dependent strings. */
    private static void makeLocaleStrings() {
        minutesChoices.makeLocaleStrings();
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
        private final WeakReference<MainActivity> weakActivity;

        private UpdateHandler(MainActivity activity) {
            weakActivity = new WeakReference<>(activity);
        }

        /** Handles a message to periodically update the display. */
        @Override
        public void handleMessage(Message msg) {
            MainActivity activity = weakActivity.get();

            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE:
                    if (activity != null) {
                        activity.displayTime();
                    }
                    scheduleNextUpdate();
                    break;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        makeLocaleStrings();
        setContentView(R.layout.activity_main);

        resetButton           = (Button) findViewById(R.id.resetButton);
        startStopButton       = (Button) findViewById(R.id.startStopButton);
        stopButton            = (Button) findViewById(R.id.stopButton);
        displayView           = (TextView) findViewById(R.id.display);
        enableRemindersToggle = (CompoundButton) findViewById(R.id.enableReminders);
        minutesPicker         = (NumberPicker) findViewById(R.id.minutesPicker);

        minutesPicker.setMinValue(0);
        minutesPicker.setMaxValue(minutesChoices.choices.length - 1);
        minutesPicker.setDisplayedValues(minutesChoices.choices);
        minutesPicker.setWrapSelectorWheel(false);
        minutesPicker.setOnValueChangedListener(this);
        minutesPicker.setFocusableInTouchMode(true);

        setVolumeControlStream(AudioManager.STREAM_ALARM);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Inflate the menu; this adds items to the action bar if it is present.
        // TODO: Inflate the menu once it has useful items:
        // getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /** The Activity is now visible. */
    @Override
    protected void onStart() {
        super.onStart();

        // Load persistent state.
        state = ApplicationState.sharedInstance(this);
        timer = state.getTimeCounter();
        state.setMainActivityIsVisible(true);
        state.save(this);

        updateUI();

        updateHandler.beginScheduledUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Take focus from minutesPicker's EditText child.
        minutesPicker.requestFocus();

        informIfAudioMuted();
    }

    /** The Activity is no longer visible. */
    @Override
    protected void onStop() {
        updateHandler.endScheduledUpdates();

        // Update persistent state.
        state.setMainActivityIsVisible(false);
        state.save(this);

        AlarmReceiver.updateNotifications(this); // after setMainActivityIsVisible()

        super.onStop();
    }

    /** Informs the user if the alarm audio is muted, offering an UNMUTE option. */
    private void informIfAudioMuted() {
        final AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int volume = am.getStreamVolume(AudioManager.STREAM_ALARM);

        // Only warn when reminders are enabled, not when running silently.
        if (!state.isEnableReminders()) {
            return;
        }

        if (volume <= 0) {
            Snackbar snackbar = Snackbar.make(findViewById(R.id.main_container),
                        R.string.alarm_muted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.alarm_unmute, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            am.adjustStreamVolume(AudioManager.STREAM_ALARM,
                                    AudioManager.ADJUST_RAISE, 0);
                        }
                    })
                    .setActionTextColor(ContextCompat.getColor(this, R.color.contrasting_text));
            snackbar.getView().setBackgroundColor(
                    ContextCompat.getColor(this, R.color.dark_orange_red));
            snackbar.show();
        }
    }

    /** The user tapped the Run/Pause button (named "StartStop"). */
    // TODO: Use listeners to update the Activity UI and app widgets.
    // A Proguard rule keeps all Activity *(View) methods.
    @SuppressWarnings("UnusedParameters")
    public void onClickStartStop(View v) {
        timer.toggleRunPause();
        updateHandler.beginScheduledUpdate();
        updateUI();
    }

    /** The user tapped the Reset button; go to Paused at 0:00. */
    @SuppressWarnings("UnusedParameters")
    public void onClickReset(View v) {
        timer.reset();
        updateHandler.beginScheduledUpdate();
        updateUI();
    }

    /** The user tapped the Stop button. */
    @SuppressWarnings("UnusedParameters")
    public void onClickStop(View v) {
        timer.stop();
        updateHandler.endScheduledUpdates();
        updateUI();
    }

    /** The user tapped the time text: Cycle Reset -> Running -> Paused -> Reset. */
    @SuppressWarnings("UnusedParameters")
    public void onClickTimerText(View v) {
        timer.cycle();
        updateHandler.beginScheduledUpdate();
        updateUI();
    }

    /** The user clicked the enable/disable periodic-reminders toggle switch/checkbox. */
    @SuppressWarnings("UnusedParameters")
    public void onClickEnableRemindersToggle(View v) {
        state.setEnableReminders(enableRemindersToggle.isChecked());
        state.save(this);
        AlarmReceiver.updateNotifications(this);
        informIfAudioMuted();
    }

    /** A NumberPicker value changed. */
    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        if (picker == minutesPicker) {
            state.setSecondsPerReminder(minutesChoices.pickerChoiceToSeconds(newVal));
            state.save(this);
            AlarmReceiver.updateNotifications(this);
        }
    }

    /** @return a ColorStateList resource ID; time-dependent for blinking. */
    // TODO: In isPausedAt0(), the Pressed state should be green ("go") like reset_timer_colors.
    @ColorRes
    private int pausedTimerColors() {
        TimeCounter timeCounter = state.getTimeCounter();
        long millis = timeCounter.elapsedRealtimeClock() - timeCounter.getPauseTime();
        long seconds = millis / 1000L;

        return (seconds & 1) == 0 ? R.color.paused_alternate_timer_colors
                : R.color.paused_timer_colors;
    }

    /** Updates the display of the elapsed time. */
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

    /** Updates the whole UI for the current state: Activity, Notifications, alarms, and widgets. */
    private void updateUI() {
        boolean isRunning = timer.isRunning();
        boolean isStopped = timer.isStopped();
        boolean isPausedAt0 = timer.isPausedAt0();

        displayTime();
        resetButton.setCompoundDrawablesWithIntrinsicBounds(
                isStopped ? R.drawable.ic_pause : R.drawable.ic_replay, 0, 0, 0);
        resetButton.setVisibility(HIDE_RESET_FEATURE ? View.GONE
                : isRunning || isPausedAt0 ? View.INVISIBLE
                : View.VISIBLE);
        startStopButton.setCompoundDrawablesWithIntrinsicBounds(
                isRunning ? R.drawable.ic_pause : R.drawable.ic_play, 0, 0, 0);
        stopButton.setVisibility(isStopped ? View.INVISIBLE : View.VISIBLE);
        enableRemindersToggle.setChecked(state.isEnableReminders());
        minutesPicker.setValue(minutesChoices.secondsToPickerChoice(
                state.getSecondsPerReminder()));

        AlarmReceiver.updateNotifications(this);

        TimerAppWidgetProvider.updateAllWidgets(this, state);
    }

}
