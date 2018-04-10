/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.database.ContentObserver;
import android.net.Uri;
import android.support.test.espresso.IdlingResource;

import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.provider.DownloadContract;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

/** DownloadCompleteIdlingResource utilize {@link org.mozilla.focus.provider.DownloadInfoProvider} to notify and
 *  determine when a download task is completed. Call registerDownloadCompleteObserver() before register this IdlingResource.
 */
public class DownloadCompleteIdlingResource implements IdlingResource {
    private ResourceCallback resourceCallback;
    private WeakReference<MainActivity> activityWeakReference;
    private AtomicBoolean isDownloadComplete = new AtomicBoolean(false);
    private ContentObserver contentObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            downloadComplete();
        }
    };

    public DownloadCompleteIdlingResource(MainActivity activity) {
        activityWeakReference = new WeakReference<>(activity);
    }

    @Override
    public String getName() {
        return DownloadCompleteIdlingResource.class.getSimpleName();
    }

    @Override
    public boolean isIdleNow() {
        return isDownloadComplete.get();
    }

    private void downloadComplete() {
        isDownloadComplete.set(true);
        if (resourceCallback != null) {
            resourceCallback.onTransitionToIdle();
        }
        final MainActivity activity = activityWeakReference.get();
        if (activity != null) {
            activity.getContentResolver().unregisterContentObserver(contentObserver);
        }
    }

    @Override
    public void registerIdleTransitionCallback(ResourceCallback callback) {
        this.resourceCallback = callback;
    }

    public void registerDownloadCompleteObserver() {
        final MainActivity activity = activityWeakReference.get();
        if (activity != null) {
            activity.getContentResolver().registerContentObserver(DownloadContract.Download.CONTENT_URI, true, contentObserver);
        }
    }

}
