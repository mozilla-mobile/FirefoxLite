/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import mozilla.components.support.base.observer.Consumable
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import org.mozilla.rocket.tabs.Session.Observer
import org.mozilla.rocket.tabs.TabView.FindListener
import org.mozilla.rocket.tabs.web.DownloadCallback
import java.util.UUID

const val TAG = "Session"

class Session @JvmOverloads constructor(
        val id: String = UUID.randomUUID().toString(),
        var parentId: String? = "",
        var title: String? = "",
        var url: String? = "",
        private val delegate: Observable<Observer> = ObserverRegistry()
) : Observable<Observer> by delegate {

    var tabView: TabView? = null
        private set

    private var tabViewClient = ViewClient(this)
    private var tabChromeClient = ChromeClient(this)
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
        unregisterObservers()

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

    interface Observer {
        fun onSessionStarted(url: String?) = Unit

        fun onSessionFinished(isSecure: Boolean) = Unit

        fun onURLChanged(url: String?) = Unit

        /**
         * Return true if the URL was handled, false if we should continue loading the current URL.
         */
        fun handleExternalUrl(url: String?): Boolean = false

        fun updateFailingUrl(url: String?, updateFromError: Boolean) = Unit

        fun onCreateWindow(isDialog: Boolean, isUserGesture: Boolean, msg: Message?) = false

        fun onCloseWindow(tabView: TabView?) = Unit

        fun onProgressChanged(progress: Int) = Unit


        /**
         * @see android.webkit.WebChromeClient
         */
        fun onShowFileChooser(tabView: TabView,
                              filePathCallback: ValueCallback<Array<Uri>>,
                              fileChooserParams: WebChromeClient.FileChooserParams) = false

        fun onReceivedTitle(view: TabView, title: String?) = Unit

        fun onReceivedIcon(view: TabView, icon: Bitmap?) = Unit

        fun onLongPress(hitTarget: TabView.HitTarget) = Unit

        /**
         * Notify the host application that the current page has entered full screen mode.
         * <p>
         * The callback needs to be invoked to request the page to exit full screen mode.
         * <p>
         * Some TabView implementations may pass a custom View which contains the web contents in
         * full screen mode.
         */
        fun onEnterFullScreen(callback: TabView.FullscreenCallback, view: View?) = Unit

        /**
         * Notify the host application that the current page has exited full screen mode.
         * <p>
         * If a View was passed when the application entered full screen mode then this view must
         * be hidden now.
         */
        fun onExitFullScreen() = Unit

        fun onGeolocationPermissionsShowPrompt(origin: String,
                                               callback: GeolocationPermissions.Callback?) = Unit
    }

    class ViewClient(val session: Session) : TabViewClient() {

        override fun onPageStarted(url: String?) = session.notifyObservers { onSessionStarted(url) }

        override fun onPageFinished(isSecure: Boolean) =
                session.notifyObservers { onSessionFinished(isSecure) }

        override fun onURLChanged(url: String?) = session.notifyObservers { onURLChanged(url) }

        override fun updateFailingUrl(url: String?, updateFromError: Boolean) =
                session.notifyObservers { updateFailingUrl(url, updateFromError) }

        override fun handleExternalUrl(url: String?): Boolean {
            var consumers = session.wrapConsumers<String?> { url -> handleExternalUrl(url) }
            return Consumable.from(url).consumeBy(consumers)
        }
    }

    class ChromeClient(val session: Session) : TabChromeClient() {
        override fun onCreateWindow(
                isDialog: Boolean,
                isUserGesture: Boolean,
                msg: Message?): Boolean {
            val consumers = session.wrapConsumers<Triple<Boolean, Boolean, Message?>> {
                onCreateWindow(it.first, it.second, it.third)
            }
            val args = Triple(isDialog, isUserGesture, msg)
            return Consumable.from(args).consumeBy(consumers)
        }

        override fun onCloseWindow(tabView: TabView?) =
                session.notifyObservers { onCloseWindow(tabView) }

        override fun onProgressChanged(progress: Int) =
                session.notifyObservers { onProgressChanged(progress) }

        override fun onShowFileChooser(
                tabView: TabView,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?): Boolean {

            val consumers = session.wrapConsumers<Triple<TabView, ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams>> {
                this.onShowFileChooser(it.first, it.second, it.third)
            }
            val args = Triple(tabView, filePathCallback!!, fileChooserParams!!)
            return !Consumable.from(args).consumeBy(consumers)
        }

        override fun onReceivedTitle(view: TabView, title: String?) =
                session.notifyObservers { onReceivedTitle(view, title) }

        override fun onReceivedIcon(view: TabView, icon: Bitmap?) =
                session.notifyObservers { onReceivedIcon(view, icon) }

        override fun onLongPress(hitTarget: TabView.HitTarget) =
                session.notifyObservers { onLongPress(hitTarget) }

        override fun onEnterFullScreen(callback: TabView.FullscreenCallback, view: View?) =
                session.notifyObservers { onEnterFullScreen(callback, view) }

        override fun onExitFullScreen() = session.notifyObservers { onExitFullScreen() }

        override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback?) =
                session.notifyObservers { onGeolocationPermissionsShowPrompt(origin, callback) }
    }

    companion object {
        const val ID_EXTERNAL = "_open_from_external_"
    }
}
