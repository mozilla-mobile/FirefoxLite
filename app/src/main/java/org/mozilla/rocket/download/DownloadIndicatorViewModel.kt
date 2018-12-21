package org.mozilla.rocket.download

import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData

class DownloadIndicatorViewModel(application: Application, private val repository: DownloadInfoRepository) : AndroidViewModel(application) {

    val downloadIndicator: LiveData<Int>?
        get() = repository.getDownloadIndicator()
}
