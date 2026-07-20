package com.onefishtwo.bbqtimer;

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import com.google.android.material.color.ColorContrast;
import com.google.android.material.color.ColorContrastOptions;
import com.onefishtwo.bbqtimer.state.ApplicationState;

import java.io.PrintWriter;
import java.io.StringWriter;

public class BBQTimerApplication extends Application {

    @Override
    public void onCreate() {
        if (BuildConfig.ENABLE_STRICT_MODE) {
            StrictMode.VmPolicy.Builder builder1 = new StrictMode.VmPolicy.Builder()
                    .detectAll() // Instead?: .detectLeakedClosableObjects()
                    .penaltyLog();
            if (Build.VERSION.SDK_INT >= 28) {
                builder1.detectNonSdkApiUsage();
                builder1.penaltyListener(getMainExecutor(), violation -> {
                    if (violation instanceof android.os.strictmode.NonSdkApiUsedViolation) {
                        String msg = violation.getMessage();
                        if (msg != null && msg.contains("makeOptionalFitsSystemWindows")) {
                            Log.d("StrictMode", "^^^ NOTE: The NonSdkApiUsedViolation for " +
                                    "makeOptionalFitsSystemWindows() is a known AppCompat issue.");
                        }
                    } else if (violation instanceof android.os.strictmode.LeakedClosableViolation
                            && Build.VERSION.SDK_INT == 32) {
                        StringWriter sw = new StringWriter();
                        violation.printStackTrace(new PrintWriter(sw));
                        String stackTraceString = sw.toString();

                        if (stackTraceString.contains("UnixSecureDirectoryStream")) {
                            Log.d("StrictMode", "^^^ NOTE: This UnixSecureDirectoryStream leak" +
                                    " is a known androidx/platform issue on API 32.");
                        }
                    }
                });
            }
            StrictMode.setVmPolicy(builder1.build());

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
        // ASSUMES: MainActivity keeps the Splash Screen open until the state finishes loading.
        new Thread(() -> ApplicationState.sharedInstance(this)).start();

        ColorContrastOptions options = new ColorContrastOptions.Builder()
                .setMediumContrastThemeOverlay(R.style.ThemeOverlay_App_Contrast_Medium)
                .setHighContrastThemeOverlay(R.style.ThemeOverlay_App_Contrast_High)
                .build();
        ColorContrast.applyToActivitiesIfAvailable(this, options);
    }
}
