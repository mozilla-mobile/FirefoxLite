/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.support.annotation.WorkerThread;

import org.mozilla.focus.R;

import java.util.HashMap;

@WorkerThread
final class FirebaseHelperInject {

    // XML default value can't read l10n values, so we use code for default values.
    static HashMap<String, Object> getRemoteConfigDefault(Context context) {
        // We need to provide different value for different build type for debug/testing purpose
        final HashMap<String, Object> map = new HashMap<>();
        map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_TITLE, context.getString(R.string.rate_app_dialog_text_title));
        map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_CONTENT, context.getString(R.string.rate_app_dialog_text_content));
        return map;
    }
}
