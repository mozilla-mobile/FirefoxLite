package org.mozilla.focus.persistence;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Transaction;

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
