package com.onefishtwo.bbqtimer;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;

import com.google.android.material.textfield.TextInputEditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Workaround: A subclass of Material Design TextInputEditText with a working OnFocusChangeListener,
 * "listener2".
 *
 * See {@link #setOnFocusChangeListener2}.
 */
public class EditText2 extends TextInputEditText {
    private OnFocusChangeListener focusChangeListener2 = null;

    public EditText2(@NonNull Context context) {
        super(context);
    }

    public EditText2(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public EditText2(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * Sets a working OnFocusChangeListener without disturbing the inherited mechanism (that doesn't
     * call its OnFocusChangeListener).
     */
    public void setOnFocusChangeListener2(OnFocusChangeListener listener2) {
        focusChangeListener2 = listener2;
    }

    @Override
    protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect);

        if (focusChangeListener2 != null) {
            focusChangeListener2.onFocusChange(this, focused);
        }
    }
}
