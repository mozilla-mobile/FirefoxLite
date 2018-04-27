/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.persistence.TabsDatabase;
import org.mozilla.focus.utils.AppConstants;

public class Inject {

    public static String getDefaultTopSites(Context context) {

        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(HomeFragment.TOPSITES_PREF, null);

    }

    public static TabsDatabase getTabsDatabase(Context context) {
        return TabsDatabase.getInstance(context);
    }


    public static boolean isTelemetryEnabled(Context context) {
        // The first access to shared preferences will require a disk read.
        final StrictMode.ThreadPolicy threadPolicy = StrictMode.allowThreadDiskReads();
        try {
            final Resources resources = context.getResources();
            final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
            final boolean isEnabledByDefault = AppConstants.isBetaBuild() || AppConstants.isReleaseBuild();
            // Telemetry is not enable by default in debug build. But the user / developer can choose to turn it on
            // in AndroidTest, this is enabled by default
            return preferences.getBoolean(resources.getString(R.string.pref_key_telemetry), isEnabledByDefault);
        } finally {
            StrictMode.setThreadPolicy(threadPolicy);
        }

    }

    public static void enableStrictMode() {
        if (AppConstants.isReleaseBuild()) {
            return;
        }

        final StrictMode.ThreadPolicy.Builder threadPolicyBuilder = new StrictMode.ThreadPolicy.Builder().detectAll();
        final StrictMode.VmPolicy.Builder vmPolicyBuilder = new StrictMode.VmPolicy.Builder().detectAll();

        threadPolicyBuilder.penaltyLog().penaltyDialog();
        // Previously we have penaltyDeath() for debug build, but in order to add crashlytics, we can't use it here.
        // ( crashlytics has untagged Network violation so it always crashes
        vmPolicyBuilder.penaltyLog();

        StrictMode.setThreadPolicy(threadPolicyBuilder.build());
        StrictMode.setVmPolicy(vmPolicyBuilder.build());
    }
}
