package com.onefishtwo.bbqtimer;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.textclassifier.TextClassifier;
import android.widget.EditText;

import com.onefishtwo.bbqtimer.state.ApplicationState;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

/**
 * A Dialog to edit the recipe list.
 * </p>
 * The FragmentActivity that instantiates a RecipeEditorDialogFragment must implement
 * RecipeEditorDialogFragmentListener.
 */
public class RecipeEditorDialogFragment extends DialogFragment {
    public static final String TAG = "RecipeEditor";
    private static final String KEY_TEXT_CONTENTS = "Text";
    static final String FALLBACK_CONTENTS = ":30\n1\n1:30\n2\n";

    /**
     * The FragmentActivity that instantiates a RecipeEditorDialogFragment must implement this
     * Listener interface so it can get the edited results. */
    @SuppressWarnings("unused")
    public interface RecipeEditorDialogFragmentListener {
        /** The user changed (edited or reset) the recipes. */
        void onEditorDialogPositiveClick(DialogInterface dialog, String text);
        /** The user cancelled the dialog; no change to the recipes. */
        @SuppressWarnings("EmptyMethod")
        void onEditorDialogNegativeClick(DialogInterface dialog);
        // Override onDismiss() to notice all dismissal cases? onDismiss() calling hideKeyboard()
        // doesn't work. Maybe it gets called too late.
    }

    private RecipeEditorDialogFragmentListener listener;
    private EditText textField;

    /** Creates and initializes a recipe list editor dialog. */
    public static RecipeEditorDialogFragment newInstance(String text) {
        RecipeEditorDialogFragment dialog = new RecipeEditorDialogFragment();
        Bundle bundle = new Bundle();

        // The text is stored in the Arguments Bundle so it's available on re-create.
        // TODO: Save contents in onPause() or onDestroy()?
        if (text.trim().length() == 0) {
            text = FALLBACK_CONTENTS;
        }
        bundle.putString(KEY_TEXT_CONTENTS, text);
        dialog.setArguments(bundle);
        return dialog;
    }

    /**
     * API 27: Work around an Android bug where double-clicking an EditText field would cause these
     * log errors:
     *
     *   TextClassifierImpl: Error suggesting selection for text. No changes to selection suggested.
     *     java.io.FileNotFoundException: No file for null locale
     *         at android.view.textclassifier.TextClassifierImpl.getSmartSelection(TextClassifierImpl.java:208)
     *         ...
     *   TextClassifierImpl: Error getting assist info.
     *     java.io.FileNotFoundException: No file for null locale
     *         at android.view.textclassifier.TextClassifierImpl.getSmartSelection(TextClassifierImpl.java:208)
     *         ...
     *
     * API > 27: Work around an Android bug that calls the TextClassifier on the main thread
     * (UI thread) [e.g. when the user double-taps the EditText field, or long-presses it, or
     * dismisses the soft keyboard when there's a text selection], causing this log warning even
     * though no app code is on the call stack:
     *
     *   W/androidtc: TextClassifier called on main thread.
     *
     * To avoid the delay and potential ANR, just bypass the irrelevant TextClassifier. (This
     * problem might not occur on API 28 - 29, but it's safer to do this uniformly.)
     */
    @SuppressWarnings("SpellCheckingInspection")
    public static void workaroundTextClassifier(EditText editText) {
        if (Build.VERSION.SDK_INT >= 27) {
            editText.setTextClassifier(TextClassifier.NO_OP);
        }
    }

    @NonNull
    public String getInitContents() {
        Bundle bundle = getArguments();

        if (bundle == null) {
            return FALLBACK_CONTENTS;
        } else {
            String string = bundle.getString(KEY_TEXT_CONTENTS);
            return string == null ? FALLBACK_CONTENTS : string;
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            listener = (RecipeEditorDialogFragmentListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(
                    context + " expected to implement RecipeEditorDialogFragmentListener");
        }
    }

    private void hideKeyboard(@Nullable View v) {
        MainActivity.hideKeyboard(requireActivity(), v);
        // TODO: v.clearFocus() to avoid showing keyboard again if you open the app from the background?
    }

    /**
     * The TextEdit field's focus changed, e.g. by TAB, arrow keys, or view.clearFocus().
     * If it lost focus, hide the soft keyboard to ensure it's not hiding the Save & Cancel buttons.
     */
    private void onEditTextFocusChange(View view, boolean nowHasFocus) {
        if (!nowHasFocus) {
            hideKeyboard(view);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        // This theme shows the dialog full-width and scrolling in a way that the soft keyboard
        // nicely resizes it, keeping the Reset/Cancel/Save buttons visible and accessible.
        // Using MaterialAlertDialogBuilder doesn't seem to accomplish that, but maybe it's a
        // question of the right theme.
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(),
                R.style.ThemeOverlay_Material3_TextInputEditText_OutlinedBox);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View content = inflater.inflate(R.layout.dialog_edit_recipes, null);
        // "To ensure consistent styling, the custom view should be inflated or constructed using
        // the alert dialog's themed context obtained via getContext()."

        builder.setTitle(R.string.edit_list_title)
                .setView(content);

        textField = content.findViewById(R.id.recipes_text_field);
        workaroundTextClassifier(textField);
        textField.setOnFocusChangeListener(this::onEditTextFocusChange);

        // Workaround: The XML scrolling attributes don't work very well.
        textField.setHorizontallyScrolling(true);
        textField.setHorizontalScrollBarEnabled(true);
        textField.setScrollbarFadingEnabled(false);

        if (textField != null) {
            textField.setText(getInitContents());

            builder.setPositiveButton(R.string.save_edits, this::saveEdits)
                    .setNeutralButton(R.string.reset, this::resetEdits)
                    .setNegativeButton(R.string.cancel_edits, this::cancelEdits);
        }

        return builder.create();
    }

    /**
     * <li>Hides the soft keyboard.
     * <li>Passes the given recipes (or if blank, the default recipes) to the listener.
     */
    private void saveText(DialogInterface dialog, String recipes) {
        hideKeyboard(textField);

        if (recipes.trim().length() == 0) {
            recipes = ApplicationState.getDefaultRecipes(textField.getContext());
        }
        listener.onEditorDialogPositiveClick(dialog, recipes);
    }

    /** DialogInterface.OnClickListener for the "Save" button. */
    private void saveEdits(DialogInterface dialog, int which) {
        saveText(dialog, textField.getText().toString());
    }

    /** DialogInterface.OnClickListener for the "Reset" button. */
    private void resetEdits(DialogInterface dialog, int which) {
        saveText(dialog, "");
    }

    /** DialogInterface.OnClickListener for the "Cancel" button. */
    private void cancelEdits(DialogInterface dialog, int which) {
        hideKeyboard(textField);
        listener.onEditorDialogNegativeClick(dialog);
        dialog.cancel();
    }
}
