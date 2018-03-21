/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray

import android.app.Activity
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.tabs.TabView
import org.mozilla.focus.tabs.TabViewProvider
import org.mozilla.focus.tabs.TabsSession
import org.mozilla.focus.web.WebViewProvider

class TabTrayPresenterTest {

    private lateinit var tabTrayPresenter: TabTrayPresenter

    @Mock
    private val mainActivity: MainActivity? = null

    @Mock
    private val tabTrayContractView: TabTrayContract.View? = null


    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)

        val tabsSession = TabsSession(TestTabViewProvider(mainActivity!!))
        val tabsSessionModel = TabsSessionModel(tabsSession)

        tabTrayPresenter = TabTrayPresenter(tabTrayContractView, tabsSessionModel)

    }


    @Test
    fun viewReady_showFocusedTab() {
        this.tabTrayPresenter.viewReady()

        verify<TabTrayContract.View>(this.tabTrayContractView).showFocusedTab(anyInt())
    }

    class TestTabViewProvider(activity: Activity) : TabViewProvider {
        private var activity: Activity = activity

        override fun create(): TabView {
            return WebViewProvider.create(this.activity, null) as TabView
        }

    }
}