/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2026 Jerry Morrison
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.onefishtwo.bbqtimer;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.os.SystemClock;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;

import java.util.Collection;
import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

/** Test utility methods for Android UI and interaction tests. */
public class TestUtils {
    public static final long DEFAULT_TIMEOUT_MS = 5000;
    public static final long DEFAULT_POLL_INTERVAL_MS = 20;

    private TestUtils() {}

    /**
     * Polls the given {@link BooleanSupplier} until it returns true or timeoutMs has elapsed.
     *
     * @param timeoutMs      maximum time to poll in milliseconds
     * @param pollIntervalMs time to sleep between checks in milliseconds
     * @param checker        condition to verify
     * @return true if the condition was met before timing out; false otherwise
     */
    public static boolean pollForExpectation(long timeoutMs, long pollIntervalMs, @NonNull BooleanSupplier checker) {
        long intervalMs = Math.max(2, pollIntervalMs);
        long deadline = SystemClock.uptimeMillis() + timeoutMs;

        Objects.requireNonNull(checker, "checker must not be null");

        while (true) {
            if (checker.getAsBoolean()) {
                return true;
            }
            if (SystemClock.uptimeMillis() >= deadline) {
                return false;
            }
            SystemClock.sleep(intervalMs);
        }
    }

    /**
     * Polls the given {@link BooleanSupplier} using {@link #DEFAULT_TIMEOUT_MS} and
     * {@link #DEFAULT_POLL_INTERVAL_MS} until it returns true or timing out.
     */
    public static boolean pollForExpectation(@NonNull BooleanSupplier checker) {
        return pollForExpectation(DEFAULT_TIMEOUT_MS, DEFAULT_POLL_INTERVAL_MS, checker);
    }

    /**
     * Asserts that the checker returns true within DEFAULT_TIMEOUT_MS.
     *
     * @throws AssertionError if checker doesn't return true within DEFAULT_TIMEOUT_MS
     */
    public static void assertPollForExpectation(@NonNull String message, @NonNull BooleanSupplier checker) {
        assertTrue(message + " (timed out after " + DEFAULT_TIMEOUT_MS + " ms)",
                pollForExpectation(checker));
    }

    /** Asserts that an Activity is in the foreground, and it passes the predicate. */
    public static void assertPollForActivity(@NonNull String message,
                                             @NonNull final Predicate<Activity> predicate) {
        assertPollForExpectation(message, () -> {
            Activity act = getResumedActivity();

            return act != null && predicate.test(act);
        });
    }

    /** Returns the Activity that is in the foreground. */
    @Nullable
    public static Activity getResumedActivity() {
        final Activity[] activityHolder = new Activity[1];

        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            Collection<Activity> resumedActivities =
                    ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
            if (!resumedActivities.isEmpty()) {
                activityHolder[0] = resumedActivities.iterator().next();
            }
        });

        return activityHolder[0];
    }
}
