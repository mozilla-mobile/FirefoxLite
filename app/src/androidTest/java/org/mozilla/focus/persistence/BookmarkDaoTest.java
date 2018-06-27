package org.mozilla.focus.persistence;

import android.arch.core.executor.testing.InstantTaskExecutorRule;
import android.arch.persistence.room.Room;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.utils.LiveDataTestUtil;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(AndroidJUnit4.class)
public class BookmarkDaoTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    private static final BookmarkModel BOOKMARK = new BookmarkModel("TEST_ID", "Yahoo TW", "https://tw.yahoo.com");
    private static final BookmarkModel BOOKMARK_2 = new BookmarkModel("TEST_ID_2", "Google", "https://www.google.com");
    private static final BookmarkModel BOOKMARK_3 = new BookmarkModel("TEST_ID_3", "GoogleV2", "https://www.google.com");

    private BookmarksDatabase bookmarksDatabase;

    @Before
    public void initDb() {
        bookmarksDatabase = Room.inMemoryDatabaseBuilder(InstrumentationRegistry.getContext(),
                BookmarksDatabase.class)
                // allowing main thread queries, just for testing
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void closeDb() {
        bookmarksDatabase.close();
    }

    @Test
    public void addSingleBookmark_checkBookmarkExistedInDB() throws InterruptedException {
        // When inserting a new bookmark in the data source
        bookmarksDatabase.bookmarkDao().addBookmarks(BOOKMARK);

        // The bookmark can be retrieved
        List<BookmarkModel> dbBookmarks = LiveDataTestUtil.getValue(bookmarksDatabase.bookmarkDao().loadBookmarks());
        assertBookmarkEquals(BOOKMARK, dbBookmarks.get(0));
    }

    @Test
    public void addMultipleBookmarks_checkBookmarksExistedInDB() throws InterruptedException {
        // When inserting a bookmark list in the data source
        bookmarksDatabase.bookmarkDao().addBookmarks(BOOKMARK, BOOKMARK_2);

        // The bookmark list can be retrieved
        List<BookmarkModel> dbBookmarks = LiveDataTestUtil.getValue(bookmarksDatabase.bookmarkDao().loadBookmarks());
        assertEquals(2, dbBookmarks.size());
        assertBookmarkEquals(BOOKMARK, dbBookmarks.get(0));
        assertBookmarkEquals(BOOKMARK_2, dbBookmarks.get(1));
    }

    @Test
    public void addMultipleBookmarks_checkSpecificBookmarkIdExistedInDB() throws InterruptedException {
        // When inserting a bookmark list in the data source
        bookmarksDatabase.bookmarkDao().addBookmarks(BOOKMARK, BOOKMARK_2, BOOKMARK_3);

        // The bookmark with matched id can be retrieved
        BookmarkModel dbBookmark = LiveDataTestUtil.getValue(bookmarksDatabase.bookmarkDao().getBookmarkById(BOOKMARK.getId()));
        assertBookmarkEquals(BOOKMARK, dbBookmark);
    }

    @Test
    public void addMultipleBookmarks_checkSpecificBookmarkUrlsExistedInDB() throws InterruptedException {
        // When inserting a bookmark list in the data source
        bookmarksDatabase.bookmarkDao().addBookmarks(BOOKMARK, BOOKMARK_2, BOOKMARK_3);

        // The bookmark list with matched url can be retrieved
        List<BookmarkModel> dbBookmarks = LiveDataTestUtil.getValue(bookmarksDatabase.bookmarkDao().getBookmarksByUrl(BOOKMARK_2.getUrl()));
        assertEquals(2, dbBookmarks.size());
    }

    @Test
    public void updateBookmark_checkBookmarkChangedInDB() throws InterruptedException {
        // Given that we have a bookmark in the data source
        bookmarksDatabase.bookmarkDao().addBookmarks(BOOKMARK);

        // When we are updating the title of the bookmark
        BookmarkModel updatedBookmark = new BookmarkModel(BOOKMARK.getId(), "new title", BOOKMARK.getUrl());
        bookmarksDatabase.bookmarkDao().addBookmarks(updatedBookmark);

        // The retrieved bookmark has the updated title
        List<BookmarkModel> dbBookmarks = LiveDataTestUtil.getValue(bookmarksDatabase.bookmarkDao().loadBookmarks());
        assertBookmarkEquals(updatedBookmark, dbBookmarks.get(0));
    }

    @Test
    public void addBookmarkThenDelete_checkNoBookmarkExistedInDB() throws InterruptedException {
        // Given that we have a bookmark in the data source
        bookmarksDatabase.bookmarkDao().addBookmarks(BOOKMARK);

        // When we are deleting the bookmark
        bookmarksDatabase.bookmarkDao().deleteBookmark(BOOKMARK);

        // The bookmark is no longer in the data source
        List<BookmarkModel> dbBookmarks = LiveDataTestUtil.getValue(bookmarksDatabase.bookmarkDao().loadBookmarks());
        assertEquals(0, dbBookmarks.size());
    }

    @Test
    public void addBookmarksThenDeleteByUrl_checkBookmarksDeletedInDB() throws InterruptedException {
        // Given that we have a bookmark in the data source
        bookmarksDatabase.bookmarkDao().addBookmarks(BOOKMARK, BOOKMARK_2, BOOKMARK_3);

        // When we are deleting the bookmark
        bookmarksDatabase.bookmarkDao().deleteBookmarksByUrl(BOOKMARK_2.getUrl());

        // Only bookmark with different url existed in the data source
        List<BookmarkModel> dbBookmarks = LiveDataTestUtil.getValue(bookmarksDatabase.bookmarkDao().loadBookmarks());
        assertEquals(1, dbBookmarks.size());
    }

    @Test
    public void addBookmarksThenDeleteAll_checkNoBookmarkExistedInDB() throws InterruptedException {
        // Given that we have a bookmark list in the data source
        bookmarksDatabase.bookmarkDao().addBookmarks(BOOKMARK, BOOKMARK_2);

        // When we are deleting all bookmarks
        bookmarksDatabase.bookmarkDao().deleteAllBookmarks();

        // The bookmark is no longer in the data source
        List<BookmarkModel> dbBookmarks = LiveDataTestUtil.getValue(bookmarksDatabase.bookmarkDao().loadBookmarks());
        assertEquals(0, dbBookmarks.size());
    }

    private void assertBookmarkEquals(BookmarkModel expectedBookmark, BookmarkModel actualBookmark) {
        assertEquals(expectedBookmark.getId(), actualBookmark.getId());
        assertEquals(expectedBookmark.getTitle(), actualBookmark.getTitle());
        assertEquals(expectedBookmark.getUrl(), actualBookmark.getUrl());
    }
}