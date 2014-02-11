package com.onefishtwo.bbqtimer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * The BBQ Timer's main activity.
 * </p>
 * TODO: Activity lifecycle.
 * TODO: Add a landscape orientation layout. Handle configuration changes.
 * TODO: Handle Android sleep modes, suspending the app, etc. Put the TimeCounter in a Service.
 * TODO: Implement a widget for the home and lock screens.
 * TODO: Create app icons.
 * TODO: Add alarms. Use or remove the Settings menu.
 * TODO: Thumbnail.
 */
public class MainActivity extends ActionBarActivity {

    /** A Handler for periodic display updates. */
    private class UpdateHandler extends Handler {
        private static final int MSG_UPDATE = 1;
        private static final long UPDATE_INTERVAL = 100; // msec

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
        public void scheduleNextUpdate() {
            if (timer.isRunning()) {
                // TODO: Any race conditions that could end the update messages?
                // TODO: Maybe use sendEmptyMessageAtTime() for drift-free scheduling.
                sendEmptyMessageDelayed(MSG_UPDATE, UPDATE_INTERVAL);
            }
        }
    }

    private final UpdateHandler updateHandler = new UpdateHandler();

    private final TimeCounter timer = new TimeCounter();

    private Button resetButton;
    private Button startStopButton;
    private TextView displayView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            // TODO
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     * </p>
     * TODO: Directly include the fragment's contents? The New Project template created this. It
     * might be handy for the lock screen widget.
     */
    private class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() { }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            resetButton     = (Button) findViewById(R.id.resetButton);
            startStopButton = (Button) findViewById(R.id.startStopButton);
            displayView     = (TextView) findViewById(R.id.display);

            updateUI();
        }
    }

    // A Proguard rule keeps all Activity onClick*() methods.
    public void onClickStartStop(View v) {
        timer.toggleRunning();
        updateHandler.scheduleNextUpdate();
        updateUI();
    }

    public void onClickReset(View v) {
        timer.reset();
        updateUI();
    }

    /** Displays the current elapsed time. I.e. updates the display. */
    private void displayTime() {
        String formatted = timer.getFormattedElapsedTime();

        displayView.setText(formatted);
    }

    /** Updates the UI for the current state. */
    private void updateUI() {
        boolean running = timer.isRunning();

        displayTime();
        resetButton.setVisibility(running
                || timer.getElapsedTime() == 0 ? View.INVISIBLE : View.VISIBLE);
        startStopButton.setText(running ? R.string.stop : R.string.start);
    }

}
