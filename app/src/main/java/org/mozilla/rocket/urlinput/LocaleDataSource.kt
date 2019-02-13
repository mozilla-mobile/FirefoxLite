package org.mozilla.rocket.urlinput

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context

class LocaleDataSource : SearchPortalDataSource {

    private lateinit var context: Context

    override fun fetchPortals(): LiveData<List<SearchPortal>> {
        val liveData = MutableLiveData<List<SearchPortal>>()
        SearchPortalUtils.loadPortalsByLocale(context, liveData)
        return liveData
    }

    companion object {

        @Volatile private var INSTANCE: LocaleDataSource? = null

        @JvmStatic
        fun getInstance(context: Context): LocaleDataSource? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: LocaleDataSource().also {
                        INSTANCE = it
                        it.context = context
                    }
                }
    }
}
