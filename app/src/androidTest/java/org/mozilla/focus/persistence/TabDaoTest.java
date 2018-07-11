package org.mozilla.focus.persistence;

import android.arch.persistence.db.SupportSQLiteDatabase;
import android.arch.persistence.db.framework.FrameworkSQLiteOpenHelperFactory;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.testing.MigrationTestHelper;
import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mozilla.focus.Inject;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TabDaoTest {

    private static final String TEST_DB_NAME = "migration-test";
    private static final TabEntity TAB = new TabEntity("TEST_ID", "ID_HOME", "Yahoo TW", "https://tw.yahoo.com");
    private static final TabEntity TAB_2 = new TabEntity("TEST_ID_2", TAB.getId(), "Google", "https://www.google.com");

    private TabsDatabase tabsDatabase;

    @Rule
    public MigrationTestHelper testHelper =
            new MigrationTestHelper(
                    InstrumentationRegistry.getInstrumentation(),
                    TabsDatabase.class.getCanonicalName(),
                    new FrameworkSQLiteOpenHelperFactory());

    @Before
    public void initDb() throws Exception {
        tabsDatabase = Inject.getTabsDatabase(null);
        tabsDatabase.tabDao().deleteAllTabs();
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
        List<TabEntity> dbTabs = tabsDatabase.tabDao().getTabs();
        assertTabEquals(TAB, dbTabs.get(0));
    }

    @Test
    public void insertAndGetTabList() {
        // When inserting a tab list in the data source
        tabsDatabase.tabDao().insertTabs(TAB, TAB_2);

        // The tab list can be retrieved
        List<TabEntity> dbTabs = tabsDatabase.tabDao().getTabs();
        assertEquals(2, dbTabs.size());
        assertTabEquals(TAB, dbTabs.get(0));
        assertTabEquals(TAB_2, dbTabs.get(1));
    }

    @Test
    public void updateAndGetTab() {
        // Given that we have a tab in the data source
        tabsDatabase.tabDao().insertTabs(TAB);

        // When we are updating the title of the tab
        TabEntity updatedTab = new TabEntity(TAB.getId(), TAB.getParentId(), "new title", TAB.getUrl());
        tabsDatabase.tabDao().insertTabs(updatedTab);

        // The retrieved tab has the updated title
        List<TabEntity> dbTabs = tabsDatabase.tabDao().getTabs();
        assertTabEquals(updatedTab, dbTabs.get(0));
    }

    @Test
    public void deleteAndGetTab() {
        // Given that we have a tab in the data source
        tabsDatabase.tabDao().insertTabs(TAB);

        // When we are deleting all tabs
        tabsDatabase.tabDao().deleteTab(TAB);

        // The tab is no longer in the data source
        List<TabEntity> dbTabs = tabsDatabase.tabDao().getTabs();
        assertEquals(0, dbTabs.size());
    }

    @Test
    public void deleteAllAndGetEmptyTab() {
        // Given that we have a tab list in the data source
        tabsDatabase.tabDao().insertTabs(TAB, TAB_2);

        // When we are deleting all tabs
        tabsDatabase.tabDao().deleteAllTabs();

        // The tab is no longer in the data source
        List<TabEntity> dbTabs = tabsDatabase.tabDao().getTabs();
        assertEquals(0, dbTabs.size());
    }

    @Test
    public void migrationFrom1To2_containsCorrectData() throws IOException {
        // Create the database in version 1
        SupportSQLiteDatabase db = testHelper.createDatabase(TEST_DB_NAME, 1);
        // Insert some data
        insertTabV1(TAB, db);
        insertTabV1(TAB_2, db);
        db.close();

        // Re-open the database with version 2 and provide MIGRATION_1_2 as the migration process.
        testHelper.runMigrationsAndValidate(TEST_DB_NAME, 2, true, TabsDatabase.MIGRATION_1_2);

        // MigrationTestHelper automatically verifies the schema changes, but not the data validity
        // Validate that the data was migrated properly.
        List<TabEntity> dbTabs = getMigratedRoomDatabase().tabDao().getTabs();
        assertEquals(2, dbTabs.size());
        assertTabEquals(TAB, dbTabs.get(0));
        assertTabEquals(TAB_2, dbTabs.get(1));
    }

    private void insertTabV1(TabEntity tabEntity, SupportSQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put("tab_id", tabEntity.getId());
        values.put("tab_parent_id", tabEntity.getParentId());
        values.put("tab_title", tabEntity.getTitle());
        values.put("tab_url", tabEntity.getUrl());
        values.put("tab_thumbnail_uri", "");
        values.put("webview_state_uri", "");
        db.insert("tabs", SQLiteDatabase.CONFLICT_REPLACE, values);
    }

    private void assertTabEquals(TabEntity expectedTab, TabEntity actualTab) {
        assertEquals(expectedTab.getId(), actualTab.getId());
        assertEquals(expectedTab.getParentId(), actualTab.getParentId());
        assertEquals(expectedTab.getTitle(), actualTab.getTitle());
        assertEquals(expectedTab.getUrl(), actualTab.getUrl());
    }

    private TabsDatabase getMigratedRoomDatabase() {
        TabsDatabase database = Room.databaseBuilder(
                InstrumentationRegistry.getTargetContext(),
                TabsDatabase.class,
                TEST_DB_NAME)
                .fallbackToDestructiveMigration()
                .addMigrations(TabsDatabase.MIGRATION_1_2)
                .build();

        testHelper.closeWhenFinished(database);
        return database;
    }
}