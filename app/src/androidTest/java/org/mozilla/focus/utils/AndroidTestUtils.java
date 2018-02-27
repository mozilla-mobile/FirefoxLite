/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;


public final class AndroidTestUtils {

    public static void beforeTest(final Context context) {
        if (context != null) {
            final Settings settings = Settings.getInstance(context);
            if (settings != null) {
                settings.setRateAppDialogDidShow();
                settings.setShareAppDialogDidShow();
            }
        }
    }
}
