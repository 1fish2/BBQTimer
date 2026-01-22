package com.onefishtwo.bbqtimer;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;

import com.google.android.material.color.ColorContrast;
import com.google.android.material.color.ColorContrastOptions;
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

        // Start loading the state in a background thread, under cover of the splash screen
        // animation, to delay the UI thread less. The overlap might not help much.
        new Thread(() -> ApplicationState.sharedInstance(this)).start();

        ColorContrastOptions options = new ColorContrastOptions.Builder()
                .setMediumContrastThemeOverlay(R.style.ThemeOverlay_App_Contrast_Medium)
                .setHighContrastThemeOverlay(R.style.ThemeOverlay_App_Contrast_High)
                .build();
        ColorContrast.applyToActivitiesIfAvailable(this, options);
    }
}
