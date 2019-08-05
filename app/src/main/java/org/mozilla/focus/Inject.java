/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus;

import android.content.Context;
import android.os.StrictMode;
import android.preference.PreferenceManager;

import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.utils.AppConstants;
import org.mozilla.focus.utils.RemoteConfigConstants;

public class Inject {

    public static String getDefaultTopSites(Context context) {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(HomeFragment.TOPSITES_PREF, null);

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

    public static RemoteConfigConstants.SURVEY getDefaultFeatureSurvey() {
        return RemoteConfigConstants.SURVEY.NONE;
    }
}
