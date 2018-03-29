/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.tabs.tabtray

import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mozilla.focus.tabs.Tab

class TabTrayPresenterTest {

    private lateinit var tabTrayPresenter: TabTrayPresenter

    @Mock
    private val tabTrayContractView: TabTrayContract.View? = null

    @Mock
    private lateinit var tabsSessionModel: TabsSessionModel

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        tabTrayPresenter = TabTrayPresenter(tabTrayContractView, tabsSessionModel)
    }


    @Test
    fun viewReady_showFocusedTab() {
        Mockito.`when`(tabsSessionModel.tabs).thenReturn(listOf())
        this.tabTrayPresenter.viewReady()
        verify<TabTrayContract.View>(this.tabTrayContractView).closeTabTray()

        Mockito.`when`(tabsSessionModel.tabs).thenReturn(listOf(Tab(), Tab(), Tab()))
        this.tabTrayPresenter.viewReady()
        verify<TabTrayContract.View>(this.tabTrayContractView).showFocusedTab(anyInt())
    }
}