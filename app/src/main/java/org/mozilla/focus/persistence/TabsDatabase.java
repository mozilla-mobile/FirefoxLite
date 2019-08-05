package org.mozilla.focus.persistence;

import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import android.content.Context;
import androidx.annotation.NonNull;

@Database(entities = {TabEntity.class}, version = 2)
public abstract class TabsDatabase extends RoomDatabase {

    public abstract TabDao tabDao();

    public static TabsDatabase create(@NonNull Context context) {
        return Room.databaseBuilder(context.getApplicationContext(),
                TabsDatabase.class, "tabs.db")
                .addMigrations(MIGRATION_1_2)
                .build();
    }

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // SQLite doesn't support removing a column from a table
            // http://www.sqlite.org/faq.html#q11
            database.execSQL("ALTER TABLE tabs RENAME TO tabs_old");
            database.execSQL("CREATE TABLE IF NOT EXISTS tabs (tab_id TEXT NOT NULL,"
                    + " tab_parent_id TEXT, tab_title TEXT, tab_url TEXT, PRIMARY KEY(tab_id))");
            database.execSQL("INSERT INTO tabs (tab_id, tab_parent_id, tab_title, tab_url)"
                    + " SELECT tab_id, tab_parent_id, tab_title, tab_url FROM tabs_old");
            database.execSQL("DROP TABLE tabs_old");
        }
    };
}
