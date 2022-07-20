package com.onefishtwo.bbqtimer;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;

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
        // TODO: Save contents at some juncture?
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
                R.style.Theme_Material3_DayNight_Dialog); // TODO: Pass in a theme ID? R.style.Theme_Material_Dialog_Alert? Theme_Material3_DayNight_Dialog? Theme_AppCompat_Dialog_Alert? Theme_Material3_Dark_Dialog_Alert?
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View content = inflater.inflate(R.layout.dialog_edit_recipes, null);
        // "To ensure consistent styling, the custom view should be inflated or constructed using
        // the alert dialog's themed context obtained via getContext()."

        builder.setTitle(R.string.edit_list_title)
                .setView(content);

        textField = content.findViewById(R.id.recipes_text_field);
        textField.setOnFocusChangeListener(this::onEditTextFocusChange);

        if (textField != null) {
            textField.setText(getInitContents());

            builder.setPositiveButton(R.string.save_edits, (dialog, which) -> {
                        hideKeyboard(textField);
                        listener.onEditorDialogPositiveClick(
                                dialog, textField.getText().toString());
                    })
                    .setNegativeButton(R.string.cancel_edits, (dialog, which) -> {
                        hideKeyboard(textField);
                        listener.onEditorDialogNegativeClick(dialog);
                        dialog.cancel();
                    });
        }

        return builder.create();
    }
}
