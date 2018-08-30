/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import org.mozilla.focus.BuildConfig;

public final class AppConstants {
    // see activity-alias name for the launcher in AndroidManifest.xml
    private static final String BUILD_TYPE_DEBUG = "debug";
    private static final String BUILD_TYPE_FIREBASE = "firebase";
    private static final String BUILD_TYPE_BETA = "beta";
    private static final String BUILD_TYPE_RELEASE = "release";

    private AppConstants() {
    }

    public static boolean isDevBuild() {
        return BUILD_TYPE_DEBUG.equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isFirebaseBuild() {
        return BUILD_TYPE_FIREBASE.equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isBuiltWithFirebase() {
        return isBetaBuild() || isReleaseBuild() || isFirebaseBuild();
    }

    public static boolean isReleaseBuild() {
        return BUILD_TYPE_RELEASE.equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean isBetaBuild() {
        return BUILD_TYPE_BETA.equals(BuildConfig.BUILD_TYPE);
    }

    public static boolean supportsDownloadingFiles() {
        return true;
    }
}
