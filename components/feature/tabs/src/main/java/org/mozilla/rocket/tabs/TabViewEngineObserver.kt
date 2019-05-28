/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs

import android.graphics.Bitmap
import android.os.Environment
import android.view.View
import android.webkit.GeolocationPermissions
import mozilla.components.browser.session.Download
import mozilla.components.browser.session.Session.FindResult
import mozilla.components.browser.session.Session.SecurityInfo
import mozilla.components.support.base.observer.Consumable
import org.mozilla.rocket.tabs.TabView.HitTarget

/**
 * This class is a simulation of EngineSession of Mozilla Android-Components. To be an abstraction
 * between TabView and Session/SessionManager. Then we can do two things independently.
 * 1. Refactor Session/SessionManager without worrying TabView.
 * 2. Refactor TabView/WebkitView without worrying Session.
 *
 * This abstraction layer will be removed once we complete migration from TabView to EngineView.
 */
class TabViewEngineObserver(
    val session: Session
) : TabViewEngineSession.Observer {

    override fun onTitleChange(title: String) {
        session.title = title
    }

    override fun onLoadingStateChange(loading: Boolean) {
        session.loading = loading
        // TODO: clear find result, just like AC EngineObserver did.
    }

    override fun onNavigationStateChange(canGoBack: Boolean?, canGoForward: Boolean?) {
        canGoBack?.let { session.canGoBack = canGoBack }
        canGoForward?.let { session.canGoForward = canGoForward }
    }

    override fun onSecurityChange(secure: Boolean, host: String?, issuer: String?) {
        session.securityInfo = SecurityInfo(secure, host ?: "", issuer ?: "")
    }

    override fun onLocationChange(url: String) {
        session.url = url
    }

    override fun onProgress(progress: Int) {
        session.progress = progress
    }

    override fun onReceivedIcon(icon: Bitmap?) {
        session.favicon = icon
        session.notifyObservers { onReceivedIcon(icon) }
    }

    override fun onLongPress(hitTarget: HitTarget) {
        session.notifyObservers { onLongPress(session, hitTarget) }
    }

    override fun onEnterFullScreen(callback: TabView.FullscreenCallback, view: View?) =
            session.notifyObservers { onEnterFullScreen(callback, view) }

    override fun onExitFullScreen() = session.notifyObservers { onExitFullScreen() }

    override fun onGeolocationPermissionsShowPrompt(
        origin: String,
        callback: GeolocationPermissions.Callback?
    ) =
            session.notifyObservers { onGeolocationPermissionsShowPrompt(origin, callback) }

    override fun onFindResult(
        activeMatchOrdinal: Int,
        numberOfMatches: Int,
        isDoneCounting: Boolean
    ) {
        session.findResults += FindResult(activeMatchOrdinal, numberOfMatches, isDoneCounting)
    }

    override fun onExternalResource(
        url: String,
        fileName: String,
        contentLength: Long?,
        contentType: String?,
        cookie: String?,
        userAgent: String?
    ) {

        val download = Download(url, fileName, contentType, contentLength, userAgent, Environment.DIRECTORY_DOWNLOADS)
        session.download = Consumable.from(download)
    }
}
