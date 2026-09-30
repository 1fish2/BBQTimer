package com.onefishtwo.bbqtimer;

import android.app.UiAutomation;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.io.IOException;
import java.io.InputStream;

/**
 * A JUnit {@link TestRule} that disables and restores animation scales for more reliable Espresso
 * tests. It also collapses the notification shade so Espresso can access the app's UI.
 */
public class DisableAnimationsRule implements TestRule {
    private static final String TAG = "DisableAnimationsRule";

    private static class AnimationScaleStatement extends Statement {
        private final Statement base;

        private AnimationScaleStatement(@NonNull Statement baseStmt) {
            base = baseStmt;
        }

        @Override
        public void evaluate() throws Throwable {
            UiAutomation uiAutomation =
                    InstrumentationRegistry.getInstrumentation().getUiAutomation();

            collapseNotificationShade(uiAutomation);

            if (Build.VERSION.SDK_INT >= 33) {
                uiAutomation.setAnimationScale(0.0f);
                try {
                    base.evaluate();
                } finally {
                    uiAutomation.setAnimationScale(1.0f);
                }
            } else {
                setAnimationScale(uiAutomation, "0.0");
                try {
                    base.evaluate();
                } finally {
                    setAnimationScale(uiAutomation, "1.0");
                }
            }
        }

        /**
         * Collapse the notification shade so Espresso can access the app's UI.
         */
        @SuppressWarnings("SpellCheckingInspection")
        private void collapseNotificationShade(@NonNull UiAutomation uiAutomation) {
            String command = "cmd statusbar collapse"; // "service call statusbar 2" on API < 24
            executeAndConsumeShellCommand(uiAutomation, command);
        }

        private void setAnimationScale(@NonNull UiAutomation uiAutomation, @NonNull String value) {
            setGlobalSetting(uiAutomation, Settings.Global.WINDOW_ANIMATION_SCALE, value);
            setGlobalSetting(uiAutomation, Settings.Global.TRANSITION_ANIMATION_SCALE, value);
            setGlobalSetting(uiAutomation, Settings.Global.ANIMATOR_DURATION_SCALE, value);
        }

        private void setGlobalSetting(@NonNull UiAutomation uiAutomation,
                                      @NonNull String key, @NonNull String value) {
            executeAndConsumeShellCommand(uiAutomation, "settings put global " + key + " " + value);
        }

        private void executeAndConsumeShellCommand(@NonNull UiAutomation uiAutomation,
                                                   @NonNull String command) {
            try (ParcelFileDescriptor pfd = uiAutomation.executeShellCommand(command);
                 InputStream is = new ParcelFileDescriptor.AutoCloseInputStream(pfd)) {
                byte[] buffer = new byte[1024];
                //noinspection StatementWithEmptyBody
                while (is.read(buffer) != -1) {
                    // Consume output to let the command finish cleanly and prevent SIGPIPE crashes
                    // which may cause "Test instrumentation process crashed".
                }
            } catch (IOException | RuntimeException e) {
                Log.w(TAG, "Failed shell command: " + command, e);
            }
        }
    }

    @NonNull
    @Override
    public Statement apply(@NonNull Statement base, @NonNull Description description) {
        return new AnimationScaleStatement(base);
    }

}
