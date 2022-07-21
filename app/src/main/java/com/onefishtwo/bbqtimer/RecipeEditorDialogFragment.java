package com.onefishtwo.bbqtimer;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
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
    public interface RecipeEditorDialogFragmentListener {
        void onEditorDialogPositiveClick(DialogInterface dialog, String text);
        void onEditorDialogNegativeClick(DialogInterface dialog);
        // Override onDismiss() to notice all dismissal cases? hideKeyboard() doesn't work there.
        // Maybe it gets called too late for that.
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
        AlertDialog.Builder builder = new AlertDialog.Builder(requireActivity(),
                R.style.ThemeOverlay_Material3_TextInputEditText_OutlinedBox);
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View content = inflater.inflate(R.layout.dialog_edit_recipes, null);
        // "To ensure consistent styling, the custom view should be inflated or constructed using
        // the alert dialog's themed context obtained via getContext()."

        builder.setTitle(R.string.edit_list_title)
                .setView(content);

        textField = content.findViewById(R.id.recipes_text_field);
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
     * DialogInterface.OnClickListener for the "Save" button: Hides the soft keyboard and returns
     * the edited text to the Activity. Returns the default text if the edited text is empty.
     */
    private void saveEdits(DialogInterface dialog, int which) {
        String result = textField.getText().toString();

        if (result.trim().length() == 0) {
            result = ApplicationState.getDefaultRecipes(textField.getContext());
        }

        hideKeyboard(textField);
        listener.onEditorDialogPositiveClick(dialog, result);
    }

    /** DialogInterface.OnClickListener for the "Reset" button. */
    private void resetEdits(DialogInterface dialog, int which) {
        textField.setText("");
        saveEdits(dialog, which);
    }

    /** DialogInterface.OnClickListener for the "Cancel" button. */
    private void cancelEdits(DialogInterface dialog, int which) {
        hideKeyboard(textField);
        listener.onEditorDialogNegativeClick(dialog);
        dialog.cancel();
    }
}
