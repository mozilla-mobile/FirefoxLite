package org.mozilla.rocket.download

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import org.mozilla.focus.Inject

class DownloadViewModelFactory private constructor(private val repository: DownloadInfoRepository) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloadIndicatorViewModel::class.java)) {
            return DownloadIndicatorViewModel(repository) as T
        } else if (modelClass.isAssignableFrom(DownloadInfoViewModel::class.java)) {
            return DownloadInfoViewModel(repository, MutableLiveData()) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {

        @Volatile private var INSTANCE: DownloadViewModelFactory? = null

        @JvmStatic
        fun getInstance(): DownloadViewModelFactory? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: DownloadViewModelFactory(Inject.provideDownloadInfoRepository()).also { INSTANCE = it }
                }
    }
}
