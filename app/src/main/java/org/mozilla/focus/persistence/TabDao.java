package org.mozilla.focus.persistence;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public abstract class TabDao {

    @Query("SELECT * FROM tabs")
    public abstract List<TabEntity> getTabs();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public abstract void insertTabs(TabEntity... tab);

    @Delete
    public abstract void deleteTab(TabEntity tab);

    @Query("DELETE FROM tabs")
    public abstract void deleteAllTabs();

    @Transaction
    public void deleteAllTabsAndInsertTabsInTransaction(TabEntity... tab) {
        deleteAllTabs();
        insertTabs(tab);
    }
}
