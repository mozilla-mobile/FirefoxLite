/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.support.test.espresso.IdlingResource;

import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.screenshot.CaptureRunnable;

public class ScreenshotIdlingResource implements IdlingResource, CaptureRunnable.CaptureStateListener {
    private ResourceCallback resourceCallback;

    private boolean completed;

    public ScreenshotIdlingResource(MainActivity activity) {
        activity.getBrowserFragment().setCaptureStateListener(this);
    }

    @Override
    public String getName() {
        return ScreenshotIdlingResource.class.getSimpleName();
    }

    @Override
    public boolean isIdleNow() {
        return completed;
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
    public void onPromptScreenshotResult() {
        completed = true;
        invokeCallback();
    }
}

