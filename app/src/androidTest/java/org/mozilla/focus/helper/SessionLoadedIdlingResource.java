/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.support.test.espresso.IdlingResource;

import org.mozilla.focus.activity.MainActivity;

import java.lang.ref.WeakReference;

/**
 * An IdlingResource implementation that waits until the BrowserFragment is not loading anymore.
 * Only after loading has completed further actions will be performed.
 */
public class SessionLoadedIdlingResource implements IdlingResource {
    private ResourceCallback resourceCallback;
    private WeakReference<MainActivity> activityWeakReference;

    public SessionLoadedIdlingResource(MainActivity activity) {
        activityWeakReference = new WeakReference<>(activity);
    }

    @Override
    public String getName() {
        return SessionLoadedIdlingResource.class.getSimpleName();
    }

    @Override
    public boolean isIdleNow() {
        final MainActivity mainActivity = activityWeakReference.get();
        if (mainActivity == null || mainActivity.getVisibleBrowserFragment() == null) {
            return false;
        }
        // FIXME: Maybe use an interface to wait for the state is better than depending on the activity directly
        final boolean isLoading = mainActivity.getVisibleBrowserFragment().isLoading();
        if (isLoading) {
            return false;
        } else {
            invokeCallback();
            return true;
        }
    }

    private void invokeCallback() {
        if (resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.resourceCallback = callback;
    }
}
