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
    private final TimeCounter time = new TimeCounter();
    private boolean running = false;

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
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            return rootView;
        }
    }

    // TODO: Add this method to Proguard rules.
    public void onStartStop(View v) {
        running = !running;
        time.addMilliseconds(2500); // TODO: actual stopwatch timing

        displayTime();
        updateUI();
    }

    // TODO: Add this method to Proguard rules.
    public void onReset(View v) {
        time.setTime(0);
        displayTime();
        updateUI();
    }

    /** Updates the time display. */
    private void displayTime() {
        String formatted = time.toString();
        TextView displayView = (TextView) findViewById(R.id.display);

        displayView.setText(formatted);
    }

    /** Updates the UI controls for the current state. */
    private void updateUI() {
        // TODO: Cache these, but onCreate() is too early to fetch them.
        Button resetButton     = (Button) findViewById(R.id.resetButton);
        Button startStopButton = (Button) findViewById(R.id.startStopButton);

        resetButton.setVisibility(running || time.getTime() == 0 ? View.INVISIBLE : View.VISIBLE);
        startStopButton.setText(running ? R.string.stop : R.string.start);
    }

}
