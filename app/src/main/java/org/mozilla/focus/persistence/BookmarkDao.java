package org.mozilla.focus.persistence;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface BookmarkDao {

    @Query("SELECT * FROM bookmarks")
    LiveData<List<BookmarkModel>> loadBookmarks();

    @Query("SELECT * FROM bookmarks WHERE id = :id")
    LiveData<BookmarkModel> getBookmarkById(String id);

    @Query("SELECT * FROM bookmarks WHERE url = :url")
    LiveData<List<BookmarkModel>> getBookmarksByUrl(String url);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void addBookmarks(BookmarkModel... bookmark);

    @Delete
    void deleteBookmark(BookmarkModel bookmark);

    @Query("DELETE FROM bookmarks WHERE url = :url")
    void deleteBookmarksByUrl(String url);

    @Query("DELETE FROM bookmarks")
    void deleteAllBookmarks();
}
