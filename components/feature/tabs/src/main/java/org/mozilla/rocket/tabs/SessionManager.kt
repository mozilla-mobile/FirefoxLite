/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import android.view.View
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import mozilla.components.support.base.observer.Consumable
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import org.mozilla.rocket.tabs.SessionManager.Factor.FACTOR_BACK_EXTERNAL
import org.mozilla.rocket.tabs.SessionManager.Factor.FACTOR_NO_FOCUS
import org.mozilla.rocket.tabs.SessionManager.Factor.FACTOR_TAB_ADDED
import org.mozilla.rocket.tabs.SessionManager.Factor.FACTOR_TAB_REMOVED
import org.mozilla.rocket.tabs.SessionManager.Factor.FACTOR_TAB_SWITCHED
import org.mozilla.rocket.tabs.SessionManager.Observer
import org.mozilla.rocket.tabs.TabView.FindListener
import org.mozilla.rocket.tabs.utils.TabUtil
import org.mozilla.rocket.tabs.web.DownloadCallback
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.LinkedList

internal val MSG_FOCUS_TAB = 0x1001
internal val MSG_ADDED_TAB = 0x1002

typealias FileChooserArgs = Triple<Session, ValueCallback<Array<Uri>>, WebChromeClient.FileChooserParams>

/**
 * Class to help on sessions management, such as adding or removing sessions.
 */
class SessionManager @JvmOverloads constructor(
        private val tabViewProvider: TabViewProvider,
        delegate: Observable<Observer> = ObserverRegistry()
) : Observable<Observer> by delegate {


    private val sessions = LinkedList<Session>()

    private val notifier: Notifier

    private var focusRef = WeakReference<Session>(null)

    private var downloadCallback: DownloadCallback? = null
    private var findListener: FindListener? = null

    /**
     * To get count of sessions in this session.
     *
     * @return count in integer
     */
    val tabsCount: Int
        get() = sessions.size

    /**
     * To get current focused tab.
     *
     * @return current focused tab. Return null if there is not any tab.
     */
    val focusSession: Session?
        get() = focusRef.get()

    init {
        this.notifier = Notifier(tabViewProvider, this)
    }

    /**
     * Copy reference of sessions which are held by this session.
     *
     * @return new List which is safe to change its order without effect this session
     */
    fun getTabs(): List<Session> {
        // create a new list, in case of caller modify this list
        return ArrayList(sessions)
    }

    /**
     * To append sessions from a list of TabModel. If the specified focusTabId exists, the tab associate
     * to the id will be focused, otherwise no tab will be focused.
     *
     *
     * This is asynchronous call.
     * TODO: make it asynchronous
     *
     * @param sessionList
     */
    fun restoreTabs(sessionList: List<Session>, focusTabId: String?) {
        var insertPos = 0
        for (session in sessionList) {
            if (!session.isValid()) {
                continue
            }

            bindCallback(session)
            this.sessions.add(insertPos++, session)
        }

        if (this.sessions.size > 0 && this.sessions.size == sessionList.size) {
            focusRef = WeakReference<Session>(getTab(focusTabId))
        }

        notifyObservers { onSessionCountChanged(sessions.size) }
    }

    /**
     * Add a tab, its attributes are controlled by arguments. The default attributes of a new tab
     * is defined by @see{org.mozilla.focus.sessions.utils.TabUtil}. Usually it 1) has no parent
     * 2) is not opened from external app 3) will not change focus
     *
     * @param url       initial url for this tab
     * @param arguments to contain dispensable arguments to indicate whether the new tab is from-external? should be focused?...etc.
     * @return id for created tab
     */
    fun addTab(url: String, arguments: Bundle): String? {

        return if (TextUtils.isEmpty(url)) {
            null
        } else addTabInternal(url,
                TabUtil.getParentId(arguments),
                TabUtil.isFromExternal(arguments),
                TabUtil.toFocus(arguments),
                arguments)

    }

    /**
     * To drop a tab from list, it will not invoke callback onFocusChanged, and only change focus to nearest tab.
     *
     * @param id the id of tab to be dropped
     */
    fun dropTab(id: String) {
        this.removeTabInternal(id, true)
    }

    /**
     * To close a tab by remove it from list and update tab focus.
     *
     * @param id the id of tab to be closed.
     */
    fun closeTab(id: String) {
        this.removeTabInternal(id, false)
    }

    private fun removeTabInternal(id: String, isDrop: Boolean) {
        val tab = getTab(id) ?: return

        val oldIndex = getTabIndex(id)
        sessions.remove(tab)
        tab.destroy()

        // Update child's parent id to its ancestor
        // TODO: in our current design, the parent of a tab are always locate at left(index -1).
        // hence no need to loop whole list.
        for (t in sessions) {
            if (TextUtils.equals(t.parentId, tab.id)) {
                t.parentId = tab.parentId
            }
        }

        // if the removing tab was focused, we need to update focus
        if (tab === focusRef.get()) {
            if (isDrop) {
                val nextIdx = Math.min(oldIndex, sessions.size - 1)
                focusRef = if (nextIdx == -1)
                    WeakReference<Session>(null)
                else
                    WeakReference(sessions[nextIdx])
            } else {
                updateFocusOnClosing(tab)
            }
        }

        notifyObservers { onSessionCountChanged(sessions.size) }
    }

    private fun updateFocusOnClosing(removedSession: Session) {
        if (TextUtils.isEmpty(removedSession.parentId)) {
            focusRef.clear()
            notifier.notifyTabFocused(null, FACTOR_NO_FOCUS)
        } else if (TextUtils.equals(removedSession.parentId, Session.ID_EXTERNAL)) {
            focusRef.clear()
            notifier.notifyTabFocused(null, FACTOR_BACK_EXTERNAL)
        } else {
            focusRef = WeakReference<Session>(getTab(removedSession.parentId!!))
            notifier.notifyTabFocused(focusRef.get(), FACTOR_TAB_REMOVED)
        }
    }

    /**
     * To focus a tab from list.
     *
     * @param id the id of tab to be focused.
     */
    fun switchToTab(id: String) {
        val nextTab = getTab(id)
        if (nextTab != null) {
            focusRef = WeakReference(nextTab)
        }

        notifier.notifyTabFocused(nextTab, FACTOR_TAB_SWITCHED)
    }

    /**
     * To check whether this session has any sessions
     *
     * @return true if this session has at least one tab
     */
    fun hasTabs(): Boolean {
        return sessions.size > 0
    }

    /**
     * To specify @see{DownloadCallback} to this session, this method will replace existing one. It
     * also replace DownloadCallback from any existing Session.
     *
     * @param downloadCallback
     */
    fun setDownloadCallback(downloadCallback: DownloadCallback?) {
        this.downloadCallback = downloadCallback
        if (hasTabs()) {
            for (tab in sessions) {
                tab.setDownloadCallback(downloadCallback)
            }
        }
    }

    fun setFindListener(findListener: FindListener?) {
        this.findListener = findListener
        if (hasTabs()) {
            for (tab in sessions) {
                tab.setFindListener(findListener)
            }
        }
    }

    /**
     * To destroy this session, and it also destroy any sessions in this session.
     * This method should be called after any View has been removed from view system.
     * No other methods may be called on this session after destroy.
     */
    fun destroy() {
        for (tab in sessions) {
            tab.destroy()
        }
    }

    /**
     * To pause this session, and it also pause any sessions in this session.
     */
    fun pause() {
        for (tab in sessions) {
            tab.pause()
        }
    }

    /**
     * To resume this session after a previous call to @see{#pause}
     */
    fun resume() {
        for (tab in sessions) {
            tab.resume()
        }
    }

    private fun bindCallback(session: Session) {
        session.register(SessionObserver(session))
        session.setDownloadCallback(downloadCallback)
        session.setFindListener(findListener)
    }

    private fun addTabInternal(url: String?,
                               parentId: String?,
                               fromExternal: Boolean,
                               toFocus: Boolean,
                               arguments: Bundle?): String {

        val tab = Session()
        tab.url = url

        bindCallback(tab)

        val parentIndex = if (TextUtils.isEmpty(parentId)) -1 else getTabIndex(parentId!!)
        if (fromExternal) {
            tab.parentId = Session.ID_EXTERNAL
            sessions.add(tab)
        } else {
            insertTab(parentIndex, tab)
        }

        notifier.notifyTabAdded(tab, arguments)

        focusRef = if (toFocus || fromExternal) WeakReference(tab) else focusRef

        tab.initializeView(this.tabViewProvider)

        if (toFocus || fromExternal) {
            notifier.notifyTabFocused(tab, FACTOR_TAB_ADDED)
        }

        notifyObservers { onSessionCountChanged(sessions.size) }
        return tab.id
    }

    private fun getTab(id: String?): Session? {
        val index = getTabIndex(id)
        return if (index == -1) null else sessions[index]
    }

    private fun getTabIndex(id: String?): Int {
        if (id == null) {
            return -1
        }

        for (i in sessions.indices) {
            val tab = sessions[i]
            if (tab.id == id) {
                return i
            }
        }
        return -1
    }

    private fun insertTab(parentIdx: Int, session: Session) {
        val parentTab = if (parentIdx >= 0 && parentIdx < sessions.size)
            sessions[parentIdx]
        else
            null
        if (parentTab == null) {
            sessions.add(session)
            return
        } else {
            sessions.add(parentIdx + 1, session)
        }

        // TODO: in our current design, the parent of a session are always locate at left(index -1).
        //       hence no need to loop whole list.
        // if the parent-session has a child, give it a new parent
        for (t in sessions) {
            if (parentTab.id == t.parentId) {
                t.parentId = session.id
            }
        }

        // update family relationship
        session.parentId = parentTab.id
    }

    internal inner class SessionObserver(var source: Session) : Session.Observer {
        private fun setTitle() {
            if (source.tabView == null) {
                return
            }
            source.title = source.tabView!!.title
        }

        override fun onSessionStarted(url: String?) {
            source.url = url
            setTitle()

            // FIXME: workaround for 'dialog new window'
            if (source.url != null) {
                notifyObservers { onSessionStarted(source) }
            }
        }

        override fun onSessionFinished(isSecure: Boolean) {
            setTitle()
            notifyObservers { onSessionFinished(source, isSecure) }
        }

        override fun onURLChanged(url: String?) {
            source.url = url
            setTitle()
            notifyObservers { onURLChanged(source, url) }
        }

        override fun handleExternalUrl(url: String?): Boolean {
            // only return false if none of listeners handled external url.
            val consumers = wrapConsumers<String?> { handleExternalUrl(it) }
            return Consumable.from(url).consumeBy(consumers)
        }

        override fun updateFailingUrl(url: String?, updateFromError: Boolean) {
            notifyObservers { updateFailingUrl(source, url, updateFromError) }
        }

        override fun onCreateWindow(
                isDialog: Boolean,
                isUserGesture: Boolean,
                msg: Message?): Boolean {

            if (msg == null) {
                return false
            }

            val id = addTabInternal(null,
                    source.id,
                    false,
                    isUserGesture, null)

            val tab = getTab(id)
                    ?: // FIXME: why null?
                    return false
            if (tab.tabView == null) {
                throw RuntimeException("webview is null, previous creation failed")
            }
            tab.tabView!!.bindOnNewWindowCreation(msg)
            return true
        }

        override fun onCloseWindow(tabView: TabView?) {
            if (source.tabView === tabView) {
                for (i in sessions.indices) {
                    val tab = sessions[i]
                    if (tab.tabView === tabView) {
                        closeTab(tab.id)
                    }
                }
            }
        }

        override fun onProgressChanged(progress: Int) {
            notifyObservers { onProgressChanged(source, progress) }
        }

        override fun onShowFileChooser(tabView: TabView,
                                       filePathCallback: ValueCallback<Array<Uri>>,
                                       fileChooserParams: WebChromeClient.FileChooserParams): Boolean {
            val consumers = wrapConsumers<FileChooserArgs> {
                onShowFileChooser(it.first, it.second, it.third)
            }
            val arg = Triple(source, filePathCallback, fileChooserParams)
            return Consumable.from(arg).consumeBy(consumers)
        }

        override fun onReceivedTitle(view: TabView, title: String?) {
            notifyObservers { onReceivedTitle(source, title) }
        }

        override fun onReceivedIcon(view: TabView, icon: Bitmap?) {
            source.favicon = icon
            notifyObservers { onReceivedIcon(source, icon) }
        }

        override fun onLongPress(hitTarget: TabView.HitTarget) {
            notifyObservers { onLongPress(source, hitTarget) }
        }

        override fun onEnterFullScreen(callback: TabView.FullscreenCallback, view: View?) {
            notifyObservers { onEnterFullScreen(source, callback, view) }
        }

        override fun onExitFullScreen() {
            notifyObservers { onExitFullScreen(source) }
        }

        override fun onGeolocationPermissionsShowPrompt(
                origin: String,
                callback: GeolocationPermissions.Callback?) {
            notifyObservers { onGeolocationPermissionsShowPrompt(source, origin, callback) }
        }
    }

    /**
     * A class to attach to UI thread for sending message.
     */
    private class Notifier internal constructor(
            private val tabViewProvider: TabViewProvider,
            private val observable: Observable<Observer>
    ) : Handler(Looper.getMainLooper()) {

        val ENUM_KEY = "_key_enum"

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_FOCUS_TAB -> focusTab(msg.obj as Session?, msg.data.getSerializable(ENUM_KEY) as Factor)
                MSG_ADDED_TAB -> addedTab(msg)
                else -> {
                }
            }
        }

        fun notifyTabAdded(session: Session, arguments: Bundle?) {
            val msg = this.obtainMessage(MSG_ADDED_TAB)
            val pojo = NotifierPojo()
            pojo.session = session
            pojo.arguements = arguments
            msg.obj = pojo
            this.sendMessage(msg)
        }

        fun addedTab(msg: Message) {
            val pojo = msg.obj as NotifierPojo
            observable.notifyObservers { onSessionAdded(pojo.session!!, pojo.arguements!!) }
        }

        fun notifyTabFocused(session: Session?, factor: Factor) {
            val msg = this.obtainMessage(MSG_FOCUS_TAB)
            msg.obj = session
            msg.data.putSerializable(ENUM_KEY, factor)
            this.sendMessage(msg)
        }

        private fun focusTab(session: Session?, factor: Factor) {

            if (session != null && session.tabView == null) {
                session.initializeView(this.tabViewProvider)
            }

            observable.notifyObservers { onFocusChanged(session, factor) }
        }

        private class NotifierPojo {
            var session: Session? = null
            var arguements: Bundle? = null
        }
    }

    enum class Factor(val value: Int) {
        FACTOR_UNKNOWN(1),
        FACTOR_TAB_ADDED(2),
        FACTOR_TAB_REMOVED(3),
        FACTOR_TAB_SWITCHED(4),
        FACTOR_NO_FOCUS(5),
        FACTOR_BACK_EXTERNAL(6)
    }

    interface Observer {
        fun onSessionStarted(session: Session) = Unit
        fun onSessionFinished(session: Session, isSecure: Boolean) = Unit
        fun onURLChanged(session: Session, url: String?) = Unit

        /**
         * Subsequent process after WebViewClient.shouldOverrideUrlLoading. TabView implementation will
         * decide whether this function be invoke or not.
         *
         * @param url External url to handle
         * @return true if this Listener already handled the external url
         */
        fun handleExternalUrl(url: String?): Boolean = false

        /**
         * Subsequent process after WebViewClient.onReceivedError.
         *
         * @param url The url that failed to load.
         * @param updateFromError To indicate whether this callback is invoked under error. If page started loading, this value would be true.
         * @return true Return true if the URL was handled, false if we should continue loading the current URL.
         */
        fun updateFailingUrl(session: Session, url: String?, updateFromError: Boolean) = Unit

        /**
         * @see android.webkit.WebChromeClient#onProgressChanged(android.webkit.WebView, int)
         */
        fun onProgressChanged(session: Session, progress: Int) = Unit

        /**
         * @see android.webkit.WebChromeClient#onReceivedTitle(android.webkit.WebView, String)
         */
        fun onReceivedTitle(session: Session, title: String?) = Unit

        /**
         * @see android.webkit.WebChromeClient#onReceivedIcon(android.webkit.WebView, Bitmap)
         */
        fun onReceivedIcon(session: Session, icon: Bitmap?) = Unit

        /**
         * Notify the host application a tab becomes 'focused tab'. It usually happens when adding,
         * removing or switching tabs.
         *
         * @param session The session becomes focused, null means there is no focused tab
         * @param factor the potential factor which cause this focus-change-event
         */
        fun onFocusChanged(session: Session?, factor: Factor) = Unit

        /**
         * Notify the host application there is a tab be added.
         *
         * @param session the session be added
         * @param arguments the same arguments when invoke @see{org.mozilla.focus.tabs.SessionManager#addTab}.
         *                  It might be null if this tab is not created from the method.
         */
        fun onSessionAdded(session: Session, arguments: Bundle?) = Unit

        /**
         * Notify the host application the total tab counts changed.
         *
         * @param count total tabs count
         */
        fun onSessionCountChanged(count: Int) = Unit

        /**
         * Notify the host application that long-press happened on a tab
         *
         * @param session The tab received long press event
         * @param hitTarget
         */
        fun onLongPress(session: Session, hitTarget: TabView.HitTarget?) = Unit

        /**
         * Notify the host application that a tab has entered full screen mode.
         * <p>
         * Some TabView implementations may pass a custom View which contains the web contents in
         * full screen mode.
         *
         * @param session The tab which entered fullscreen.
         * @param callback The callback needs to be invoked to request the page to exit full screen mode.
         * @param fullscreenContent The contentView requested to be displayed in fullscreen
         */
        fun onEnterFullScreen(session: Session,
                              callback: TabView.FullscreenCallback,
                              fullscreenContent: View?) = Unit

        /**
         * Notify the host application that the a tab has exited full screen mode.
         * <p>
         * If a View was passed when the application entered full screen mode then this view must
         * be hidden now.
         *
         * @param session the tab which which existed fullscreen.
         */
        fun onExitFullScreen(session: Session) = Unit

        /**
         * Notify the host application to show a file chooser. Usually for file uploading.
         *
         * @param session The tab which is asking file chooser.
         * @param filePathCallback
         * @param fileChooserParams
         * @see android.webkit.WebChromeClient#onShowFileChooser(android.webkit.WebView, ValueCallback, WebChromeClient.FileChooserParams)
         */
        fun onShowFileChooser(session: Session,
                              filePathCallback: ValueCallback<Array<Uri>>,
                              fileChooserParams: WebChromeClient.FileChooserParams): Boolean = false

        /**
         * @see android.webkit.WebChromeClient#onGeolocationPermissionsShowPrompt(String, GeolocationPermissions.Callback)
         */
        fun onGeolocationPermissionsShowPrompt(session: Session,
                                               origin: String?,
                                               callback: GeolocationPermissions.Callback?) = Unit
    }
}
