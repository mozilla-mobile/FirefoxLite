/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.tabs.utils

import android.os.Bundle
import android.text.TextUtils

object TabUtil {
    private const val ARG_PARENT_ID = "_tab_parent_"
    private const val ARG_EXTERNAL = "_tab_external_"
    private const val ARG_FOCUS = "_tab_focus_"

    /**
     * To create a bundle which contains dispensable arguments for manipulating tabs.
     *
     * @param parentId     id of parent tab
     * @param fromExternal true if the tab is opened by external app
     * @param toFocus      true if the tab should be focused after creation
     * @return the @see{android.os.Bundle} contains arguments
     */
    @JvmStatic
    fun argument(
        parentId: String?,
        fromExternal: Boolean,
        toFocus: Boolean
    ): Bundle {
        val bundle = Bundle()
        if (!TextUtils.isEmpty(parentId)) {
            bundle.putString(ARG_PARENT_ID, parentId)
        }
        bundle.putBoolean(ARG_EXTERNAL, fromExternal)
        bundle.putBoolean(ARG_FOCUS, toFocus)
        return bundle
    }

    @JvmStatic
    fun getParentId(bundle: Bundle): String? {
        return bundle.getString(ARG_PARENT_ID)
    }

    @JvmStatic
    fun isFromExternal(bundle: Bundle): Boolean {
        return bundle.getBoolean(ARG_EXTERNAL, false)
    }

    @JvmStatic
    fun toFocus(bundle: Bundle): Boolean {
        return bundle.getBoolean(ARG_FOCUS, false)
    }
}
