package org.mozilla.focus.persistence;

import android.arch.persistence.room.Room;
import android.support.test.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TabDaoTest {

    private static final TabModel TAB = new TabModel("TEST_ID", "ID_HOME", "Yahoo TW", "https://tw.yahoo.com");
    private static final TabModel TAB_2 = new TabModel("TEST_ID_2", TAB.getId(), "Google", "https://www.google.com");

    private TabsDatabase tabsDatabase;

    @Before
    public void initDb() throws Exception {
        // using an in-memory database because the information stored here disappears when the
        // process is killed
        tabsDatabase = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
                TabsDatabase.class).build();
    }

    @After
    public void closeDb() throws Exception {
        tabsDatabase.close();
    }

    @Test
    public void insertAndGetTab() {
        // When inserting a new tab in the data source
        tabsDatabase.tabDao().insertTabs(TAB);

        // The tab can be retrieved
        List<TabModel> dbTabs = tabsDatabase.tabDao().getTabs();
        assertTabEquals(TAB, dbTabs.get(0));
    }

    @Test
    public void insertAndGetTabList() {
        // When inserting a tab list in the data source
        tabsDatabase.tabDao().insertTabs(TAB, TAB_2);

        // The tab list can be retrieved
        List<TabModel> dbTabs = tabsDatabase.tabDao().getTabs();
        assertEquals(2, dbTabs.size());
        assertTabEquals(TAB, dbTabs.get(0));
        assertTabEquals(TAB_2, dbTabs.get(1));
    }

    @Test
    public void updateAndGetTab() {
        // Given that we have a tab in the data source
        tabsDatabase.tabDao().insertTabs(TAB);

        // When we are updating the title of the tab
        TabModel updatedTab = new TabModel(TAB.getId(), TAB.getParentId(), "new title", TAB.getUrl(), TAB.getThumbnailUri(), TAB.getWebViewStateUri());
        tabsDatabase.tabDao().insertTabs(updatedTab);

        // The retrieved tab has the updated title
        List<TabModel> dbTabs = tabsDatabase.tabDao().getTabs();
        assertTabEquals(updatedTab, dbTabs.get(0));
    }

    @Test
    public void deleteAndGetTab() {
        // Given that we have a tab in the data source
        tabsDatabase.tabDao().insertTabs(TAB);

        // When we are deleting all tabs
        tabsDatabase.tabDao().deleteTab(TAB);

        // The tab is no longer in the data source
        List<TabModel> dbTabs = tabsDatabase.tabDao().getTabs();
        assertEquals(0, dbTabs.size());
    }

    @Test
    public void deleteAllAndGetEmptyTab() {
        // Given that we have a tab list in the data source
        tabsDatabase.tabDao().insertTabs(TAB, TAB_2);

        // When we are deleting all tabs
        tabsDatabase.tabDao().deleteAllTabs();

        // The tab is no longer in the data source
        List<TabModel> dbTabs = tabsDatabase.tabDao().getTabs();
        assertEquals(0, dbTabs.size());
    }

    private void assertTabEquals(TabModel expectedTab, TabModel actualTab) {
        assertEquals(expectedTab.getId(), actualTab.getId());
        assertEquals(expectedTab.getParentId(), actualTab.getParentId());
        assertEquals(expectedTab.getTitle(), actualTab.getTitle());
        assertEquals(expectedTab.getUrl(), actualTab.getUrl());
        assertEquals(expectedTab.getThumbnailUri(), actualTab.getThumbnailUri());
        assertEquals(expectedTab.getWebViewStateUri(), actualTab.getWebViewStateUri());
    }
}