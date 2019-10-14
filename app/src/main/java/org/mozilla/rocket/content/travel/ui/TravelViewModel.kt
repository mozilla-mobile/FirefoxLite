package org.mozilla.rocket.content.travel.ui

import androidx.lifecycle.ViewModel
import org.mozilla.rocket.download.SingleLiveEvent

class TravelViewModel : ViewModel() {

    val loadTabs = SingleLiveEvent<Unit>()

    fun onRefreshClicked() {
        loadTabs.call()
    }

    fun initTabs() {
        loadTabs.call()
    }
}