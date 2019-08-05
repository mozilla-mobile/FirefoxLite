package org.mozilla.focus.persistence.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import org.mozilla.focus.persistence.TabModelStore
import org.mozilla.focus.persistence.TabsDatabase
import javax.inject.Singleton

@Module
object TabsModule {

    @JvmStatic
    @Singleton
    @Provides
    fun provideTabsDatabase(appContext: Context): TabsDatabase =
            // using an in-memory database because the information stored here disappears
            // when the process is killed
            Room.inMemoryDatabaseBuilder(appContext, TabsDatabase::class.java).build()

    @JvmStatic
    @Singleton
    @Provides
    fun provideTabModelStore(tabsDatabase: TabsDatabase): TabModelStore = TabModelStore(tabsDatabase)
}