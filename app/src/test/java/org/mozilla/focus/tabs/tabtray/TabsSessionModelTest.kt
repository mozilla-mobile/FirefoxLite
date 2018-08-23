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
import org.mozilla.rocket.tabs.Session
import org.mozilla.rocket.tabs.SessionManager

class TabsSessionModelTest {

    private lateinit var tabsSessionModel: TabsSessionModel

    @Mock
    private lateinit var sessionManager: SessionManager

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        tabsSessionModel = TabsSessionModel(sessionManager)
    }

    @Test
    fun loadTabs_getTabs() {
        // Prepare
        assumeTabs(listOf(Session(), Session(), Session()))

        // First load
        tabsSessionModel.loadTabs {
            Assert.assertEquals(3, tabsSessionModel.tabs.size)
        }

        // Prepare
        assumeTabs(listOf(Session(), Session()))

        // Second load
        tabsSessionModel.loadTabs {
            Assert.assertEquals(2, tabsSessionModel.tabs.size)
        }
    }

    @Test
    fun targetExist_switchTab_tabsSessionSwitchToTab() {
        // Prepare
        assumeTabs(listOf(Session(), Session(), Session()))
        tabsSessionModel.loadTabs(null)

        // Test
        tabsSessionModel.switchTab(2)
        verify<SessionManager>(this.sessionManager).switchToTab(anyString())
    }

    @Test
    fun targetRemoved_switchTab_doNothing() {
        // Prepare
        val tab0 = Session()
        val tab1 = Session()
        val tab2 = Session()

        // Load 3 tabs into cache
        assumeTabs(listOf(tab0, tab1, tab2))
        tabsSessionModel.loadTabs(null)

        // Somehow tab1 was closed and removed from SessionManager
        assumeTabs(listOf(tab0, tab2))

        // Nothing should happen when trying to switch to tab1
        tabsSessionModel.switchTab(1)
        verify<SessionManager>(this.sessionManager, never()).switchToTab(anyString())
    }

    @Test
    fun targetExist_removeTab_tabsSessionDropTab() {
        val target = Session()
        assumeTabs(listOf(Session(), target, Session()))
        tabsSessionModel.loadTabs(null)

        tabsSessionModel.removeTab(1)
        verify<SessionManager>(this.sessionManager).dropTab(target.id)
    }

    private fun assumeTabs(list: List<Session>) {
        Mockito.`when`(sessionManager.getTabs())
                .thenReturn(list)
    }
}
