package com.onefishtwo.bbqtimer;

import android.app.UiAutomation;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;

/**
 * A JUnit {@link TestRule} that disables and re-enables animations for more reliable Espresso tests.
 * It also collapses the notification shade so Espresso can access the app's UI.
 */
public class DisableAnimationsRule implements TestRule {
    private static final String TAG = "DisableAnimationsRule";

    private static class AnimationScaleStatement extends Statement {
        private final Statement base;

        private AnimationScaleStatement(@NonNull Statement _base) {
            base = _base;
        }

        @Override
        public void evaluate() throws Throwable {
            collapseNotificationShade();
            setAnimationScale("0.0");

            try {
                base.evaluate();
            } finally {
                setAnimationScale("1.0");
            }
        }

        /**
         * Collapse the notification shade so Espresso can access the app's UI.
         */
        private void collapseNotificationShade() {
            final UiAutomation uiAutomation =
                    InstrumentationRegistry.getInstrumentation().getUiAutomation();
            //noinspection SpellCheckingInspection
            final String command = android.os.Build.VERSION.SDK_INT >= 29
                    ? "cmd statusbar collapse" // supported on Android API 29+
                    : "service call statusbar 2";

            try {
                android.os.ParcelFileDescriptor pfd = uiAutomation.executeShellCommand(command);
                try (java.io.InputStream is = new android.os.ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
                    byte[] buffer = new byte[1024];
                    //noinspection StatementWithEmptyBody
                    while (is.read(buffer) != -1) {
                        // Consume output to let the command finish cleanly and prevent SIGPIPE crashes
                        // which may cause "Test instrumentation process crashed".
                    }
                }
            } catch (IOException | RuntimeException e) {
                Log.w(TAG, "Failed to collapse status bar via: " + command, e);
            }
        }

        private void setAnimationScale(@NonNull String value) {
            final UiAutomation uiAutomation =
                    InstrumentationRegistry.getInstrumentation().getUiAutomation();

            // TODO: On API 33+ this could call uiAutomation.setAnimationScale(float).
            setGlobalSetting(uiAutomation, "window_animation_scale", value);
            setGlobalSetting(uiAutomation, "transition_animation_scale", value);
            setGlobalSetting(uiAutomation, "animator_duration_scale", value);
        }

        private void setGlobalSetting(@NonNull UiAutomation uiAutomation,
                                      @NonNull String key, @NonNull String value) {
            try {
                uiAutomation.executeShellCommand(
                        "settings put global " + key + " " + value).close();
            } catch (IOException | RuntimeException e) {
                Log.w(TAG, "Failed to set UI " + key + " = " + value, e);
            }
        }
    }

    @NonNull
    @Override
    public Statement apply(@NonNull Statement base, @NonNull Description description) {
        return new AnimationScaleStatement(base);
    }

}
