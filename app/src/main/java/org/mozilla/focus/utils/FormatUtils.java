/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import java.text.DecimalFormat;

public class FormatUtils {

    private final static DecimalFormat DF = new DecimalFormat("0.0");
    private final static long K = 1024;
    private final static long SIZE_KB = 1024L;
    private final static long SIZE_MB = SIZE_KB * K;
    private final static long SIZE_GB = SIZE_MB * K;
    private final static long SIZE_TB = SIZE_GB * K; // insane

    public static String getReadableStringFromFileSize(long size) {
        if (size < SIZE_MB) {
            return DF.format(size / SIZE_KB) + " KB";
        } else if (size < SIZE_GB) {
            return DF.format(size / SIZE_MB) + " MB";
        } else if (size < SIZE_TB) {
            return DF.format(size / SIZE_GB) + " GB";
        } else {
            return DF.format(size / SIZE_TB) + " TB";
        }
    }
}
