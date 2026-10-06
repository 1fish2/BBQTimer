// The MIT License (MIT)
//
// Copyright (c) 2026 Jerry Morrison
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

import android.app.Activity;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.DrawableRes;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.TooltipCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.snackbar.Snackbar;

public class BaseActivity extends AppCompatActivity {

    /** Enable edge-to-edge display? It's required on API 35+. Its problems on API < 29 might be
     *  fixed now but there's no strong need to test and debug it thoroughly on Android < Pie. */
    static final boolean EDGE_TO_EDGE = Build.VERSION.SDK_INT >= 29;
    public static final int REMINDER_STREAM = AudioManager.STREAM_ALARM;

    /**
     * Set the window insets policy for edge-to-edge display, as done in
     * developer.android.com/develop/ui/views/layout/edge-to-edge#system-bars-insets
     * <p>
     * @noinspection SameReturnValue
     */
    static @NonNull WindowInsetsCompat mainWindowInsetsListener(
            @NonNull View view, @NonNull WindowInsetsCompat windowInsets) {
        @NonNull Insets insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() // status, caption, & nav bars
                        | WindowInsetsCompat.Type.displayCutout()
                        | WindowInsetsCompat.Type.ime());
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();

        if (layoutParams != null) {
            ViewGroup.MarginLayoutParams mlp = (ViewGroup.MarginLayoutParams) layoutParams;

            mlp.setMargins(insets.left, insets.top, insets.right, insets.bottom);
            view.setLayoutParams(mlp);
        }

        return WindowInsetsCompat.CONSUMED; // don't pass windowInsets to nested Views
    }

    /** Hides the soft keyboard -- best efforts. */
    // https://stackoverflow.com/a/17789187/1682419
    public static void hideKeyboard(@NonNull Activity activity, @Nullable View v) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(INPUT_METHOD_SERVICE);

        if (v != null) {
            imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }

        View focussed = activity.getCurrentFocus();
        // TODO: Another fallback: if (focussed == null) focussed = new View(activity);
        if (focussed != null) {
            imm.hideSoftInputFromWindow(focussed.getWindowToken(), 0);
        }
    }

    /** Enables edge-to-edge mode on suitable OS versions. */
    void enableEdgeToEdge() {
        if (EDGE_TO_EDGE) {
            EdgeToEdge.enable(this);
        }
    }

    /**
     * Sets a WindowInsetsListener on the root View when in edge-to-edge mode to adjust its margins
     * to accommodate system bars, display cutouts, and the IME.
     *
     * @param rootView The layout's root {@link View}.
     */
    static void setEdgeToEdgeWindowInsetsListener(@NonNull View rootView) {
        // EdgeToEdge.enable(this) is already called in onCreate()
        if (EDGE_TO_EDGE) {
            ViewCompat.setOnApplyWindowInsetsListener(rootView,
                    BaseActivity::mainWindowInsetsListener);
        }
    }

    /** Finds a View and sets its Tooltip to match its ContentDescription. */
    <T extends View> T findViewByIdAndSetTooltip(@IdRes int id) {
        T view = findViewById(id);

        if (view != null) {
            TooltipCompat.setTooltipText(view, view.getContentDescription());
        }
        return view;
    }

    protected void setContentDescriptionAndTooltip(View view, @StringRes int resId) {
        CharSequence desc = getText(resId);

        view.setContentDescription(desc);
        TooltipCompat.setTooltipText(view, desc);
    }

    void logTheConfiguration(@NonNull String tag, @NonNull Configuration config) {
        String orientation = switch (config.orientation) {
            case Configuration.ORIENTATION_PORTRAIT -> "portrait";
            case Configuration.ORIENTATION_LANDSCAPE -> "landscape";
            default -> "undefined orientation";
        };

        Log.i(tag,
                String.format("Screen config: %dx%d dp, %d DPI, %s",
                        config.screenWidthDp, config.screenHeightDp, // Android 15+ includes system bars
                        config.densityDpi,
                        orientation));
    }

    /**
     * Sets the Snackbar's action.
     * <p>
     * NOTE: This used to set the action's text color but a custom background color gets overridden
     * now in day or night theme, so the custom text color became low-contrast. The two colors might
     * be settable in Theme.App but why bother?
     */
    @UiThread
    void setSnackbarAction(@NonNull Snackbar snackbar, @StringRes int resId,
                           View.OnClickListener listener) {
        snackbar.setAction(resId, listener);
    }

    /**
     * Remove focus from the text field.
     * <p>
     * Workaround: Older Android versions grab and hold focus or immediately refocus. So force the
     * text field to defocus by temporarily making it not-focusable.
     * <a href="https://stackoverflow.com/a/11044709/1682419">Stack Overflow</a>
     * <p>
     * Re-enable focusability after another short delay rather than via `setOnTouchListener()` [in
     * the stackoverflow post] so TAB & arrow keys can still enter/exit the text field.
     * <p>
     * Setting a caret in the text field, then using the popup menu to set & confirm new contents
     * might make Android log warnings such as:
     *    `W/IInputConnectionWrapper: requestCursorAnchorInfo on inactive InputConnection`
     * Delaying setFocusable(false) by 50ms would reduce those. Is it a net win?
     * <p>
     * NOTE: The UI test method delayForDefocusTextFieldWorkaround() must wait for this delay.
     */
    @UiThread
    void defocusTextField(@NonNull EditText textField) {
        if (Build.VERSION.SDK_INT <= 27) {
            textField.setFocusable(false);

            textField.postDelayed(() -> {
                textField.setFocusable(true);
                textField.setFocusableInTouchMode(true);
            }, 50);
        } else {
            textField.clearFocus();
        }
    }

    /**
     * Returns true if "Reduced Motion" is enabled in system settings.
     */
    boolean isReducedMotionEnabled() {
        try {
            float animatorScale = Settings.Global.getFloat(
                    getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE,
                    1.0f);
            return animatorScale <= 0.0f;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Set the left drawable of a Button (or any TextView); tag it with the resId to avoid redundant
     * animations and to aid testing.
     */
    void setDrawableRes(@NonNull TextView view, @DrawableRes int resId) {
        view.setCompoundDrawablesWithIntrinsicBounds(resId, 0, 0, 0);
        view.setTag(resId);
    }
}
