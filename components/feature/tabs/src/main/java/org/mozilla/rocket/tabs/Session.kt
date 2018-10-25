/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs

import android.graphics.Bitmap
import android.text.TextUtils
import android.view.View
import android.webkit.GeolocationPermissions
import mozilla.components.browser.session.Download
import mozilla.components.browser.session.Session.FindResult
import mozilla.components.browser.session.Session.SecurityInfo
import mozilla.components.support.base.observer.Consumable
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import org.mozilla.rocket.tabs.Session.Observer
import java.util.UUID
import kotlin.properties.Delegates

const val TAG = "Session"

class Session @JvmOverloads constructor(
        val id: String = UUID.randomUUID().toString(),
        var parentId: String? = "",
        var initialUrl: String? = "",
        private val delegate: Observable<Observer> = ObserverRegistry()
) : Observable<Observer> by delegate {

    var engineSession: TabViewEngineSession? = null
    var engineObserver: TabViewEngineSession.Observer? = null

    var favicon: Bitmap? = null

    val isFromExternal: Boolean
        get() = ID_EXTERNAL == parentId

    /**
     * The currently loading or loaded URL.
     */
    var url: String? by Delegates.observable(initialUrl) { _, old, new ->
        if (old != null && new != null) {
            notifyObservers(old, new) { onUrlChanged(this@Session, new) }
        }
    }

    /**
     * The title of the currently displayed website changed.
     */
    var title: String by Delegates.observable("") { _, old, new ->
        notifyObservers(old, new) { onTitleChanged(this@Session, new) }
    }

    /**
     * The progress loading the current URL.
     */
    var progress: Int by Delegates.observable(0) { _, old, new ->
        notifyObservers(old, new) { onProgress(this@Session, new) }
    }

    /**
     * Loading state, true if this session's url is currently loading, otherwise false.
     */
    var loading: Boolean by Delegates.observable(false) { _, old, new ->
        notifyObservers(old, new) { onLoadingStateChanged(this@Session, new) }
    }

    /**
     * Security information indicating whether or not the current session is
     * for a secure URL, as well as the host and SSL certificate authority, if applicable.
     */
    var securityInfo: SecurityInfo by Delegates.observable(SecurityInfo()) { _, old, new ->
        notifyObservers(old, new) { onSecurityChanged(this@Session, new.secure) }
    }

    /**
     * Last download request if it wasn't consumed by at least one observer.
     */
    var download: Consumable<Download> by Delegates.vetoable(Consumable.empty()) { _, _, download ->
        val consumers = wrapConsumers<Download> { onDownload(this@Session, it) }
        !download.consumeBy(consumers)
    }

    /**
     * List of results of that latest "find in page" operation.
     */
    var findResults: List<FindResult> by Delegates.observable(emptyList()) { _, old, new ->
        notifyObservers(old, new) {
            if (new.isNotEmpty()) {
                onFindResult(this@Session, findResults.last())
            }
        }
    }

    fun isValid(): Boolean {
        return id.isNotBlank() && (url?.isNotBlank() ?: false)
    }

    fun hasParentTab(): Boolean {
        return !isFromExternal && !TextUtils.isEmpty(parentId)
    }

    /**
     * Helper method to notify observers.
     */
    private fun notifyObservers(old: Any, new: Any, block: Observer.() -> Unit) {
        if (old != new) {
            notifyObservers(block)
        }
    }

    interface Observer {
        fun onLoadingStateChanged(session: Session, loading: Boolean) = Unit
        fun onSecurityChanged(session: Session, isSecure: Boolean) = Unit
        fun onUrlChanged(session: Session, url: String?) = Unit
        fun onProgress(session: Session, progress: Int) = Unit
        fun onTitleChanged(session: Session, title: String?) = Unit
        fun onReceivedIcon(icon: Bitmap?) = Unit
        fun onLongPress(session: Session, hitTarget: TabView.HitTarget) = Unit
        fun onDownload(session: Session, download: Download): Boolean = false
        fun onFindResult(session: Session, result: FindResult) = Unit

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

    companion object {
        const val ID_EXTERNAL = "_open_from_external_"
    }
}
