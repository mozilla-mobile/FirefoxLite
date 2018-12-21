package org.mozilla.rocket.download

import android.app.Application
import android.arch.lifecycle.LiveData

class DownloadInfoRepository {

    var downloadIndicator: DownloadIndicatorLiveData? = null

    fun getDownloadIndicator(): LiveData<Int>? {
        return downloadIndicator
    }

    companion object {

        @Volatile private var INSTANCE: DownloadInfoRepository? = null

        @JvmStatic
        fun getInstance(application: Application): DownloadInfoRepository? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: DownloadInfoRepository().also {
                        INSTANCE = it
                        INSTANCE?.downloadIndicator = DownloadIndicatorLiveData(application.applicationContext)
                    }
                }
    }
}
