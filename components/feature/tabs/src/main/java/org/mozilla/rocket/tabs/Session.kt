/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs

import android.graphics.Bitmap
import android.os.Bundle
import android.text.TextUtils
import android.view.ViewGroup
import org.mozilla.rocket.tabs.TabView.FindListener
import org.mozilla.rocket.tabs.web.DownloadCallback
import java.util.UUID

const val TAG = "Session"

class Session @JvmOverloads constructor(
        val id: String = UUID.randomUUID().toString(),
        var parentId: String? = "",
        var title: String? = "",
        var url: String? = ""
) {
    var tabView: TabView? = null
        private set

    private var tabViewClient = sDefViewClient
    private var tabChromeClient = sDefChromeClient
    private var downloadCallback: DownloadCallback? = null
    private var findListener: TabView.FindListener? = null

    var webViewState: Bundle? = null

    var favicon: Bitmap? = null

    val securityState: Int
        @SiteIdentity.SecurityState
        get() = if (tabView == null) {
            SiteIdentity.UNKNOWN
        } else tabView!!.securityState

    val isFromExternal: Boolean
        get() = ID_EXTERNAL == parentId

    // TODO: not implement completely
    private var thumbnail: Bitmap? = null

    /**
     * To sync session's properties to view, before saving. This method would be retired once we
     * involve Observable class for those properties.
     */
    fun syncFromView() {
        if (webViewState == null) {
            webViewState = Bundle()
        }
        if (tabView != null) {
            this.title = tabView!!.title
            this.url = tabView!!.url
            tabView!!.saveViewState(this.webViewState)
        }
    }

    fun isValid(): Boolean {
        return id.isNotBlank() && (url?.isNotBlank() ?: false)
    }

    internal fun setTabViewClient(client: TabViewClient?) {
        tabViewClient = client ?: sDefViewClient
    }

    internal fun setTabChromeClient(client: TabChromeClient?) {
        tabChromeClient = client ?: sDefChromeClient
    }

    internal fun setDownloadCallback(callback: DownloadCallback?) {
        downloadCallback = callback
    }

    internal fun setFindListener(listener: FindListener?) {
        findListener = listener
        if (tabView != null) {
            tabView!!.setFindListener(listener)
        }
    }

    fun setContentBlockingEnabled(enabled: Boolean) {
        if (tabView != null) {
            tabView!!.setContentBlockingEnabled(enabled)
        }
    }

    fun setImageBlockingEnabled(enabled: Boolean) {
        if (tabView != null) {
            tabView!!.setImageBlockingEnabled(enabled)
        }
    }

    fun hasParentTab(): Boolean {
        return !isFromExternal && !TextUtils.isEmpty(parentId)
    }

    /**
     * To detach @see{android.view.View} of this tab, if any, is detached from its parent.
     */
    fun detach() {
        val hasParentView = (tabView != null
                && tabView!!.view != null
                && tabView!!.view.parent != null)
        if (hasParentView) {
            val parent = tabView!!.view.parent as ViewGroup
            parent.removeView(tabView!!.view)
        }
    }

    /* package */ internal fun destroy() {
        setDownloadCallback(null)
        setFindListener(null)
        setTabViewClient(null)

        if (tabView != null) {
            // ensure the view not bind to parent
            detach()

            tabView!!.destroy()
        }
    }

    /* package */ internal fun resume() {
        if (tabView != null) {
            tabView!!.onResume()
        }
    }

    /* package */ internal fun pause() {
        if (tabView != null) {
            tabView!!.onPause()
        }
    }

    /* package */ internal fun initializeView(provider: TabViewProvider): TabView? {
        val url = this.url // fallback for restoring tab
        if (tabView == null) {
            tabView = provider.create()

            tabView!!.setViewClient(tabViewClient)
            tabView!!.setChromeClient(tabChromeClient)
            tabView!!.setDownloadCallback(downloadCallback)
            tabView!!.setFindListener(findListener)

            if (webViewState != null) {
                tabView!!.restoreViewState(webViewState)
            } else if (!TextUtils.isEmpty(url)) {
                tabView!!.loadUrl(url)
            }
        }

        return tabView
    }

    // TODO: not implement completely
    private fun updateThumbnail() {
        if (tabView != null) {
            val view = tabView!!.view
            view.isDrawingCacheEnabled = true
            tabView!!.getDrawingCache(true)?.let { bitmap ->
                this.thumbnail = Bitmap.createBitmap(bitmap)
                bitmap.recycle()
            }

            view.isDrawingCacheEnabled = false
        }
    }

    companion object {

        const val ID_EXTERNAL = "_open_from_external_"

        /**
         * A placeholder in case of there is no callback to use.
         */
        private val sDefViewClient = TabViewClient()
        private val sDefChromeClient = TabChromeClient()
    }
}
