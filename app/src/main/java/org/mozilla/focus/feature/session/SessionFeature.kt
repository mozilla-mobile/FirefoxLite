/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.feature.session

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import org.mozilla.rocket.tabs.Session
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabView
import org.mozilla.rocket.tabs.TabViewEngineSession
import org.mozilla.rocket.tabs.utils.TabUtil

/**
 * Contains the details of using Session and SessionManager
 */
class SessionFeature(
        val sessionManager: SessionManager,
        val webViewSlot: ViewGroup
) {

    var canGoForward: Boolean = false
        get() {
            return sessionManager.focusSession?.canGoForward ?: false
        }

    var canGoBack: Boolean = false
        get() {
            return sessionManager.focusSession?.canGoBack ?: false
        }

    private var focusEngineSession: TabViewEngineSession? = null
        get() {
            return sessionManager.focusSession?.let { sessionManager.getOrCreateEngineSession(it) }
        }

    fun add(url: String, parentId: String?, isFromExternal: Boolean, toFocus: Boolean) {
        sessionManager.addTab(url, TabUtil.argument(parentId, isFromExternal, toFocus))
    }

    fun loadUrl(url: String): Boolean {
        focusEngineSession?.tabView?.loadUrl(url) ?: return false
        return true
    }

    fun goForeground() {
        focusEngineSession?.tabView?.let {
            if (webViewSlot.childCount == 0) {
                webViewSlot.addView(it.view)
            }
        }
    }

    fun goBackground() {
        val es = focusEngineSession ?: return
        es.detach()
        es.tabView?.let { webViewSlot.removeView(it.view) }
    }

    fun goBack(): String? {
        val tabView = focusEngineSession?.tabView ?: return null

        val nextUrl = if (tabView is WebView) {
            val webBackForwardList = tabView.copyBackForwardList()
            val item = webBackForwardList.getItemAtIndex(webBackForwardList.currentIndex - 1)
            item.url
        } else null

        focusEngineSession?.goBack()

        return nextUrl
    }

    fun goForward(): String? {
        val tabView = focusEngineSession?.tabView ?: return null

        val nextUrl = if (tabView is WebView) {
            val webBackForwardList = tabView.copyBackForwardList()
            val item = webBackForwardList.getItemAtIndex(webBackForwardList.currentIndex + 1)
            item.url
        } else null

        focusEngineSession?.goForward()

        return nextUrl
    }

    fun reload() = focusEngineSession?.reload()
    fun stop() = focusEngineSession?.stopLoading()

    fun saveSession(outState: Bundle) {
        focusEngineSession?.tabView?.saveViewState(outState)
    }

    fun restoreSession(savedState: Bundle) {
        // FIXME: Obviously, only restore current tab is not enough
        val focusSession = sessionManager.focusSession ?: return
        val tabView = sessionManager.getOrCreateEngineSession(focusSession).tabView
        if (tabView != null) {
            tabView.restoreViewState(savedState)
        } else {
            // Focus to tab again to force initialization.
            sessionManager.switchToTab(focusSession.id)
        }
    }

    fun capturePage(): WebView? {
        val tabView = focusEngineSession?.tabView ?: return null
        return if (tabView is WebView) tabView else null
    }

    fun existsFullScreen() = focusEngineSession?.tabView?.performExitFullScreen();

    fun setContentBlockingEnabled(enabled: Boolean) {
        // TODO: Better if we can move this logic to some setting-like classes, and provider interface
        // for configuring blocking function of each tab.
        for (session in sessionManager.getTabs()) {
            val es = sessionManager.getEngineSession(session)
            es?.tabView?.setContentBlockingEnabled(enabled)
        }
    }

    fun setImageBlockingEnabled(enabled: Boolean) {
        // TODO: Better if we can move this logic to some setting-like classes, and provider interface
        // for configuring blocking function of each tab.
        for (session in sessionManager.getTabs()) {
            val es = sessionManager.getEngineSession(session)
            es?.tabView?.setImageBlockingEnabled(enabled)
        }
    }

    fun prepareViewsOnTransition(target: Session): Pair<View?, View> {
        val es = sessionManager.getEngineSession(target)
        val inView = es?.tabView?.view
                ?: throw java.lang.RuntimeException("Tabview should be created at this moment and never be null")
        // ensure it does not have attach to parent earlier.
        es.detach()

        val outView = findExistingTabView(webViewSlot)
        return Pair(outView, inView)
    }

    private fun findExistingTabView(parent: ViewGroup): View? {
        val viewCount = parent.childCount
        for (childIdx in 0..viewCount) {
            val childView = parent.getChildAt(childIdx)
            if (childView is TabView) {
                return childView.view
            }
        }
        return null
    }
}
