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

import android.os.SystemClock;

import androidx.annotation.NonNull;

/** Test utility methods for Android UI and interaction tests. */
public class TestUtils {
    public static final long DEFAULT_TIMEOUT_MS = 5000;
    public static final long DEFAULT_POLL_INTERVAL_MS = 20;

    @FunctionalInterface
    public interface CheckExpectation {
        boolean check();
    }

    /**
     * Polls the given {@link CheckExpectation} until it returns true or timeoutMs has elapsed.
     *
     * @param timeoutMs      maximum time to poll in milliseconds
     * @param pollIntervalMs time to sleep between checks in milliseconds
     * @param checker        condition to verify
     * @return true if the condition was met before timing out; false otherwise
     */
    public static boolean pollForExpectation(long timeoutMs, long pollIntervalMs, @NonNull CheckExpectation checker) {
        long deadline = SystemClock.uptimeMillis() + timeoutMs;

        while (SystemClock.uptimeMillis() < deadline) {
            if (checker.check()) {
                return true;
            }
            SystemClock.sleep(pollIntervalMs);
        }

        return false;
    }

    /**
     * Polls the given {@link CheckExpectation} using {@link #DEFAULT_TIMEOUT_MS} and
     * {@link #DEFAULT_POLL_INTERVAL_MS} until it returns true or timing out.
     */
    public static boolean pollForExpectation(@NonNull CheckExpectation checker) {
        return pollForExpectation(DEFAULT_TIMEOUT_MS, DEFAULT_POLL_INTERVAL_MS, checker);
    }
}
