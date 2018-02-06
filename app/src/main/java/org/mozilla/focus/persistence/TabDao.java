package org.mozilla.focus.persistence;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface TabDao {

    @Query("SELECT * FROM tabs")
    List<TabModel> getTabs();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTab(TabModel tab);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertTabs(List<TabModel> tabs);

    @Delete
    void deleteTab(TabModel tab);

    @Query("DELETE FROM tabs")
    void deleteAllTabs();
}
