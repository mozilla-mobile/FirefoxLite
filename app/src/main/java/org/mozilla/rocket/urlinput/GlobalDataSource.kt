package org.mozilla.rocket.urlinput

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.content.Context

class GlobalDataSource : SearchPortalDataSource {

    private lateinit var context: Context

    override fun fetchPortals(): LiveData<List<SearchPortal>> {
        val liveData = MutableLiveData<List<SearchPortal>>()
        SearchPortalUtils.loadDefaultPortals(context, liveData)
        return liveData
    }

    companion object {

        @Volatile private var INSTANCE: GlobalDataSource? = null

        @JvmStatic
        fun getInstance(context: Context): GlobalDataSource? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: GlobalDataSource().also {
                        INSTANCE = it
                        it.context = context
                    }
                }
    }
}
