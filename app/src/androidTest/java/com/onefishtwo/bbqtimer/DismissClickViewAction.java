package com.onefishtwo.bbqtimer;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.ViewActions;

import org.hamcrest.Matcher;

/**
 * A ViewAction to click on a dismiss-dialog button then make Espresso wait until the dialog closes
 * before performing more actions.
 */
public class DismissClickViewAction implements ViewAction {
    private final String dialogTag;
    private final ViewAction click = ViewActions.click();

    /**
     * Construct a ViewAction to click on a dialog button that's intended to close it ("Save",
     * "Cancel", ...), then make Espresso wait until it's closed before performing more actions.
     */
    public DismissClickViewAction(String _dialogTag) {
        dialogTag = _dialogTag;
    }

    @Override
    public Matcher<View> getConstraints() {
        return click.getConstraints();
    }

    @Override
    public String getDescription() {
        return "click to dismiss a dialog and declare 'busy' until it closes";
    }

    @Override
    public void perform(UiController uiController, View view) {
        // Get the FragmentManager from the view's context.
        FragmentActivity activity = unwrap(view.getContext());
        FragmentManager fm = activity.getSupportFragmentManager();

        // Perform the click.
        click.perform(uiController, view);

        // Register a DialogIdlingResource AFTER performing the click to start blocking further
        // actions until the dialog closes. It will then unregister itself.
        DialogIdlingResource.registerNewIdlingResource(fm, dialogTag);
    }

    private static FragmentActivity unwrap(Context context) {
        while (!(context instanceof Activity) && context instanceof ContextWrapper) {
            context = ((ContextWrapper) context).getBaseContext();
        }

        // This could throw ClassCastException if the Activity isn't a FragmentActivity.
        //noinspection DataFlowIssue
        return (FragmentActivity) context;
    }
}
