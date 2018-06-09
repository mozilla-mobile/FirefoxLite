/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.support.test.espresso.IdlingResource;

import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.fragment.BrowserFragment;

import java.lang.ref.WeakReference;

/**
 * An IdlingResource implementation that waits until the BrowserFragment is not loading anymore.
 * Only after loading has completed further actions will be performed.
 */
public class SessionLoadedIdlingResource implements IdlingResource, BrowserFragment.LoadStateListener {
    private ResourceCallback resourceCallback;
    private boolean isCompleted;

    public SessionLoadedIdlingResource(MainActivity activity) {
        activity.getBrowserFragment().setIsLoadingListener(this);
    }

    @Override
    public String getName() {
        return SessionLoadedIdlingResource.class.getSimpleName();
    }

    @Override
    public boolean isIdleNow() {
        return isCompleted;
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

    @Override
    public void isLoadingChanged(boolean isLoading) {
        if (!isLoading) {
            invokeCallback();
            isCompleted = true;
        }

    }
}
