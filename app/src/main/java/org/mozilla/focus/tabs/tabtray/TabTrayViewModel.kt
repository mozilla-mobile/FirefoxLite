package org.mozilla.focus.tabs.tabtray

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel


class TabTrayViewModel : ViewModel() {
    private var hasPrivateTab = MutableLiveData<Boolean>()

    fun hasPrivateTab(): MutableLiveData<Boolean> {

        return hasPrivateTab
    }


}