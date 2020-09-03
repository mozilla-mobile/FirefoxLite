/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.tabs

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.view.View
import org.mozilla.rocket.tabs.SiteIdentity.SecurityState
import org.mozilla.rocket.tabs.web.DownloadCallback

interface TabView {
    val view: View
    val isBlockingEnabled: Boolean
    val url: String?
    val title: String?

    @get:SecurityState
    val securityState: Int

    /**
     * Enable/Disable content blocking for this session (Only the blockers that are enabled in the app's settings will be turned on/off).
     */
    fun setContentBlockingEnabled(enabled: Boolean)

    /**
     * Be invoked by TabChromeClient.onCreateWindow to transport this new-created-window to its parent window.
     *
     * @param msg The message to send when once a new WebView has been created
     */
    fun bindOnNewWindowCreation(msg: Message)

    fun setImageBlockingEnabled(enabled: Boolean)
    fun setJavaScriptBlockingEnabled(enabled: Boolean)
    fun performExitFullScreen()
    fun setViewClient(viewClient: TabViewClient?)
    fun setChromeClient(chromeClient: TabChromeClient?)
    fun setDownloadCallback(callback: DownloadCallback?)
    fun setFindListener(callback: FindListener?)
    fun onPause()
    fun onResume()
    fun destroy()
    fun reload()
    fun stopLoading()
    fun loadUrl(url: String?)
    fun cleanup()
    fun goForward()
    fun goBack()
    fun canGoForward(): Boolean
    fun canGoBack(): Boolean
    fun restoreViewState(inState: Bundle?)
    fun saveViewState(outState: Bundle?)
    fun insertBrowsingHistory()
    fun buildDrawingCache(autoScale: Boolean)
    fun getDrawingCache(autoScale: Boolean): Bitmap?

    class HitTarget(
        source: TabView,
        isLink: Boolean,
        linkURL: String?,
        isImage: Boolean,
        imageURL: String?
    ) {
        @JvmField
        val source: TabView

        @JvmField
        val isLink: Boolean

        @JvmField
        val linkURL: String?

        @JvmField
        val isImage: Boolean

        @JvmField
        val imageURL: String?

        init {
            check(!(isLink && linkURL == null)) { "link hittarget must contain URL" }
            check(!(isImage && imageURL == null)) { "image hittarget must contain URL" }
            this.source = source
            this.isLink = isLink
            this.linkURL = linkURL
            this.isImage = isImage
            this.imageURL = imageURL
        }
    }

    interface FullscreenCallback {
        fun fullScreenExited()
    }

    interface FindListener {
        fun onFindResultReceived(
            activeMatchOrdinal: Int,
            numberOfMatches: Int,
            isDoneCounting: Boolean
        )
    }
}
