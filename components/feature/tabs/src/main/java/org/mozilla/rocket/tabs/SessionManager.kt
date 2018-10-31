/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.text.TextUtils
import mozilla.components.browser.session.Session
import mozilla.components.support.base.observer.Observable
import mozilla.components.support.base.observer.ObserverRegistry
import org.mozilla.rocket.tabs.SessionManager.Factor.FACTOR_BACK_EXTERNAL
import org.mozilla.rocket.tabs.SessionManager.Factor.FACTOR_NO_FOCUS
import org.mozilla.rocket.tabs.SessionManager.Factor.FACTOR_TAB_ADDED
import org.mozilla.rocket.tabs.SessionManager.Factor.FACTOR_TAB_REMOVED
import org.mozilla.rocket.tabs.SessionManager.Factor.FACTOR_TAB_SWITCHED
import org.mozilla.rocket.tabs.SessionManager.Observer
import org.mozilla.rocket.tabs.ext.BLANK_URL
import org.mozilla.rocket.tabs.ext.ID_EXTERNAL
import org.mozilla.rocket.tabs.ext.isFromExternal
import org.mozilla.rocket.tabs.ext.engineSessionHolder
import org.mozilla.rocket.tabs.ext.getParentId
import org.mozilla.rocket.tabs.ext.isValid
import org.mozilla.rocket.tabs.ext.setParentId
import org.mozilla.rocket.tabs.utils.TabUtil
import java.lang.ref.WeakReference
import java.util.ArrayList
import java.util.LinkedList

internal val MSG_FOCUS_TAB = 0x1001
internal val MSG_ADDED_TAB = 0x1002
internal val MSG_REMOVEDED_TAB = 0x1003

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
        this.notifier = Notifier(this)
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

            this.sessions.add(insertPos++, session)
        }

        if (this.sessions.size > 0 && this.sessions.size == sessionList.size) {
            focusRef = WeakReference<Session>(getTab(focusTabId))
        }

        notifyObservers { onSessionCountChanged(sessions.size) }
    }

    fun restore(states: List<SessionWithState>, focusTabId: String?) {

        for (state in states) {
            if (state.session.isValid()) {
                getOrCreateEngineSession(state.session).let { link(state.session, it) }
                this.sessions.add(state.session)
            }
        }

        if (this.sessions.size > 0 && this.sessions.size == states.size) {
            focusRef = WeakReference<Session>(getTab(focusTabId))
        }

        notifyObservers { onSessionCountChanged(sessions.size) }
    }

    fun add(session: Session,
            selected: Boolean = false,
            engineSession: TabViewEngineSession? = null,
            parent: Session? = null
    ) {
        engineSession?.let { link(session, it) }
        val parentId = parent?.id
        session.url?.let { addTabInternal(it, parentId, session.isFromExternal(), selected, null) }
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

        // schedule tab.destroy() later, to avoid concurrent-modification of session, which is
        // complained by Observable.notifyObservers
        notifier.notifyTabRemoved(tab)

        // Update child's parent id to its ancestor
        // TODO: in our current design, the parent of a tab are always locate at left(index -1).
        // hence no need to loop whole list.
        for (t in sessions) {
            if (TextUtils.equals(t.getParentId(), tab.id)) {
                t.setParentId(tab.getParentId())
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
        if (TextUtils.isEmpty(removedSession.getParentId())) {
            focusRef.clear()
            notifier.notifyTabFocused(null, FACTOR_NO_FOCUS)
        } else if (TextUtils.equals(removedSession.getParentId(), ID_EXTERNAL)) {
            focusRef.clear()
            notifier.notifyTabFocused(null, FACTOR_BACK_EXTERNAL)
        } else {
            focusRef = WeakReference<Session>(getTab(removedSession.getParentId()!!))
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
     * To destroy this session, and it also destroy any sessions in this session.
     * This method should be called after any View has been removed from view system.
     * No other methods may be called on this session after destroy.
     */
    fun destroy() {
        for (tab in sessions) {
            destroySession(tab)
        }
    }

    /**
     * To pause this session, and it also pause any sessions in this session.
     */
    fun pause() {
        for (session in sessions) {
            session.engineSessionHolder.engineSession?.tabView?.onPause()
        }
    }

    /**
     * To resume this session after a previous call to @see{#pause}
     */
    fun resume() {
        for (session in sessions) {
            session.engineSessionHolder.engineSession?.tabView?.onResume()
        }
    }

    fun getEngineSession(session: Session) = session.engineSessionHolder.engineSession

    fun getOrCreateEngineSession(session: Session): TabViewEngineSession {
        getEngineSession(session)?.let { return it }

        return TabViewEngineSession().apply {
            link(session, this)
        }
    }

    private fun initializeEngineView(session: Session) {
        if (session.engineSessionHolder.engineSession == null) {
            getOrCreateEngineSession(session)
        }

        val tabView = tabViewProvider.create()
        session.engineSessionHolder.engineSession?.tabView = tabView
        if (session.engineSessionHolder.engineSession?.webViewState != null) {
            tabView.restoreViewState(session.engineSessionHolder.engineSession?.webViewState)
        } else if (!TextUtils.equals(session.url, BLANK_URL)) {
            tabView.loadUrl(session.url)
        }
    }

    private fun link(session: Session, engineSession: TabViewEngineSession) {
        unlink(session)

        session.engineSessionHolder.engineObserver = TabViewEngineObserver(session).also { observer ->
            engineSession.register(observer)
        }
        session.engineSessionHolder.engineSession = engineSession.also { it.windowClient = WindowClient(session) }
    }

    private fun unlink(session: Session) {
        session.engineSessionHolder.engineObserver?.let { observer ->
            session.engineSessionHolder.engineSession?.unregister(observer)
        }
        session.engineSessionHolder.engineSession?.destroy()
        session.engineSessionHolder.engineSession = null
        session.engineSessionHolder.engineObserver = null
    }

    private fun destroySession(session: Session) {
        unlink(session)
        session.unregisterObservers()
    }

    private fun addTabInternal(url: String?,
                               parentId: String?,
                               fromExternal: Boolean,
                               toFocus: Boolean,
                               arguments: Bundle?): String {

        val initUrl = url ?: BLANK_URL
        val tab = Session(initialUrl = initUrl)

        tab.setParentId(parentId)

        val parentIndex = if (TextUtils.isEmpty(parentId)) -1 else getTabIndex(parentId!!)
        if (fromExternal) {
            tab.setParentId(ID_EXTERNAL)
            sessions.add(tab)
        } else {
            insertTab(parentIndex, tab)
        }

        notifier.notifyTabAdded(tab, arguments)

        focusRef = if (toFocus || fromExternal) WeakReference(tab) else focusRef

        getOrCreateEngineSession(tab)
        initializeEngineView(tab)

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
            if (parentTab.id == t.getParentId()) {
                t.setParentId(session.id)
            }
        }

        // update family relationship
        session.setParentId(parentTab.id)
    }

    data class SessionWithState(
            val session: Session,
            val engineSession: TabViewEngineSession? = null
    )

    internal inner class WindowClient(var source: Session) : TabViewEngineSession.WindowClient {

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
            tab.engineSessionHolder.engineSession?.tabView
                    ?: throw RuntimeException("webview is null, previous creation failed")
            tab.engineSessionHolder.engineSession!!.tabView!!.bindOnNewWindowCreation(msg)
            return true
        }

        override fun onCloseWindow(es: TabViewEngineSession) {
            if (source.engineSessionHolder.engineSession === es) {
                sessions.firstOrNull { it.engineSessionHolder.engineSession == es }
                        ?.let { session -> closeTab(session.id) }
            }
        }
    }

    /**
     * A class to attach to UI thread for sending message.
     */
    private class Notifier internal constructor(
            private val observable: SessionManager
    ) : Handler(Looper.getMainLooper()) {

        val ENUM_KEY = "_key_enum"

        override fun handleMessage(msg: Message) {
            when (msg.what) {
                MSG_FOCUS_TAB -> focusTab(msg.obj as Session?, msg.data.getSerializable(ENUM_KEY) as Factor)
                MSG_ADDED_TAB -> addedTab(msg)
                MSG_REMOVEDED_TAB -> removedTab(msg)
                else -> {
                }
            }
        }

        fun notifyTabAdded(session: Session, arguments: Bundle?) {
            val msg = this.obtainMessage(MSG_ADDED_TAB)
            msg.obj = Pair(session, arguments)
            this.sendMessage(msg)
        }

        fun notifyTabRemoved(session: Session) {
            val msg = this.obtainMessage(MSG_REMOVEDED_TAB)
            msg.obj = session
            this.sendMessage(msg)
        }

        fun addedTab(msg: Message) {
            val pair = msg.obj as Pair<Session, Bundle?>
            observable.notifyObservers { onSessionAdded(pair.first, pair.second) }
        }

        fun removedTab(msg: Message) {
            val session = msg.obj as Session
            observable.destroySession(session)
        }

        fun notifyTabFocused(session: Session?, factor: Factor) {
            val msg = this.obtainMessage(MSG_FOCUS_TAB)
            msg.obj = session
            msg.data.putSerializable(ENUM_KEY, factor)
            this.sendMessage(msg)
        }

        private fun focusTab(session: Session?, factor: Factor) {
            session?.let {
                if (observable.getOrCreateEngineSession(session).tabView == null) {
                    observable.initializeEngineView(session)
                }
            }

            observable.notifyObservers { onFocusChanged(session, factor) }
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
    }
}
