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
 */
public class DisableAnimationsRule implements TestRule {
    private static final String TAG = "DisableAnimationsRule";

    @NonNull
    @Override
    public Statement apply(@NonNull Statement base, @NonNull Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                setAnimationScale("0.0");
                try {
                    base.evaluate();
                } finally {
                    setAnimationScale("1.0");
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
        };
    }

}
