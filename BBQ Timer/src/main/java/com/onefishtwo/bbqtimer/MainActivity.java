package com.onefishtwo.bbqtimer;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity {
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

    // TODO: Add this method to Proguard rules.
    public void onStartStop(View v) {
        if (timer.isRunning()) {
            timer.pause();
        } else {
            timer.start();
        }

        updateUI();
    }

    // TODO: Add this method to Proguard rules.
    public void onReset(View v) {
        timer.reset();
        updateUI();
    }

    /** Updates the time display. */
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
