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
import android.content.SharedPreferences;

/**
 * Provides access to the application's state. The state is kept in shared preferences and cached in
 * static variables.
 */
public class ApplicationState {

    /** PERSISTENT STATE filename. */
    public static final String APPLICATION_PREF_FILE = "BBQ_Timer_Prefs";

    /** PERSISTENT STATE identifiers. */
    static final String PREF_MAIN_ACTIVITY_IS_VISIBLE = "App_mainActivityIsVisible";

    /** Application state. It's cached iff timeCounter != null. */
    private static TimeCounter timeCounter;
    private static boolean mainActivityIsVisible; // between onStart() .. onStop()

    /** Loads persistent state on demand. */
    private static void loadState(Context context) {
        if (timeCounter == null) {
            timeCounter = new TimeCounter();
            SharedPreferences prefs =
                    context.getSharedPreferences(APPLICATION_PREF_FILE, Context.MODE_PRIVATE);

            timeCounter.load(prefs);
            mainActivityIsVisible = prefs.getBoolean(PREF_MAIN_ACTIVITY_IS_VISIBLE, false);
        }
    }

    /** Saves persistent state. */
    static void saveState(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(APPLICATION_PREF_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();

        timeCounter.save(prefsEditor);
        prefsEditor.putBoolean(PREF_MAIN_ACTIVITY_IS_VISIBLE, mainActivityIsVisible);
        prefsEditor.commit();
    }

    /** Gets the shared TimeCounter instance, using context to load app state if needed. */
    static TimeCounter getTimeCounter(Context context) {
        loadState(context);
        return timeCounter;
    }

    /** Gets isMainActivityVisible, using context to load app state if needed. */
    static boolean isMainActivityVisible(Context context) {
        loadState(context);
        return mainActivityIsVisible;
    }

    /**
     * Sets isMainActivityVisible, using context to load app state if needed.</p>
     *
     * Call {@link #saveState(android.content.Context)} to save it.
     */
    public static void setMainActivityIsVisible(Context context, boolean mainActivityIsVisible) {
        loadState(context);
        ApplicationState.mainActivityIsVisible = mainActivityIsVisible;
    }
}
