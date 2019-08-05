package org.mozilla.focus.persistence.di

import android.content.Context
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
    fun provideTabsDatabase(appContext: Context): TabsDatabase = TabsDatabase.create(appContext)

    @JvmStatic
    @Singleton
    @Provides
    fun provideTabModelStore(tabsDatabase: TabsDatabase): TabModelStore = TabModelStore(tabsDatabase)
}