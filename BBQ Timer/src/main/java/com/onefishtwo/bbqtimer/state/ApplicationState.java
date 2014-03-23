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

package com.onefishtwo.bbqtimer.state;

import android.content.Context;
import android.content.SharedPreferences;

import com.onefishtwo.bbqtimer.TimeCounter;

/**
 * Stores the application's state persistently in SharedPreferences and caches it in static
 * variables while the process is in memory.</p>
 *
 * This does not currently provide listener notifications.</p>
 *
 * The setters only update the state in memory. Call {@link #saveState(android.content.Context)} to
 * make the changes persistent.
 */
public class ApplicationState {

    /** PERSISTENT STATE filename. */
    public static final String APPLICATION_PREF_FILE = "BBQ_Timer_Prefs";

    /** PERSISTENT STATE identifiers. */
    static final String PREF_MAIN_ACTIVITY_IS_VISIBLE = "App_mainActivityIsVisible";
    static final String PREF_ENABLE_REMINDERS = "App_enableReminders";
    static final String PREF_SECONDS_PER_REMINDER = "App_secondsPerReminder";

    /** Application state. It's cached iff timeCounter != null. */
    private static TimeCounter timeCounter;
    private static boolean mainActivityIsVisible; // between onStart() .. onStop()
    private static boolean enableReminders;
    private static int secondsPerReminder;

    /** Loads persistent state on demand. */
    private static void loadState(Context context) {
        if (timeCounter == null) {
            timeCounter = new TimeCounter();
            SharedPreferences prefs =
                    context.getSharedPreferences(APPLICATION_PREF_FILE, Context.MODE_PRIVATE);

            timeCounter.load(prefs);
            mainActivityIsVisible = prefs.getBoolean(PREF_MAIN_ACTIVITY_IS_VISIBLE, false);
            enableReminders       = prefs.getBoolean(PREF_ENABLE_REMINDERS, false);
            secondsPerReminder    = prefs.getInt(PREF_SECONDS_PER_REMINDER, 5 * 60);
        }
    }

    /** Saves persistent state. */
    public static void saveState(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(APPLICATION_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();

        timeCounter.save(prefsEditor);
        prefsEditor.putBoolean(PREF_MAIN_ACTIVITY_IS_VISIBLE, mainActivityIsVisible);
        prefsEditor.putBoolean(PREF_ENABLE_REMINDERS, enableReminders);
        prefsEditor.putInt(PREF_SECONDS_PER_REMINDER, secondsPerReminder);
        prefsEditor.commit();
    }

    /**
     * Returns the shared TimeCounter instance, using context to load the app state if needed.</p>
     *
     * NOTE: The TimeCounter is a shared, mutable object. The caller can update it then call
     * {@link #saveState(android.content.Context)} to store the updated data.
     */
    public static TimeCounter getTimeCounter(Context context) {
        loadState(context);
        return timeCounter;
    }

    /**
     * Returns a boolean indicating whether MainActivity is visible [it's between
     * onStart() .. onStop()], using context to load the app state if needed.
     */
    public static boolean isMainActivityVisible(Context context) {
        loadState(context);
        return mainActivityIsVisible;
    }

    /**
     * Sets a boolean indicating whether MainActivity is visible, using context to load the app
     * state if needed.</p>
     *
     * Call {@link #saveState(android.content.Context)} to save it.
     */
    public static void setMainActivityIsVisible(Context context, boolean mainActivityIsVisible) {
        loadState(context);
        ApplicationState.mainActivityIsVisible = mainActivityIsVisible;
    }

    /**
     * Returns a boolean indicating whether periodic reminders are enabled, using context to load
     * the app state if needed.
     */
    public static boolean isEnableReminders(Context context) {
        loadState(context);
        return enableReminders;
    }

    /**
     * Sets a boolean indicating whether periodic reminders are enabled, using context to load the
     * app state if needed.</p>
     *
     * Call {@link #saveState(android.content.Context)} to save it.
     */
    public static void setEnableReminders(Context context, boolean enableReminders) {
        loadState(context);
        ApplicationState.enableReminders = enableReminders;
    }

    /**
     * Returns the number of seconds between periodic reminders, using context to load the app state
     * if needed.
     */
    public static int getSecondsPerReminder(Context context) {
        loadState(context);
        return secondsPerReminder;
    }

    /**
     * Returns the number of milliseconds between periodic reminders, using context to load the app
     * state if needed.
     */
    public static long getMillisecondsPerReminder(Context context) {
        return getSecondsPerReminder(context) * 1000L;
    }

    /**
     * Sets the number of seconds between periodic reminders, using context to load the app state if
     * needed.</p>
     *
     * Call {@link #saveState(android.content.Context)} to save it.
     */
    public static void setSecondsPerReminder(Context context, int secondsPerReminder) {
        loadState(context);
        ApplicationState.secondsPerReminder = secondsPerReminder;
    }
}
