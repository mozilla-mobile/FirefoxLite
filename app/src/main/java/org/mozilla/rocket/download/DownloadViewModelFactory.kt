package org.mozilla.rocket.download

import android.app.Application
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import org.mozilla.focus.Inject

class DownloadViewModelFactory private constructor(private val application: Application, private val repository: DownloadInfoRepository) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloadIndicatorViewModel::class.java)) {
            return DownloadIndicatorViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {

        @Volatile private var INSTANCE: DownloadViewModelFactory? = null

        @JvmStatic
        fun getInstance(application: Application): DownloadViewModelFactory? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: DownloadViewModelFactory(application,
                            Inject.provideDownloadInfoRepository(application)).also { INSTANCE = it }
                }
    }
}
