/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import org.mozilla.focus.BuildConfig;

public final class AppConstants {
    // see activity-alias name for the launcher in AndroidManifest.xml
    public static final String LAUNCHER_ACTIVITY_ALIAS = "org.mozilla.rocket.activity.MainActivity";
    public static final String LAUNCHER_PRIVATE_ACTIVITY_ALIAS = "org.mozilla.rocket.activity.PrivateModeActivity";

    static final String BUILD_TYPE_DEBUG = "debug";
    static final String BUILD_TYPE_FIREBASE = "firebase";
    static final String BUILD_TYPE_RELEASE = "release";
    static final String BUILD_TYPE_COVERAGE = "coverage";
    static final String FLAVOR_product_NIGHTLY = "preview";

    private static Boolean isUnderEspressoTest;

    private AppConstants() {
    }

    public static boolean isDevBuild() {
        return BUILD_TYPE_DEBUG.equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isCoverageBuild() {
        return BUILD_TYPE_COVERAGE.equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isFirebaseBuild() {
        return BUILD_TYPE_FIREBASE.equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isBuiltWithFirebase() {
        return isReleaseBuild() || isFirebaseBuild();
    }

    public static boolean isReleaseBuild() {
        return BUILD_TYPE_RELEASE.equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean supportsDownloadingFiles() {
        return true;
    }

    public static boolean isNightlyBuild() {
        return BuildConfig.FLAVOR_product == FLAVOR_product_NIGHTLY;
    }

    /**
     * Return the distribution channel that this app will deploy to. (release, nightly, debug...etc)
     * This is tightly coupled to the build script that we have right now. I can't make it more general
     * cause I don't want to give people the freedom to change this.
     *
     *
     */
    public static String getChannel() {
        if (isReleaseBuild()) {
            if (isNightlyBuild()) {
                return FLAVOR_product_NIGHTLY;
            } else {
                return BUILD_TYPE_RELEASE;
            }
        } else if (isDevBuild()) {
            return BUILD_TYPE_DEBUG;
        } else if (isFirebaseBuild()) {
            return BUILD_TYPE_FIREBASE;
        } else if (isCoverageBuild()) {
            return BUILD_TYPE_COVERAGE;
        }
        throw new IllegalArgumentException("Unexpected Telemetry Channel Detected");
    }

    public static boolean isUnderEspressoTest() {
        if (isUnderEspressoTest == null) {
            try {
                Class.forName("androidx.test.espresso.Espresso");
                isUnderEspressoTest = true;
            } catch (ClassNotFoundException e) {
                isUnderEspressoTest = false;
            }
        }

        return isUnderEspressoTest;
    }

}
