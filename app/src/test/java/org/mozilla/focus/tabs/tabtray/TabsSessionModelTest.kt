package org.mozilla.focus.tabs.tabtray

import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.anyString
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.mozilla.rocket.tabs.Tab
import org.mozilla.rocket.tabs.TabsSession

class TabsSessionModelTest {

    private lateinit var tabsSessionModel: TabsSessionModel

    @Mock
    private lateinit var tabsSession: TabsSession

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        tabsSessionModel = TabsSessionModel(tabsSession)
    }

    @Test
    fun loadTabs_getTabs() {
        // Prepare
        assumeTabs(listOf(Tab(), Tab(), Tab()))

        // First load
        tabsSessionModel.loadTabs {
            Assert.assertEquals(3, tabsSessionModel.tabs.size)
        }

        // Prepare
        assumeTabs(listOf(Tab(), Tab()))

        // Second load
        tabsSessionModel.loadTabs {
            Assert.assertEquals(2, tabsSessionModel.tabs.size)
        }
    }

    @Test
    fun targetExist_switchTab_tabsSessionSwitchToTab() {
        // Prepare
        assumeTabs(listOf(Tab(), Tab(), Tab()))
        tabsSessionModel.loadTabs(null)

        // Test
        tabsSessionModel.switchTab(2)
        verify<TabsSession>(this.tabsSession).switchToTab(anyString())
    }

    @Test
    fun targetRemoved_switchTab_doNothing() {
        // Prepare
        val tab0 = Tab()
        val tab1 = Tab()
        val tab2 = Tab()

        // Load 3 tabs into cache
        assumeTabs(listOf(tab0, tab1, tab2))
        tabsSessionModel.loadTabs(null)

        // Somehow tab1 was closed and removed from TabsSession
        assumeTabs(listOf(tab0, tab2))

        // Nothing should happen when trying to switch to tab1
        tabsSessionModel.switchTab(1)
        verify<TabsSession>(this.tabsSession, never()).switchToTab(anyString())
    }

    @Test
    fun targetExist_removeTab_tabsSessionDropTab() {
        val target = Tab()
        assumeTabs(listOf(Tab(), target, Tab()))
        tabsSessionModel.loadTabs(null)

        tabsSessionModel.removeTab(1)
        verify<TabsSession>(this.tabsSession).dropTab(target.id)
    }

    private fun assumeTabs(list: List<Tab>) {
        Mockito.`when`(tabsSession.tabs)
                .thenReturn(list)
    }
}
