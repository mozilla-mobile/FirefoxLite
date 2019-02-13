package org.mozilla.rocket.urlinput

import android.arch.lifecycle.LiveData

class SearchPortalRepository(private val globalDataSource: SearchPortalDataSource, private val localeDataSource: SearchPortalDataSource)/*: SearchPortalDataSource */ {

    fun fetchGlobal(): LiveData<List<SearchPortal>> {
        return globalDataSource.fetchPortals()
    }

    fun fetchLocale(): LiveData<List<SearchPortal>> {
        return localeDataSource.fetchPortals()
    }

    companion object {

        @Volatile private var INSTANCE: SearchPortalRepository? = null

        @JvmStatic
        fun getInstance(globalDataSource: SearchPortalDataSource, localeDataSource: SearchPortalDataSource): SearchPortalRepository? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: SearchPortalRepository(globalDataSource, localeDataSource).also {
                        INSTANCE = it
                    }
                }
    }
}