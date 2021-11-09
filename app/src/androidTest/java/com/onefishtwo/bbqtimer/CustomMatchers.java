/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Jerry Morrison
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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.TextView;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.BoundedMatcher;

class CustomMatchers {
    /** Matches a child View at the given position in a parent View. From Espresso Test Recorder. */
    @SuppressWarnings("unused")
    @NonNull
    static Matcher<View> childAtPosition(
            @NonNull final Matcher<View> parentMatcher, final int position) {

        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(@NonNull Description description) {
                description.appendText("Child at position " + position + " in parent ");
                parentMatcher.describeTo(description);
            }

            @Override
            public boolean matchesSafely(@NonNull View view) {
                ViewParent parent = view.getParent();
                return parent instanceof ViewGroup && parentMatcher.matches(parent)
                        && view.equals(((ViewGroup) parent).getChildAt(position));
            }
        };
    }

    /**
     * Matches a View that has the given compound drawable resource. Cribbed and tweaked from
     * <a href="https://gist.github.com/frankiesardo/7490059">@frankiesardo</a>.
     */
    @SuppressWarnings("SpellCheckingInspection")
    @NonNull
    static Matcher<View> withCompoundDrawable(@DrawableRes final int resourceId) {
        return new BoundedMatcher<View, TextView>(TextView.class) {
            @Override
            public void describeTo(@NonNull Description description) {
                description.appendText("has compound drawable resource " + resourceId);
            }

            @Override
            public boolean matchesSafely(@NonNull TextView textView) {
                for (Drawable drawable : textView.getCompoundDrawables()) {
                    if (sameBitmap(textView.getContext(), drawable, resourceId)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    /**
     * <b>Modifies the given ViewInteraction</b> to have a no-op FailureHandler. Use this, e.g.,
     * to perform an action on a View if it's visible and not complain if it isn't.
     *
     * @return the modified ViewInteraction
     */
    @SuppressWarnings("unused")
    @NonNull
    static ViewInteraction ignoringFailures(@NonNull ViewInteraction interaction) {
        return interaction.withFailureHandler((error, viewMatcher) -> {
        });
    }

    private static boolean sameBitmap(@NonNull Context context,
            @Nullable Drawable drawable, @DrawableRes int resourceId) {
        Drawable otherDrawable = ContextCompat.getDrawable(context, resourceId);

        if (drawable == null || otherDrawable == null) {
            return false;
        }

        drawable = drawable.getCurrent();
        otherDrawable = otherDrawable.getCurrent();

        if (drawable instanceof BitmapDrawable && otherDrawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Bitmap otherBitmap = ((BitmapDrawable) otherDrawable).getBitmap();
            return bitmap != null && bitmap.sameAs(otherBitmap);
        }
        return false;
    }

}
