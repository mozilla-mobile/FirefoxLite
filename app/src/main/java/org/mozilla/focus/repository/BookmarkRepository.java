package org.mozilla.focus.repository;

import android.arch.lifecycle.LiveData;

import org.mozilla.focus.persistence.BookmarkModel;
import org.mozilla.focus.persistence.BookmarksDatabase;
import org.mozilla.focus.utils.ThreadUtils;

import java.util.List;
import java.util.UUID;

public class BookmarkRepository {
    private static volatile BookmarkRepository instance;

    private BookmarksDatabase bookmarksDatabase;

    private BookmarkRepository(final BookmarksDatabase database) {
        bookmarksDatabase = database;
    }

    public static BookmarkRepository getInstance(final BookmarksDatabase database) {
        if (instance == null) {
            synchronized (BookmarkRepository.class) {
                if (instance == null) {
                    instance = new BookmarkRepository(database);
                }
            }
        }
        return instance;
    }

    public LiveData<List<BookmarkModel>> loadBookmarks() {
        return bookmarksDatabase.bookmarkDao().loadBookmarks();
    }

    public LiveData<BookmarkModel> getBookmarkById(String id) {
        return bookmarksDatabase.bookmarkDao().getBookmarkById(id);
    }

    public LiveData<List<BookmarkModel>> getBookmarksByUrl(String url) {
        return bookmarksDatabase.bookmarkDao().getBookmarksByUrl(url);
    }

    public String addBookmark(String title, String url) {
        final BookmarkModel bookmark = new BookmarkModel(UUID.randomUUID().toString(), title, url);
        ThreadUtils.postToBackgroundThread(() -> bookmarksDatabase.bookmarkDao().addBookmarks(bookmark));

        return bookmark.getId();
    }

    public void updateBookmark(BookmarkModel bookmark) {
        ThreadUtils.postToBackgroundThread(() -> bookmarksDatabase.bookmarkDao().updateBookmark(bookmark));
    }

    public void deleteBookmark(BookmarkModel bookmark) {
        ThreadUtils.postToBackgroundThread(() -> bookmarksDatabase.bookmarkDao().deleteBookmark(bookmark));
    }

    public void deleteBookmarksByUrl(String url) {
        ThreadUtils.postToBackgroundThread(() -> bookmarksDatabase.bookmarkDao().deleteBookmarksByUrl(url));
    }
}
