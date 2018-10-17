/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Message
import android.view.View
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import mozilla.components.concept.engine.EngineSession
import mozilla.components.support.base.observer.Consumable
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry

/**
 * This class is a simulation of EngineSession of Mozilla Android-Components. To be an abstraction
 * between TabView and Session/SessionManager. Then we can do two things independently.
 * 1. Refactor Session/SessionManager without worrying TabView.
 * 2. Refactor TabView/WebkitView without worrying Session.
 *
 * This abstraction layer will be removed once we complete migration from TabView to EngineView.
 */

class TabViewEngineSession constructor(
        private val delegate: Observable<TabViewEngineSession.Observer> = ObserverRegistry()
) : Observable<TabViewEngineSession.Observer> by delegate {

    var webViewState: Bundle? = null

    var tabView: TabView?
        set(value) {
            value?.setViewClient(ViewClient(this))
            value?.setChromeClient(ChromeClient(this))
            engineView?.setViewClient(null)
            engineView?.setChromeClient(null)
            engineView = value
        }
        get() = engineView

    private var engineView: TabView? = null

    fun goBack() = tabView?.goBack()
    fun goForward() = tabView?.goForward()
    fun loadUrl(url: String) = tabView?.loadUrl(url)
    fun reload() = tabView?.reload()
    fun stopLoading() = tabView?.stopLoading()

    /**
     * To sync session's properties to view, before saving. This method would be retired once we
     * involve Observable class for those properties.
     */
    fun saveState() {
        if (webViewState == null) {
            webViewState = Bundle()
        }
        tabView?.let {
            //TODO: should we update latest url, title of TabView to Session?
            it.saveViewState(webViewState)
        }
    }

    /**
     * To detach @see{android.view.View} of this tab, if any, is detached from its parent.
     */
    fun detach() {
        tabView?.view?.let { v ->
            v.parent?.let { p ->
                val parent = p as ViewGroup
                parent.removeView(v)
            }
        }
    }

    internal fun destroy() {
        unregisterObservers()
        // ensure the view not bind to parent
        detach()
        tabView?.destroy()
    }


    interface Observer : EngineSession.Observer {
        fun updateFailingUrl(url: String?, updateFromError: Boolean)
        fun handleExternalUrl(url: String?): Boolean
        fun onCreateWindow(isDialog: Boolean,
                           isUserGesture: Boolean,
                           msg: Message?): Boolean

        fun onCloseWindow(es: TabViewEngineSession)

        fun onShowFileChooser(
                es: TabViewEngineSession,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?): Boolean

        fun onReceivedIcon(icon: Bitmap?)
        fun onLongPress(hitTarget: TabView.HitTarget)
        fun onEnterFullScreen(callback: TabView.FullscreenCallback, view: View?)
        fun onExitFullScreen()
        fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback?)
    }

    class ViewClient(private val es: TabViewEngineSession) : TabViewClient() {
        override fun onPageStarted(url: String?) {
            es.notifyObservers { onLoadingStateChange(true) }
            url?.let { es.notifyObservers { onLocationChange(it) } }
        }

        override fun onPageFinished(isSecure: Boolean) {
            es.notifyObservers { onLoadingStateChange(false) }
            es.notifyObservers { onSecurityChange(isSecure) }
        }

        override fun onURLChanged(url: String?) {
            url?.let { es.notifyObservers { onLocationChange(it) } }
        }

        override fun updateFailingUrl(url: String?, updateFromError: Boolean) {
            es.notifyObservers { updateFailingUrl(url, updateFromError) }
        }

        override fun handleExternalUrl(url: String?): Boolean {
            var consumers = es.wrapConsumers<String?> { url -> handleExternalUrl(url) }
            return Consumable.from(url).consumeBy(consumers)
        }
    }

    class ChromeClient(private val es: TabViewEngineSession) : TabChromeClient() {
        override fun onCreateWindow(
                isDialog: Boolean,
                isUserGesture: Boolean,
                msg: Message?): Boolean {
            val consumers = es.wrapConsumers<Triple<Boolean, Boolean, Message?>> {
                onCreateWindow(it.first, it.second, it.third)
            }
            val args = Triple(isDialog, isUserGesture, msg)
            return Consumable.from(args).consumeBy(consumers)
        }

        override fun onCloseWindow(tabView: TabView?) =
                es.notifyObservers { onCloseWindow(es) }

        override fun onProgressChanged(progress: Int) =
                es.notifyObservers { onProgress(progress) }

        override fun onShowFileChooser(
                tabView: TabView,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: WebChromeClient.FileChooserParams?): Boolean {

            val consumers = es.wrapConsumers<Triple<TabViewEngineSession, ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams>> {
                this.onShowFileChooser(it.first, it.second, it.third)
            }
            val args = Triple(es, filePathCallback!!, fileChooserParams!!)
            return Consumable.from(args).consumeBy(consumers)
        }

        override fun onReceivedTitle(view: TabView, title: String?) {
            if (title != null) {
                es.notifyObservers { onTitleChange(title) }
            }
        }

        override fun onReceivedIcon(view: TabView, icon: Bitmap?) =
                es.notifyObservers { onReceivedIcon(icon) }

        override fun onLongPress(hitTarget: TabView.HitTarget) =
                es.notifyObservers { onLongPress(hitTarget) }

        override fun onEnterFullScreen(callback: TabView.FullscreenCallback, view: View?) =
                es.notifyObservers { onEnterFullScreen(callback, view) }

        override fun onExitFullScreen() = es.notifyObservers { onExitFullScreen() }

        override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback?) =
                es.notifyObservers { onGeolocationPermissionsShowPrompt(origin, callback) }
    }
}
