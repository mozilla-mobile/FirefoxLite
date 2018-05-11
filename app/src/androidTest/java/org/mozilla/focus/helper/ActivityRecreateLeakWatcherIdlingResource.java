/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.support.test.espresso.IdlingResource;

import org.mozilla.focus.utils.LeakWatcher;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

// This idling resource will track the latest destroyed activity.
// And check if the old instance is cleared when re-created.
public class ActivityRecreateLeakWatcherIdlingResource implements IdlingResource, Application.ActivityLifecycleCallbacks {

    private ResourceCallback resourceCallback;
    private AtomicBoolean isCompleted = new AtomicBoolean(false);
    private AtomicBoolean hasLeak = new AtomicBoolean(false);

    // this idling resource listen to the activity recreation
    public ActivityRecreateLeakWatcherIdlingResource(Activity activity) {
        activity.getApplication().registerActivityLifecycleCallbacks(this);
    }

    public boolean hasLeak() {
        return hasLeak.get();
    }

    @Override
    public String getName() {
        return ActivityRecreateLeakWatcherIdlingResource.class.getSimpleName();
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
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (LeakWatcher.getReference() != null && LeakWatcher.getReference().get() != null) {
            LeakWatcher.runGc();
            hasLeak.set(LeakWatcher.getReference().get() == null);
        }
        isCompleted.set(true);
    }

    @Override
    public void onActivityStarted(Activity activity) {

    }

    @Override
    public void onActivityResumed(Activity activity) {

    }

    @Override
    public void onActivityPaused(Activity activity) {

    }

    @Override
    public void onActivityStopped(Activity activity) {

    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        LeakWatcher.setReference(new WeakReference<>(activity));
    }
}
