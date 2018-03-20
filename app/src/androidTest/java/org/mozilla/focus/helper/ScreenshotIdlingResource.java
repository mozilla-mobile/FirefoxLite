/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.database.ContentObserver;
import android.net.Uri;
import android.support.test.espresso.IdlingResource;

import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.provider.ScreenshotContract;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public class ScreenshotIdlingResource implements IdlingResource {
    private ResourceCallback resourceCallback;
    private WeakReference<MainActivity> activityWeakReference;
    private AtomicBoolean isScreenshotInserted = new AtomicBoolean(false);
    private ContentObserver contentObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            screenshotIsInserted();
        }
    };

    public ScreenshotIdlingResource(MainActivity activity) {
        activityWeakReference = new WeakReference<>(activity);
    }

    @Override
    public String getName() {
        return ScreenshotIdlingResource.class.getSimpleName();
    }

    @Override
    public boolean isIdleNow() {
        return isScreenshotInserted.get();
    }

    private void screenshotIsInserted() {
        isScreenshotInserted.set(true);
        if (resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }
        MainActivity activity = activityWeakReference.get();
        if (activity != null) {
            activity.getContentResolver().unregisterContentObserver(contentObserver);
        }
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.resourceCallback = callback;
    }

    public void registerScreenshotObserver() {
        MainActivity activity = activityWeakReference.get();
        if (activity != null) {
            activity.getContentResolver().registerContentObserver(ScreenshotContract.Screenshot.CONTENT_URI, true, contentObserver);
        }
    }


}
