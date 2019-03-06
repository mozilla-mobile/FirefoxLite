/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.content

import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.fragment.ListPanelDialog
import org.mozilla.focus.fragment.ListPanelDialog.TYPE_DEFAULT
import org.mozilla.focus.fragment.ListPanelDialog.TYPE_NEWS

object HomeFragmentViewState {
    // TODO: make it enum class
    @JvmStatic
    var STATE = TYPE_DEFAULT

    @JvmStatic
    fun reset() {
        STATE = TYPE_DEFAULT
    }

    @JvmStatic
    fun apply(activity: MainActivity?) {
        when (STATE) {
            TYPE_NEWS -> {
                activity?.showListPanel(ListPanelDialog.TYPE_NEWS)
                reset()
            }
        }
    }

    @JvmStatic
    fun set(state: Int) {
        STATE = state
    }
}
