/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.test.espresso.IdlingResource;

import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.utils.Constants;

import java.lang.ref.WeakReference;

/**
 * An IdlingResource implementation that waits until the BrowserFragment is not loading anymore.
 * Only after loading has completed further actions will be performed.
 */
public class DownloadCompletedIdlingResource extends BroadcastReceiver implements IdlingResource {
    private ResourceCallback resourceCallback;
    private boolean downloadCompleted;

    @Override
    public void onReceive(Context context, Intent intent) {
        switch (intent.getAction()) {
            case Constants.ACTION_NOTIFY_RELOCATE_FINISH:
                downloadCompleted = true;
                break;
            default:
                break;
        }
    }
    @Override
    public String getName() {
        return DownloadCompletedIdlingResource.class.getSimpleName();
    }

    @Override
    public boolean isIdleNow() {
        if (downloadCompleted) {
            invokeCallback();
            return true;
        } else {
            return false;
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
