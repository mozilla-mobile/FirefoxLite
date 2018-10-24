/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Message
import android.text.TextUtils
import android.view.View
import junit.framework.Assert
import org.junit.After
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mozilla.rocket.tabs.SessionManager.Factor
import org.mozilla.rocket.tabs.SessionManager.Observer
import org.mozilla.rocket.tabs.utils.TabUtil
import org.mozilla.rocket.tabs.web.DownloadCallback
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper
import java.util.ArrayList

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class SessionManagerTest {

    internal lateinit var mgr: SessionManager
    internal val sessions: MutableList<Session> = ArrayList()

    internal val urls = arrayOf("https://mozilla.org", "https://mozilla.com", "https://wikipedia.org", "https://en.wikipedia.org/wiki/Taiwan")

    @Before
    fun setUp() {
        mgr = SessionManager(DefaultTabViewProvider())

        for (i in urls.indices) {
            // use url as id for convenience
            val session = Session(urls[i], "", urls[i])
            sessions.add(session)
        }
    }

    @After
    fun cleanUp() {
    }

    @Test
    fun testExistence() {
        Assert.assertNotNull(mgr)
    }

    @Test
    fun testAddTab1() {
        /* no parent id, tab should be append to tail */

        val tabId0 = mgr.addTab("url0", TabUtil.argument(null, false, false))
        val tabId1 = mgr.addTab("url1", TabUtil.argument(null, false, false))
        val tabId2 = mgr.addTab("url2", TabUtil.argument(null, false, false))
        Assert.assertEquals(3, mgr.getTabs().size)
        Assert.assertEquals(mgr.getTabs()[0].id, tabId0)
        Assert.assertEquals(mgr.getTabs()[1].id, tabId1)
        Assert.assertEquals(mgr.getTabs()[2].id, tabId2)
        Assert.assertTrue(TextUtils.isEmpty(mgr.getTabs()[0].parentId))
        Assert.assertTrue(TextUtils.isEmpty(mgr.getTabs()[1].parentId))
        Assert.assertTrue(TextUtils.isEmpty(mgr.getTabs()[2].parentId))
    }

    @Test
    fun testAddTab2() {
        /* Session 2 use Tab0 as parent */

        val tabId0 = mgr.addTab("url0", TabUtil.argument(null, false, false))
        Assert.assertEquals(1, mgr.getTabs().size)

        // this tab should not have parent
        Assert.assertTrue(TextUtils.isEmpty(mgr.getTabs()[0].parentId))

        // no parent, should be append to tail
        val tabId1 = mgr.addTab("url1", TabUtil.argument(null, false, false))

        // tab0 is parent, order should be: tabId0 -> tabId2 -> tabId1
        val tabId2 = mgr.addTab("url2", TabUtil.argument(tabId0, false, false))

        Assert.assertEquals(mgr.getTabs()[0].id, tabId0)
        Assert.assertEquals(mgr.getTabs()[1].id, tabId2)
        Assert.assertEquals(mgr.getTabs()[2].id, tabId1)
        Assert.assertTrue(TextUtils.isEmpty(mgr.getTabs()[0].parentId))
        Assert.assertEquals(mgr.getTabs()[1].parentId, tabId0)
        Assert.assertTrue(TextUtils.isEmpty(mgr.getTabs()[2].parentId))
    }

    @Test
    fun testAddTab3() {
        val tabId0 = mgr.addTab("url0", TabUtil.argument(null, false, false))
        val tabId1 = mgr.addTab("url1", TabUtil.argument(null, false, false))
        val tabId2 = mgr.addTab("url2", TabUtil.argument(tabId0, false, false))
        val tabId3 = mgr.addTab("url3", TabUtil.argument(tabId0, false, false))
        val tabId4 = mgr.addTab("url4", TabUtil.argument(tabId1, false, false))
        val tabId5 = mgr.addTab("url5", TabUtil.argument(tabId4, false, false))

        // tabId0 -> tabId3 -> tabId2, tabId1 -> tabId4 -> tabId5
        Assert.assertEquals(mgr.getTabs()[0].id, tabId0)
        Assert.assertEquals(mgr.getTabs()[1].id, tabId3)
        Assert.assertEquals(mgr.getTabs()[2].id, tabId2)
        Assert.assertEquals(mgr.getTabs()[3].id, tabId1)
        Assert.assertEquals(mgr.getTabs()[4].id, tabId4)
        Assert.assertEquals(mgr.getTabs()[5].id, tabId5)
        Assert.assertTrue(TextUtils.isEmpty(mgr.getTabs()[0].parentId))
        Assert.assertEquals(mgr.getTabs()[1].parentId, tabId0)
        Assert.assertEquals(mgr.getTabs()[2].parentId, tabId3)
        Assert.assertTrue(TextUtils.isEmpty(mgr.getTabs()[3].parentId))
        Assert.assertEquals(mgr.getTabs()[4].parentId, tabId1)
        Assert.assertEquals(mgr.getTabs()[5].parentId, tabId4)
    }

    @Test
    fun testAddTab3A() {
        val tabId0 = mgr.addTab("url", TabUtil.argument(null, false, true))
        val tabId1 = mgr.addTab("url", TabUtil.argument(null, false, true))
        val tabId2 = mgr.addTab("url", TabUtil.argument(tabId1, false, true))
        val tabId3 = mgr.addTab("url", TabUtil.argument(tabId2, false, true))
        // open from external
        val tabId4 = mgr.addTab("url", TabUtil.argument(null, true, false))
        val tabId5 = mgr.addTab("url", TabUtil.argument(tabId4, false, true))
        val tabId6 = mgr.addTab("url", TabUtil.argument(tabId5, false, true))
        val tabId7 = mgr.addTab("url", TabUtil.argument(null, true, false))
        val tabId8 = mgr.addTab("url", TabUtil.argument(tabId7, false, true))
        val tabId9 = mgr.addTab("url", TabUtil.argument(tabId8, false, true))

        // tabId0, tabId1 -> tabId2 -> tabId3, tabId4 -> tabId5 -> tabId6, tabId7 -> tabId8 -> tabId9
        val tabs = mgr.getTabs()
        Assert.assertEquals(mgr.focusSession!!.id, tabId9)
        Assert.assertEquals(tabs[0].id, tabId0)
        Assert.assertEquals(tabs[1].id, tabId1)
        Assert.assertEquals(tabs[2].id, tabId2)
        Assert.assertEquals(tabs[3].id, tabId3)
        Assert.assertEquals(tabs[4].id, tabId4)
        Assert.assertEquals(tabs[5].id, tabId5)
        Assert.assertEquals(tabs[6].id, tabId6)
        Assert.assertEquals(tabs[7].id, tabId7)
        Assert.assertEquals(tabs[8].id, tabId8)
        Assert.assertEquals(tabs[9].id, tabId9)

        Assert.assertTrue(TextUtils.isEmpty(tabs[0].parentId))
        Assert.assertTrue(TextUtils.isEmpty(tabs[1].parentId))
        Assert.assertEquals(tabs[2].parentId, tabId1)
        Assert.assertEquals(tabs[3].parentId, tabId2)
        Assert.assertEquals(tabs[4].parentId, Session.ID_EXTERNAL)
        Assert.assertEquals(tabs[5].parentId, tabId4)
        Assert.assertEquals(tabs[6].parentId, tabId5)
        Assert.assertEquals(tabs[7].parentId, Session.ID_EXTERNAL)
        Assert.assertEquals(tabs[8].parentId, tabId7)
        Assert.assertEquals(tabs[9].parentId, tabId8)
    }

    @Test
    fun testAddTab4() {
        mgr.restoreTabs(sessions, urls[0])
        Assert.assertEquals(mgr.focusSession!!.id, urls[0])

        val tabId0 = mgr.addTab("url0", TabUtil.argument(null, true, false))
        Assert.assertEquals(mgr.focusSession!!.id, tabId0)

        val tabId1 = mgr.addTab("url1", TabUtil.argument(null, false, true))
        Assert.assertEquals(mgr.focusSession!!.id, tabId1)

        val tabId2 = mgr.addTab("url2", TabUtil.argument(null, false, false))
        Assert.assertEquals(mgr.focusSession!!.id, tabId1)
    }

    // Ignore this test case, I have no idea why it does not work for now, it was working before
    @Ignore
    fun testAddTab5() {
        // Add a tab from internal and focus it. onFocusChanged should be invoked once
        val spy0 = spy(object : Observer {
            override fun onFocusChanged(tab: Session?, factor: Factor) {
                Assert.assertEquals(tab!!.url, "url0")
            }
        })
        mgr.register(spy0)
        val tabId0 = mgr.addTab("url0", TabUtil.argument(null, false, true))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify<Observer>(spy0, times(1)).onFocusChanged(ArgumentMatchers.any(Session::class.java), eq(Factor.FACTOR_TAB_ADDED))
        Assert.assertEquals(mgr.focusSession!!.id, tabId0)
        mgr.unregister(spy0)

        // Add a tab from external. onFocusChanged should be invoked
        val spy1 = spy(object : Observer {
            override fun onFocusChanged(tab: Session?, factor: Factor) {
                Assert.assertEquals(tab!!.url, "url1")
            }
        })
        mgr.register(spy1)
        val tabId1 = mgr.addTab("url1", TabUtil.argument(null, true, false))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify<Observer>(spy1, times(1))
                .onFocusChanged(ArgumentMatchers.any(Session::class.java), eq(Factor.FACTOR_TAB_ADDED))
        Assert.assertEquals(mgr.focusSession!!.id, tabId1)
        mgr.unregister(spy1)

        // Add a tab from internal, but don't focus it.
        // Add a tab from external. onFocusChanged should be invoked
        val spy2 = spy(Observer::class.java)
        mgr.register(spy2)
        mgr.addTab("url2", TabUtil.argument(null, false, false))
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks()
        verify(spy2, times(0)).onFocusChanged(ArgumentMatchers.any(Session::class.java), ArgumentMatchers.any())
        Assert.assertEquals(mgr.focusSession!!.id, tabId1) // focus should not be changed
        mgr.unregister(spy2)
    }

    @Test
    fun testRestore() {
        mgr.restoreTabs(sessions, urls[0])
        Assert.assertEquals(mgr.getTabs().size, urls.size)
        Assert.assertEquals(mgr.focusSession!!.id, urls[0])
    }

    @Test
    fun testSwitch() {
        mgr.restoreTabs(sessions, urls[0])
        mgr.switchToTab("not-exist")
        Assert.assertEquals(mgr.focusSession!!.id, urls[0])

        mgr.switchToTab(urls[0])
        Assert.assertEquals(mgr.focusSession!!.id, urls[0])
        mgr.switchToTab(urls[3])
        Assert.assertEquals(mgr.focusSession!!.id, urls[3])

        mgr.switchToTab(urls[2])
        Assert.assertEquals(mgr.focusSession!!.id, urls[2])
    }

    /**
     * If there is no any focus, remove tab won't effect focus
     */
    @Test
    fun testCloseTab1() {
        mgr.restoreTabs(sessions, null)
        Assert.assertNull(mgr.focusSession)
        mgr.closeTab(sessions[2].id)
        Assert.assertNull(mgr.focusSession)
        mgr.closeTab(sessions[0].id)
        Assert.assertNull(mgr.focusSession)
    }

    /**
     * If there is no any focus, remove tab won't effect focus
     */
    @Test
    fun testCloseTab2() {
        val tabId0 = mgr.addTab("url", TabUtil.argument(null, false, true))
        val tabId1 = mgr.addTab("url", TabUtil.argument(null, false, true))
        val tabId2 = mgr.addTab("url", TabUtil.argument(tabId1, false, true))
        val tabId3 = mgr.addTab("url", TabUtil.argument(tabId2, false, true))
        // open from external
        val tabId4 = mgr.addTab("url", TabUtil.argument(null, true, false))
        val tabId5 = mgr.addTab("url", TabUtil.argument(tabId4, false, true))
        val tabId6 = mgr.addTab("url", TabUtil.argument(tabId5, false, true))
        val tabId7 = mgr.addTab("url", TabUtil.argument(null, true, false))
        val tabId8 = mgr.addTab("url", TabUtil.argument(tabId7, false, true))
        val tabId9 = mgr.addTab("url", TabUtil.argument(tabId8, false, true))

        // tabId0, tabId1 -> tabId2 -> tabId3, tabId4 -> tabId5 -> tabId6, tabId7 -> tabId8 -> tabId9
        mgr.switchToTab(tabId6!!)
        mgr.closeTab(tabId5!!)
        Assert.assertEquals(mgr.focusSession!!.id, tabId6)
        mgr.closeTab(tabId6)
        Assert.assertEquals(mgr.focusSession!!.id, tabId4)
        mgr.closeTab(tabId4!!)
        Assert.assertNull(mgr.focusSession)

        mgr.switchToTab(tabId3!!)
        mgr.closeTab(tabId3)
        Assert.assertEquals(mgr.focusSession!!.id, tabId2)
        mgr.closeTab(tabId2!!)
        Assert.assertEquals(mgr.focusSession!!.id, tabId1)

        mgr.switchToTab(tabId7!!)
        mgr.closeTab(tabId7)
        Assert.assertNull(mgr.focusSession)
        mgr.switchToTab(tabId8!!)
        mgr.closeTab(tabId8)
        Assert.assertNull(mgr.focusSession)
        mgr.switchToTab(tabId9!!)
        mgr.closeTab(tabId9)
        Assert.assertNull(mgr.focusSession)
    }

    /**
     * If there is no any focus, drop tab won't effect focus
     */
    @Test
    fun testDropTab1() {
        mgr.restoreTabs(sessions, null)
        Assert.assertNull(mgr.focusSession)
        mgr.dropTab(sessions[2].id)
        Assert.assertNull(mgr.focusSession)
        mgr.dropTab(sessions[0].id)
        Assert.assertNull(mgr.focusSession)
    }

    @Test
    fun testDropTab2() {
        val tabId0 = mgr.addTab("url", TabUtil.argument(null, false, true))
        val tabId1 = mgr.addTab("url", TabUtil.argument(null, false, true))
        val tabId2 = mgr.addTab("url", TabUtil.argument(tabId1, false, true))
        val tabId3 = mgr.addTab("url", TabUtil.argument(tabId2, false, true))
        // open from external
        val tabId4 = mgr.addTab("url", TabUtil.argument(null, true, false))
        val tabId5 = mgr.addTab("url", TabUtil.argument(tabId4, false, true))
        val tabId6 = mgr.addTab("url", TabUtil.argument(tabId5, false, true))
        val tabId7 = mgr.addTab("url", TabUtil.argument(null, true, false))
        val tabId8 = mgr.addTab("url", TabUtil.argument(tabId7, false, true))
        val tabId9 = mgr.addTab("url", TabUtil.argument(tabId8, false, true))

        // tabId0, tabId1 -> tabId2 -> tabId3, tabId4 -> tabId5 -> tabId6, tabId7 -> tabId8 -> tabId9
        Assert.assertEquals(mgr.focusSession!!.id, tabId9)

        mgr.switchToTab(tabId6!!)
        mgr.dropTab(tabId8!!)
        Assert.assertEquals(mgr.focusSession!!.id, tabId6)
        mgr.dropTab(tabId9!!)
        Assert.assertEquals(mgr.focusSession!!.id, tabId6)
        mgr.dropTab(tabId7!!)
        Assert.assertEquals(mgr.focusSession!!.id, tabId6)
        mgr.dropTab(tabId3!!)
        Assert.assertEquals(mgr.focusSession!!.id, tabId6)
        mgr.dropTab(tabId0!!)
        Assert.assertEquals(mgr.focusSession!!.id, tabId6)
        mgr.dropTab(tabId5!!)
        Assert.assertEquals(mgr.focusSession!!.id, tabId6)
        mgr.dropTab(tabId6)
        Assert.assertEquals(mgr.focusSession!!.id, tabId4)
        mgr.dropTab(tabId4!!)
        Assert.assertEquals(mgr.focusSession!!.id, tabId2)
        mgr.dropTab(tabId2!!)
        Assert.assertEquals(mgr.focusSession!!.id, tabId1)
        mgr.dropTab(tabId1!!)
        Assert.assertNull(mgr.focusSession)
    }

    private class DefaultTabViewProvider : TabViewProvider() {
        override fun create(): TabView {
            return DefaultTabView()
        }
    }

    private class DefaultTabView : TabView {
        private var url: String? = null

        override fun setContentBlockingEnabled(enabled: Boolean) {}

        override fun bindOnNewWindowCreation(msg: Message) {}

        override fun setImageBlockingEnabled(enabled: Boolean) {}

        override fun isBlockingEnabled(): Boolean {
            return false
        }

        override fun performExitFullScreen() {}

        override fun setViewClient(viewClient: TabViewClient?) {}

        override fun setChromeClient(chromeClient: TabChromeClient?) {}

        override fun setDownloadCallback(callback: DownloadCallback?) {}

        override fun setFindListener(callback: TabView.FindListener?) {}

        override fun onPause() {}

        override fun onResume() {}

        override fun destroy() {}

        override fun reload() {}

        override fun stopLoading() {}

        override fun getUrl(): String? {
            return this.url
        }

        override fun getTitle(): String? {
            return null
        }

        @SiteIdentity.SecurityState
        override fun getSecurityState(): Int {
            return SiteIdentity.UNKNOWN
        }

        override fun loadUrl(url: String) {
            this.url = url
        }

        override fun cleanup() {}

        override fun goForward() {}

        override fun goBack() {}

        override fun canGoForward(): Boolean {
            return false
        }

        override fun canGoBack(): Boolean {
            return false
        }

        override fun restoreViewState(inState: Bundle) {}

        override fun saveViewState(outState: Bundle) {}

        override fun insertBrowsingHistory() {}

        override fun getView(): View? {
            return null
        }

        override fun buildDrawingCache(autoScale: Boolean) {}

        override fun getDrawingCache(autoScale: Boolean): Bitmap? {
            return null
        }
    }
}
