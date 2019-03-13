/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.content

object HomeFragmentViewState {

    // TODO: make them enum / sealed class
    private const val TYPE_DEFAULT = 0
    private const val TYPE_NEWS = 1

    private var state = TYPE_DEFAULT

    var lastScrollPos: Int? = null

    @JvmStatic
    fun reset() {
        state = TYPE_DEFAULT
        lastScrollPos = null
    }

    @JvmStatic
    fun lastOpenNews() {
        state = HomeFragmentViewState.TYPE_NEWS
    }

    fun isLastOpenNews(): Boolean {
        return state == TYPE_NEWS
    }
}