package com.onefishtwo.bbqtimer;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import com.onefishtwo.bbqtimer.state.ApplicationState;

import com.onefishtwo.bbqtimer.BuildConfig;

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

        // Load the state now, not in the UI thread, to avoid delaying the UI thread, and under
        // cover of the splash screen animation.
        // TODO: Load it asynchronously with access synchronization.
        ApplicationState.sharedInstance(this);
    }
}
