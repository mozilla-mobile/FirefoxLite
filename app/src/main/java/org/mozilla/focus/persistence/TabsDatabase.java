package org.mozilla.focus.persistence;

import android.arch.persistence.room.Database;
import android.arch.persistence.room.Room;
import android.arch.persistence.room.RoomDatabase;
import android.content.Context;
import android.support.annotation.NonNull;

@Database(entities = {TabModel.class}, version = 1)
public abstract class TabsDatabase extends RoomDatabase {

    private static volatile TabsDatabase instance;

    public abstract TabDao tabDao();

    public static TabsDatabase getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (TabsDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            TabsDatabase.class, "tabs.db")
                            .build();
                }
            }
        }
        return instance;
    }
}
