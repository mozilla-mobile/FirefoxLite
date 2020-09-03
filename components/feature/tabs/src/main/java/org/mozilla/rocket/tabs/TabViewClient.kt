/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.tabs

/**
 * An abstract layer of @see{android.webkit.WebViewClient}
 */
open class TabViewClient {
    open fun onPageStarted(url: String?) {}
    open fun onPageFinished(isSecure: Boolean) {}
    open fun onURLChanged(url: String?) {}

    /**
     * Return true if the URL was handled, false if we should continue loading the current URL.
     */
    open fun handleExternalUrl(url: String?): Boolean = false

    open fun updateFailingUrl(url: String?, updateFromError: Boolean) {}
    open fun onHttpAuthRequest(callback: HttpAuthCallback, host: String?, realm: String?) {}

    interface HttpAuthCallback {
        fun proceed(username: String?, password: String?)
        fun cancel()
    }
}
