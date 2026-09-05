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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.onefishtwo.bbqtimer.state.ApplicationState;

/**
 * A Dialog to edit the recipe list.
 * <p>
 * The FragmentActivity that instantiates a RecipeEditorDialogFragment must implement
 * {@link RecipeEditorDialogFragmentListener}.
 */
public class RecipeEditorDialogFragment extends DialogFragment {
    public static final String TAG = "RecipeEditor";
    private static final String KEY_TEXT_CONTENTS = "Text";
    static final String FALLBACK_CONTENTS = ":30\n1\n1:30\n2\n";

    /**
     * The FragmentActivity that instantiates a RecipeEditorDialogFragment must implement this
     * Listener interface so it can get the edited results.
     */
    public interface RecipeEditorDialogFragmentListener {
        /** The user changed (edited or reset) the recipes. */
        void onEditorDialogPositiveClick(@NonNull String text);
        /** The user canceled the dialog; no change to the recipes. */
        @SuppressWarnings("EmptyMethod")
        void onEditorDialogNegativeClick();
    }

    private RecipeEditorDialogFragmentListener listener;
    private EditText textField;

    /** Creates and initializes a recipe list editor dialog. */
    @NonNull
    public static RecipeEditorDialogFragment newInstance(@Nullable String text) {
        RecipeEditorDialogFragment dialog = new RecipeEditorDialogFragment();
        Bundle bundle = new Bundle();
        String contents = (text == null || text.trim().isEmpty()) ? FALLBACK_CONTENTS : text;

        bundle.putString(KEY_TEXT_CONTENTS, contents);
        dialog.setArguments(bundle);
        return dialog;
    }

    /**
     * API 27: Work around an Android bug where double-clicking an EditText field would cause these
     * log errors:
     * <p>
     *   TextClassifierImpl: Error suggesting selection for text. No changes to selection suggested.
     *     java.io.FileNotFoundException: No file for null locale
     *         at android.view.textclassifier.TextClassifierImpl.getSmartSelection(TextClassifierImpl.java:208)
     *         ...
     *   TextClassifierImpl: Error getting assist info.
     *     java.io.FileNotFoundException: No file for null locale
     *         at android.view.textclassifier.TextClassifierImpl.getSmartSelection(TextClassifierImpl.java:208)
     *         ...
     * <p>
     * API > 27: Work around an Android bug that calls the TextClassifier on the main thread
     * (UI thread) [e.g. when the user double-taps the EditText field, or long-presses it, or
     * dismisses the soft keyboard when there's a text selection], causing this log warning even
     * though no app code is on the call stack:
     * <p>
     *   W/androidtc: TextClassifier called on main thread.
     * <p>
     * To avoid the delay and potential ANR, just bypass the irrelevant TextClassifier. (This
     * problem might not occur on API 28 - 29, but it's safer to do this uniformly.)
     */
    public static void workaroundTextClassifier(@NonNull EditText editText) {
        if (Build.VERSION.SDK_INT >= 27) {
            editText.setTextClassifier(TextClassifier.NO_OP);
        }
    }

    @NonNull
    public String getInitContents() {
        Bundle bundle = getArguments();

        if (bundle != null) {
            String string = bundle.getString(KEY_TEXT_CONTENTS);

            if (string != null) {
                return string;
            }
        }

        return FALLBACK_CONTENTS;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof RecipeEditorDialogFragmentListener) {
            listener = (RecipeEditorDialogFragmentListener) context;
        } else {
            throw new ClassCastException(
                    context + " expected to implement RecipeEditorDialogFragmentListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        listener = null;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        textField = null;
    }

    private void hideKeyboard(@Nullable View v) {
        MainActivity.hideKeyboard(requireActivity(), v);
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
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(requireActivity(),
                R.style.AppThemeOverlay_Material3_MaterialAlertDialog);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View content = inflater.inflate(R.layout.dialog_edit_recipes, null);

        builder.setView(content);

        textField = content.findViewById(R.id.recipes_text_field);

        if (textField != null) {
            workaroundTextClassifier(textField);
            textField.setOnFocusChangeListener(this::onEditTextFocusChange);

            // Workaround: The XML scrolling attributes don't work very well.
            textField.setHorizontallyScrolling(true);
            textField.setHorizontalScrollBarEnabled(true);
            textField.setScrollbarFadingEnabled(false);

            // Set initial text only on fresh creation to preserve user edits on rotation. Actually,
            // change listeners will restore some state unless disabled via setSaveEnabled(false).
            //noinspection VariableNotUsedInsideIf
            if (savedInstanceState == null) {
                textField.setText(getInitContents());
            } else {
                // Workaround: Get the cursor/caret rendered after rotation.
                textField.post(() -> {
                    if (textField != null) {
                        textField.requestFocus();
                        textField.invalidate();
                    }
                });
            }
        }

        builder.setPositiveButton(R.string.save_edits, (dialog, which) -> {
                    String text = textField != null ? textField.getText().toString() : "";
                    saveText(text);
                })
                .setNeutralButton(R.string.reset, (dialog, which) -> saveText(""))
                .setNegativeButton(R.string.cancel_edits, (dialog, which) -> cancelEdits(dialog));

        return builder.create();
    }

    /**
     * Hides the soft keyboard and passes the given recipes (or if blank, the default recipes)
     * to the listener.
     */
    private void saveText(@NonNull String recipes) {
        hideKeyboard(textField);

        String resolvedRecipes = recipes.trim().isEmpty()
                ? ApplicationState.getDefaultRecipes(requireContext())
                : recipes;

        if (listener != null) {
            listener.onEditorDialogPositiveClick(resolvedRecipes);
        }
    }

    private void cancelEdits(DialogInterface dialog) {
        hideKeyboard(textField);

        if (listener != null) {
            listener.onEditorDialogNegativeClick();
        }

        dialog.cancel();
    }
}
