package com.onefishtwo.bbqtimer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The BBQ Timer's main activity.
 */
public class MainActivity extends ActionBarActivity {

    /** PERSISTENT STATE filename. */
    public static final String TIMER_PREF_FILE = "timer";

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

        /** Schedules the next display update if the timer is running. */
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

    private final TimeCounter timer = new TimeCounter();

    private Button resetButton;
    private Button startStopButton;
    private TextView displayView;
    private TextView fractionDisplayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        resetButton         = (Button) findViewById(R.id.resetButton);
        startStopButton     = (Button) findViewById(R.id.startStopButton);
        displayView         = (TextView) findViewById(R.id.display);
        fractionDisplayView = (TextView) findViewById(R.id.fractionDisplay);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Load persistent state.
        SharedPreferences prefs = getSharedPreferences(TIMER_PREF_FILE, Context.MODE_PRIVATE);
        timer.load(prefs);

        updateUI();

        updateHandler.beginScheduledUpdate();
    }

    @Override
    protected void onPause() {
        // Save persistent state.
        SharedPreferences prefs = getSharedPreferences(TIMER_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        timer.save(prefsEditor);
        prefsEditor.commit();

        super.onPause();
    }

    @Override
    protected void onStop() {
        updateHandler.endScheduledUpdates();
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

    /** The user tapped the Start/Stop button. */
    // TODO: Use listeners to update the Activity UI and app widgets.
    // A Proguard rule keeps all Activity onClick*() methods.
    public void onClickStartStop(View v) {
        timer.toggleRunning();
        updateHandler.beginScheduledUpdate();
        updateUI();
        TimerAppWidgetProvider.updateAllWidgets(this, timer);
    }

    /** The user tapped the Reset button. */
    public void onClickReset(View v) {
        timer.reset();
        updateUI();
        TimerAppWidgetProvider.updateAllWidgets(this, timer);
    }

    /** The user tapped the time text: Cycle Reset -> Running -> Paused -> Reset. */
    public void onClickTimerText(View v) {
        timer.cycle();
        updateHandler.beginScheduledUpdate();
        updateUI();
        TimerAppWidgetProvider.updateAllWidgets(this, timer);
    }

    /** Updates the display to show the current elapsed time. */
    private void displayTime() {
        long elapsedTime         = timer.getElapsedTime();
        String formatted         = TimeCounter.formatHhMmSs(elapsedTime);
        String formattedFraction = TimeCounter.formatFractionalSeconds(elapsedTime);
        int textColorId          = timer.isReset() ? R.color.gray_text : R.color.orange_red_text;
        int textColor            = getResources().getColor(textColorId);

        displayView.setText(formatted);
        fractionDisplayView.setText(formattedFraction);

        displayView.setTextColor(textColor);
        fractionDisplayView.setTextColor(textColor);
    }

    /** Updates the UI for the current state. */
    private void updateUI() {
        boolean running = timer.isRunning();

        displayTime();
        resetButton.setVisibility(running
                || timer.getElapsedTime() == 0 ? View.INVISIBLE : View.VISIBLE);
        startStopButton.setText(running ? R.string.stop : R.string.start);
        startStopButton.setCompoundDrawablesWithIntrinsicBounds(
                running ? R.drawable.ic_pause : R.drawable.ic_play, 0, 0, 0);
    }

}
