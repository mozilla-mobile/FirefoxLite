package org.mozilla.focus.persistence;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import android.content.Context;
import androidx.annotation.NonNull;

@Database(entities = {BookmarkModel.class}, version = 1)
public abstract class BookmarksDatabase extends RoomDatabase {

    private static volatile BookmarksDatabase instance;

    public abstract BookmarkDao bookmarkDao();

    public static BookmarksDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (BookmarksDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            BookmarksDatabase.class, "bookmarks.db")
                            .build();
                }
            }
        }
        return instance;
    }
}
