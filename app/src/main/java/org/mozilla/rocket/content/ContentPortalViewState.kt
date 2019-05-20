/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.content

/**
 * Preserve the state of everything on Content portal.
 * Since Content Portal is attached to HomeFragment right now and HomeFragment is re-created very often,
 * we need to keep the state manually instead of reusing fragments in Content Portal
 * (due to [org.mozilla.focus.home.HomeFragment] recreates often.
 */
object ContentPortalViewState {

    var lastNewsTab: Int? = null
    var lastNewsPos: Int? = null
    var lastEcTab: Int? = null

    // TODO: make them enum / sealed class
    private const val STATE_CLOSED = 0
    private const val STATE_OPENED = 1

    private var state = STATE_CLOSED

    @JvmStatic
    fun reset() {
        state = STATE_CLOSED
        lastNewsTab = null
        lastNewsPos = null
        lastEcTab = null
    }

    @JvmStatic
    fun lastOpened() {
        state = STATE_OPENED
    }

    fun isOpened(): Boolean {
        return state == STATE_OPENED
    }
}