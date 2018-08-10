package org.mozilla.focus.tabs.tabtray

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel


class TabTrayViewModel : ViewModel() {
    var hasPrivateTab = MutableLiveData<Boolean>()
}