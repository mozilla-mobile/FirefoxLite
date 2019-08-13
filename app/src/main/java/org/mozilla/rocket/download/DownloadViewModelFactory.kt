package org.mozilla.rocket.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class DownloadViewModelFactory(
    private val repository: DownloadInfoRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DownloadIndicatorViewModel::class.java)) {
            return DownloadIndicatorViewModel(repository) as T
        } else if (modelClass.isAssignableFrom(DownloadInfoViewModel::class.java)) {
            return DownloadInfoViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}
