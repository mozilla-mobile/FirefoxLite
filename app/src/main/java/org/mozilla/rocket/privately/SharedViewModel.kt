package org.mozilla.rocket.privately

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

class SharedViewModel : ViewModel() {

    private val url: MutableLiveData<String> = MutableLiveData()

    fun setUrl(newUrl: String) {
        url.value = newUrl
    }

    fun getUrl(): LiveData<String> {
        return url
    }
}