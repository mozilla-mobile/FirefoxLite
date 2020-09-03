/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.tabs

import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient.FileChooserParams
import org.mozilla.rocket.tabs.TabView.FullscreenCallback

/**
 * An abstract layer of @see{android.webkit.WebChromeClient}
 */
open class TabChromeClient {

    open fun onCreateWindow(
        isDialog: Boolean,
        isUserGesture: Boolean,
        msg: Message?
    ): Boolean = false

    open fun onCloseWindow(tabView: TabView?) {}
    open fun onProgressChanged(progress: Int) {}

    /**
     * @see android.webkit.WebChromeClient
     */
    open fun onShowFileChooser(
        tabView: TabView,
        filePathCallback: ValueCallback<Array<Uri>>?,
        fileChooserParams: FileChooserParams?
    ): Boolean = false

    open fun onReceivedTitle(view: TabView, title: String?) {}
    open fun onReceivedIcon(view: TabView, icon: Bitmap?) {}
    open fun onLongPress(hitTarget: TabView.HitTarget) {}

    /**
     * Notify the host application that the current page has entered full screen mode.
     *
     *
     * The callback needs to be invoked to request the page to exit full screen mode.
     *
     *
     * Some TabView implementations may pass a custom View which contains the web contents in
     * full screen mode.
     */
    open fun onEnterFullScreen(callback: FullscreenCallback, view: View?) {}

    /**
     * Notify the host application that the current page has exited full screen mode.
     *
     *
     * If a View was passed when the application entered full screen mode then this view must
     * be hidden now.
     */
    open fun onExitFullScreen() {}
    open fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback?
    ) {
    }
}
