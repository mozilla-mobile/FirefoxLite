/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.database.ContentObserver;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.test.espresso.IdlingResource;
import android.util.Log;

import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.provider.ScreenshotContract;
import org.mozilla.focus.utils.FirebaseHelper;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public class FirebaseEnablerIdlingResource implements IdlingResource, FirebaseHelper.BlockingEnabler.BlockingEnablerCallback {
    private static final String TAG = "FirebaseEnablerIdlingResource";
    private ResourceCallback resourceCallback;
    @NonNull
    private AtomicBoolean isCompleted = new AtomicBoolean(false);

    @Override
    public String getName() {
        return FirebaseEnablerIdlingResource.class.getSimpleName();
    }

    @Override
    public boolean isIdleNow() {
        if (isCompleted.get()) {
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


    @Override
    public void runDelayOnExecution() {
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Log.d(TAG, "runDelayOnExecution: ");
        }
    }

    @Override
    public void onComplete() {
        isCompleted.set(true);
    }
}
