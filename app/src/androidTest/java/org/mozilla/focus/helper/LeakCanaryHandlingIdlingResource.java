/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.helper;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.test.espresso.IdlingResource;

import com.squareup.leakcanary.LeakCanary;

import org.mozilla.focus.utils.AndroidTestAnalysisResultService;

import java.lang.ref.WeakReference;
import java.util.concurrent.atomic.AtomicBoolean;

public class LeakCanaryHandlingIdlingResource implements IdlingResource {

    WeakReference<Context> context;
    private static final String LEAKCANARY_DISPLAYLEAKSERVICE_NAME = AndroidTestAnalysisResultService.class.getName();
    private static final String TAG = "LeakCanaryHandlingIdlingResource";
    private ResourceCallback resourceCallback;
    @NonNull
    private AtomicBoolean isCompleted = new AtomicBoolean(false);

    public LeakCanaryHandlingIdlingResource(Context c){
        context = new WeakReference<>(c.getApplicationContext());


    }

    @Override
    public String getName() {
        return LeakCanaryHandlingIdlingResource.class.getSimpleName();
    }

    @Override
    public boolean isIdleNow() {
        final Context ctx = context.get();
        if (ctx == null) {
            return false;
        }
        LeakCanary a;
        final ComponentName component = new ComponentName(ctx, LEAKCANARY_DISPLAYLEAKSERVICE_NAME);
        final PackageManager packageManager = ctx.getPackageManager();
        final boolean enabled = packageManager.getComponentEnabledSetting(component)==PackageManager.COMPONENT_ENABLED_STATE_ENABLED ;

        if (enabled && AndroidTestAnalysisResultService.isCompleted) {
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
