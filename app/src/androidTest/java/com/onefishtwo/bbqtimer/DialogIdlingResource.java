package com.onefishtwo.bbqtimer;

import android.util.Log;

import androidx.fragment.app.FragmentManager;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;

/**
 * An Espresso {@link IdlingResource} that monitors the presence of the tagged DialogFragment so an
 * Espresso test will wait until that dialog closes before performing more actions.
 * Some experiments indicate that Espresso checks IdlingResources before performing a ViewAction.
 */
public class DialogIdlingResource implements IdlingResource {
    public static final String TAG = "DialogIdlingResource";
    private final FragmentManager fragmentManager;
    private final String tag;
    private volatile ResourceCallback resourceCallback;

    /**
     * Create a new DialogIdlingResource and register it with the Espresso UI test framework.
     * Call this right after performing an action that dismisses a dialog so the next action will
     * wait until the dialog closes.
     * The DialogIdlingResource will unregister itself when it becomes idle.
     */
    static void registerNewIdlingResource(FragmentManager fm, String dialogTag) {
        IdlingResource idlingResource = new DialogIdlingResource(fm, dialogTag);
        IdlingRegistry.getInstance().register(idlingResource);
        Log.d(TAG, "registered");
    }

    public DialogIdlingResource(FragmentManager _fragmentManager, String _tag) {
        fragmentManager = _fragmentManager;
        tag = _tag;
    }

    @Override
    public String getName() {
        return DialogIdlingResource.class.getName() + ":" + tag;
    }

    @Override
    public boolean isIdleNow() {
        boolean idle = fragmentManager.findFragmentByTag(tag) == null;
        Log.d(TAG, idle ? "isIdleNow" : "not isIdleNow");
        if (idle) {
            if (resourceCallback != null) {
                resourceCallback.onTransitionToIdle();
            }
            IdlingRegistry.getInstance().unregister(this);
        }
        return idle;
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback _resourceCallback) {
        resourceCallback = _resourceCallback;
    }
}
