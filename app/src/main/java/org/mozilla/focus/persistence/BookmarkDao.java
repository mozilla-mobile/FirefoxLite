package org.mozilla.focus.persistence;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface BookmarkDao {

    @Query("SELECT * FROM bookmarks")
    List<BookmarkModel> loadBookmarks();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addBookmarks(BookmarkModel... bookmark);

    @Delete
    void deleteBookmark(BookmarkModel bookmark);

    @Query("DELETE FROM bookmarks")
    void deleteAllBookmarks();
}
