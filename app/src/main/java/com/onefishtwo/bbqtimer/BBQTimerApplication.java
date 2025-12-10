package com.onefishtwo.bbqtimer;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import com.onefishtwo.bbqtimer.state.ApplicationState;

public class BBQTimerApplication extends Application {

    @Override
    public void onCreate() {
        if (BuildConfig.ENABLE_STRICT_MODE) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectAll() // Instead?: .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build());

            StrictMode.ThreadPolicy.Builder builder = new StrictMode.ThreadPolicy.Builder()
                    .detectAll();
            if (Build.VERSION.SDK_INT >= 34) {
                builder.permitExplicitGc();
            }
            StrictMode.setThreadPolicy(builder
                    .penaltyLog()
                    .build());
        }

        super.onCreate();

        // Start loading the state now, not in the UI thread, to avoid delaying the UI thread, and
        // under cover of the splash screen animation. The overlap probably doesn't help much.
        new Thread(() -> ApplicationState.sharedInstance(this)).start();

        // DynamicColors.applyToActivitiesIfAvailable(this);
    }
}
