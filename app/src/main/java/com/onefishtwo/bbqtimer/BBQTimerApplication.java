// The MIT License (MIT)
//
// Copyright (c) 2024 Jerry Morrison
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

import android.app.Application;
import android.os.Build;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.google.android.material.color.ColorContrast;
import com.google.android.material.color.ColorContrastOptions;
import com.onefishtwo.bbqtimer.state.ApplicationState;

public class BBQTimerApplication extends Application {

    private static final String STRICT_MODE_TAG = "StrictMode";

    @Override
    public void onCreate() {
        if (BuildConfig.ENABLE_STRICT_MODE) {
            setupStrictMode();
        }

        super.onCreate();

        // Start loading the state in a background thread, under cover of the splash screen
        // animation, to delay the UI thread less. The overlap might not help much.
        // ASSUMES: MainActivity keeps the Splash Screen open until the state finishes loading.
        new Thread(() -> ApplicationState.sharedInstance(this), "StateLoaderThread")
                .start();

        ColorContrastOptions options = new ColorContrastOptions.Builder()
                .setMediumContrastThemeOverlay(R.style.ThemeOverlay_App_Contrast_Medium)
                .setHighContrastThemeOverlay(R.style.ThemeOverlay_App_Contrast_High)
                .build();
        ColorContrast.applyToActivitiesIfAvailable(this, options);
    }

    private void setupStrictMode() {
        StrictMode.VmPolicy.Builder vmPolicyBuilder = new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog();

        if (Build.VERSION.SDK_INT >= 28) {
            vmPolicyBuilder.detectNonSdkApiUsage();
            vmPolicyBuilder.penaltyListener(getMainExecutor(), this::handleVmPolicyViolation);
        }

        StrictMode.setVmPolicy(vmPolicyBuilder.build());

        StrictMode.ThreadPolicy.Builder threadPolicyBuilder = new StrictMode.ThreadPolicy.Builder()
                .detectAll();

        if (Build.VERSION.SDK_INT >= 34) {
            threadPolicyBuilder.permitExplicitGc();
        }

        StrictMode.setThreadPolicy(threadPolicyBuilder
                .penaltyLog()
                .build());
    }

    /** Filters known framework / library StrictMode violations to avoid log spam during development. */
    @RequiresApi(api = Build.VERSION_CODES.P)
    private void handleVmPolicyViolation(android.os.strictmode.Violation violation) {
        //noinspection ChainOfInstanceofChecks
        if (violation instanceof android.os.strictmode.NonSdkApiUsedViolation) {
            String msg = violation.getMessage();
            if (msg != null && msg.contains("makeOptionalFitsSystemWindows")) {
                Log.d(STRICT_MODE_TAG, "^^^ NOTE: The NonSdkApiUsedViolation for " +
                        "makeOptionalFitsSystemWindows() is a known AppCompat issue.");
            }
        } else if (violation instanceof android.os.strictmode.LeakedClosableViolation
                && Build.VERSION.SDK_INT == 32) {
            if (hasStackTraceFrame(violation, "UnixSecureDirectoryStream")) {
                Log.d(STRICT_MODE_TAG, "^^^ NOTE: This UnixSecureDirectoryStream leak" +
                        " is a known androidx/platform issue on API 32.");
            }
        }
    }

    /** Checks if any stack trace element in the given Throwable contains the target class/method string. */
    private static boolean hasStackTraceFrame(
            Throwable throwable,
            @SuppressWarnings("SameParameterValue") CharSequence target) {
        for (StackTraceElement element : throwable.getStackTrace()) {
            if (element.toString().contains(target)) {
                return true;
            }
        }
        return false;
    }
}
