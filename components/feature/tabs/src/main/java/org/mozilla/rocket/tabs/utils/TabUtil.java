/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.tabs.utils;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.text.TextUtils;

public final class TabUtil {

    static final String ARG_PARENT_ID = "_tab_parent_";
    static final String ARG_EXTERNAL = "_tab_external_";
    static final String ARG_FOCUS = "_tab_focus_";

    private TabUtil() {
    }

    /**
     * To create a bundle which contains dispensable arguments for manipulating tabs.
     *
     * @param parentId     id of parent tab
     * @param fromExternal true if the tab is opened by external app
     * @param toFocus      true if the tab should be focused after creation
     * @return the @see{android.os.Bundle} contains arguments
     */
    public static Bundle argument(@Nullable final String parentId,
                                  boolean fromExternal,
                                  boolean toFocus) {
        final Bundle bundle = new Bundle();
        if (!TextUtils.isEmpty(parentId)) {
            bundle.putString(ARG_PARENT_ID, parentId);
        }

        bundle.putBoolean(ARG_EXTERNAL, fromExternal);
        bundle.putBoolean(ARG_FOCUS, toFocus);
        return bundle;
    }

    @Nullable
    public static String getParentId(@NonNull final Bundle bundle) {
        return bundle.getString(ARG_PARENT_ID);
    }

    public static boolean isFromExternal(@NonNull final Bundle bundle) {
        return bundle.getBoolean(ARG_EXTERNAL, false);
    }

    public static boolean toFocus(@NonNull final Bundle bundle) {
        return bundle.getBoolean(ARG_FOCUS, false);
    }
}
