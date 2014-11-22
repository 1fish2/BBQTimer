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

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.onefishtwo.bbqtimer.state.ApplicationState;

/**
 * The BBQ Timer's main activity.
 */
public class MainActivity extends ActionBarActivity implements NumberPicker.OnValueChangeListener {
    /** Hide the Stop feature (that distinguishes Stop from Paused) on older OS versions. */
    static final boolean HIDE_STOP_FEATURE = !Notifier.PAUSEABLE_NOTIFICATIONS;

    /** A Handler for periodic display updates. */
    private class UpdateHandler extends Handler {
        private static final int MSG_UPDATE = 1;
        private static final long UPDATE_INTERVAL = 100; // msec

        /** Handles a message to periodically update the display. */
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_UPDATE:
                    displayTime();
                    scheduleNextUpdate();
                    break;
            }
        }

        /** Schedules the next Activity display update if the timer is running. */
        private void scheduleNextUpdate() {
            if (timer.isRunning()) {
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

    private final UpdateHandler updateHandler = new UpdateHandler();
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

        setContentView(R.layout.activity_main);

        resetButton           = (Button) findViewById(R.id.resetButton);
        startStopButton       = (Button) findViewById(R.id.startStopButton);
        stopButton            = (Button) findViewById(R.id.stopButton);
        displayView           = (TextView) findViewById(R.id.display);
        enableRemindersToggle = (CompoundButton) findViewById(R.id.enableReminders);
        minutesPicker         = (NumberPicker) findViewById(R.id.minutesPicker);

        minutesPicker.setMinValue(1);
        minutesPicker.setMaxValue(99);
        minutesPicker.setWrapSelectorWheel(false);
        minutesPicker.setOnValueChangedListener(this);
        minutesPicker.setFocusableInTouchMode(true);
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

        updateUI();

        updateHandler.beginScheduledUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Take focus from minutesPicker's EditText child.
        minutesPicker.requestFocus();
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // TODO: Implement Settings.
            Toast.makeText(this, "Settings are not yet implemented", Toast.LENGTH_SHORT).show();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** The user tapped the Run/Pause button. */
    // TODO: Use listeners to update the Activity UI and app widgets.
    // A Proguard rule keeps all Activity *(View) methods.
    public void onClickStartStop(View v) {
        timer.toggleRunPause();
        updateHandler.beginScheduledUpdate();
        updateUI();
    }

    /** The user tapped the Reset button. */
    public void onClickReset(View v) {
        timer.reset();
        updateUI();
    }

    /** The user tapped the Stop button. */
    public void onClickStop(View v) {
        timer.stop();
        updateUI();
    }

    /** The user tapped the time text: Cycle Reset -> Running -> Paused -> Reset. */
    public void onClickTimerText(View v) {
        timer.cycle();
        updateHandler.beginScheduledUpdate();
        updateUI();
    }

    /** The user clicked the enable/disable periodic-reminders toggle switch/checkbox. */
    public void onClickEnableRemindersToggle(View v) {
        state.setEnableReminders(enableRemindersToggle.isChecked());
        state.save(this);
        AlarmReceiver.updateNotifications(this);
    }

    /** A NumberPicker value changed. */
    @Override
    public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
        if (picker == minutesPicker) {
            state.setSecondsPerReminder(newVal * 60);
            state.save(this);
            AlarmReceiver.updateNotifications(this);
        }
    }

    /** Updates the display of the elapsed time. */
    private void displayTime() {
        Spanned formatted         = timer.formatHhMmSsFraction();
        int textColorsId          =
                timer.isRunning() ? R.color.running_timer_colors
                : timer.isReset() ? R.color.reset_timer_colors
                : R.color.paused_timer_colors;
        ColorStateList textColors = getResources().getColorStateList(textColorsId);

        displayView.setText(formatted);
        displayView.setTextColor(textColors);
    }

    /** Updates the whole UI for the current state: Activity, Notifications, alarms, and widgets. */
    private void updateUI() {
        boolean isRunning = timer.isRunning();

        displayTime();
        resetButton.setVisibility(isRunning || timer.isReset() ? View.INVISIBLE : View.VISIBLE);
        startStopButton.setCompoundDrawablesWithIntrinsicBounds(
                isRunning ? R.drawable.ic_action_pause : R.drawable.ic_action_play, 0, 0, 0);
        stopButton.setVisibility(HIDE_STOP_FEATURE ? View.GONE
                : timer.isStopped() ? View.INVISIBLE
                : View.VISIBLE);
        enableRemindersToggle.setChecked(state.isEnableReminders());
        minutesPicker.setValue(state.getSecondsPerReminder() / 60);

        AlarmReceiver.updateNotifications(this);

        TimerAppWidgetProvider.updateAllWidgets(this, state);
    }

}
