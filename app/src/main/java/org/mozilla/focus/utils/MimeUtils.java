/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import androidx.annotation.Nullable;
import android.text.TextUtils;

import java.util.regex.Pattern;

/**
 * Simple utility to help check MIME type. Its matching algorithm is not perfect but enough for now.
 */
public class MimeUtils {

    private static final Pattern textPattern = Pattern.compile("^text/[0-9,a-z,A-Z,-,*]+?$");
    private static final Pattern imgPattern = Pattern.compile("^image/[0-9,a-z,A-Z,-,*]+?$");
    private static final Pattern audioPattern = Pattern.compile("^audio/[0-9,a-z,A-Z,-,*]+?$");
    private static final Pattern videoPattern = Pattern.compile("^video/[0-9,a-z,A-Z,-,*]+?$");

    private MimeUtils() {
        throw new RuntimeException("Do not initialize util class");
    }

    public static boolean isText(@Nullable String type) {
        return !TextUtils.isEmpty(type) && textPattern.matcher(type).find();
    }

    public static boolean isImage(@Nullable String type) {
        return !TextUtils.isEmpty(type) && imgPattern.matcher(type).find();
    }

    public static boolean isAudio(@Nullable String type) {
        return !TextUtils.isEmpty(type) && audioPattern.matcher(type).find();
    }

    public static boolean isVideo(@Nullable String type) {
        return !TextUtils.isEmpty(type) && videoPattern.matcher(type).find();
    }
}
