/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.tabs

import android.content.Context

/**
 * A class to create TabView instance.
 */
abstract class TabViewProvider {

    abstract fun create(): TabView

    /**
     * To clean up some persistent data which effect provided TabView, but not directly inside the TabView.
     *
     * For instance, a cookie effect a @see{android.webkit.WebView} but it is stores in another place.
     */
    open fun purify(context: Context?) {}
}
