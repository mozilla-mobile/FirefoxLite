package org.mozilla.focus.persistence;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

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

    @Update
    void updateBookmark(BookmarkModel bookmark);

    @Delete
    void deleteBookmark(BookmarkModel bookmark);

    @Query("DELETE FROM bookmarks WHERE url = :url")
    void deleteBookmarksByUrl(String url);

    @Query("DELETE FROM bookmarks")
    void deleteAllBookmarks();
}
