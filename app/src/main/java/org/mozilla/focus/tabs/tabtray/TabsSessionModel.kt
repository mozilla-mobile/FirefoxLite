/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient

import org.mozilla.focus.BuildConfig
import org.mozilla.rocket.tabs.Session
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.SessionManager.Observer
import org.mozilla.rocket.tabs.TabView

import java.util.ArrayList

internal class TabsSessionModel(private val sessionManager: SessionManager) : TabTrayContract.Model {
    private var modelObserver: SessionModelObserver? = null

    private val tabs = ArrayList<Session>()

    override fun loadTabs(listener: TabTrayContract.Model.OnLoadCompleteListener?) {
        tabs.clear()
        tabs.addAll(sessionManager.getTabs())

        listener?.onLoadComplete()
    }

    override fun getTabs(): List<Session> {
        return tabs
    }

    override fun getFocusedTab(): Session? {
        return sessionManager.focusSession
    }

    override fun switchTab(tabPosition: Int) {
        if (tabPosition >= 0 && tabPosition < tabs.size) {
            val target = tabs[tabPosition]
            val latestTabs = sessionManager.getTabs()
            val exist = latestTabs.indexOf(target) != -1
            if (exist) {
                sessionManager.switchToTab(target.id)
            }
        } else {
            if (BuildConfig.DEBUG) {
                throw ArrayIndexOutOfBoundsException("index: " + tabPosition + ", size: " + tabs.size)
            }
        }
    }

    override fun removeTab(tabPosition: Int) {
        if (tabPosition >= 0 && tabPosition < tabs.size) {
            sessionManager.dropTab(tabs[tabPosition].id)
        } else {
            if (BuildConfig.DEBUG) {
                throw ArrayIndexOutOfBoundsException("index: " + tabPosition + ", size: " + tabs.size)
            }
        }
    }

    override fun clearTabs() {
        val tabs = sessionManager.getTabs()
        for (tab in tabs) {
            sessionManager.dropTab(tab.id)
        }
    }

    override fun subscribe(observer: TabTrayContract.Model.Observer) {
        if (this.modelObserver == null) {
            this.modelObserver = object : SessionModelObserver() {
                override fun onTabModelChanged(session: Session) {
                    observer.onTabUpdate(session)
                }

                override fun onSessionCountChanged(count: Int) {
                    observer.onUpdate(sessionManager.getTabs())
                }
            }
        }
        sessionManager.register(this.modelObserver as Observer)
    }

    override fun unsubscribe() {
        if (modelObserver != null) {
            sessionManager.unregister(modelObserver as Observer)
            modelObserver = null
        }
    }

    private abstract class SessionModelObserver : SessionManager.Observer {
        override fun onSessionStarted(session: Session) {}

        override fun onSessionFinished(session: Session, isSecure: Boolean) {}

        override fun onURLChanged(session: Session, url: String?) {
            onTabModelChanged(session)
        }

        override fun handleExternalUrl(url: String?): Boolean {
            return false
        }

        override fun updateFailingUrl(session: Session, url: String?, updateFromError: Boolean) {
            onTabModelChanged(session)
        }

        override fun onProgressChanged(session: Session, progress: Int) {}

        override fun onReceivedTitle(session: Session, title: String?) {
            onTabModelChanged(session)
        }

        override fun onReceivedIcon(session: Session, icon: Bitmap?) {
            onTabModelChanged(session)
        }

        override fun onFocusChanged(session: Session?, factor: SessionManager.Factor) {}

        override fun onSessionAdded(session: Session, arguments: Bundle?) {}

        override fun onSessionCountChanged(count: Int) {}

        override fun onLongPress(session: Session, hitTarget: TabView.HitTarget?) {}

        override fun onEnterFullScreen(session: Session, callback: TabView.FullscreenCallback, fullscreenContent: View?) {}

        override fun onExitFullScreen(session: Session) {}

        override fun onShowFileChooser(session: Session, filePathCallback: ValueCallback<Array<Uri>>, fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
            return false
        }

        override fun onGeolocationPermissionsShowPrompt(session: Session, origin: String?, callback: GeolocationPermissions.Callback?) {

        }

        internal abstract fun onTabModelChanged(session: Session)
    }
}
