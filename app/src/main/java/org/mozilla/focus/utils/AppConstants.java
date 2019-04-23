/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import org.mozilla.focus.BuildConfig;

public final class AppConstants {
    // see activity-alias name for the launcher in AndroidManifest.xml
    public static final String LAUNCHER_ACTIVITY_ALIAS = "org.mozilla.rocket.activity.MainActivity";

    private static final String BUILD_TYPE_DEBUG = "debug";
    private static final String BUILD_TYPE_FIREBASE = "firebase";
    private static final String BUILD_TYPE_RELEASE = "release";
    private static final String FLAVOR_product_NIGHTLY = "preview";
    private static final String FLAVOR_product_PRODUCTION = "focus";

    private AppConstants() {
    }

    public static boolean isDevBuild() {
        return BUILD_TYPE_DEBUG.equals(BuildConfig.BUILD_TYPE);
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

    public static boolean isProductionBuild() {
        return isReleaseBuild() && BuildConfig.FLAVOR_product == FLAVOR_product_PRODUCTION;
    }

}
