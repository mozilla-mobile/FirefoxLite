package org.mozilla.focus.tabs.tabtray

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel

class TabTrayViewModel : ViewModel() {
    private var hasPrivateTab = MutableLiveData<Boolean>()

    fun hasPrivateTab(): MutableLiveData<Boolean> {

        return hasPrivateTab
    }
}