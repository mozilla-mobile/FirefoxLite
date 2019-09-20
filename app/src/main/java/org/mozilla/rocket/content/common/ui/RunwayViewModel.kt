package org.mozilla.rocket.content.common.ui

import androidx.lifecycle.ViewModel
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.download.SingleLiveEvent

class RunwayViewModel : ViewModel() {

    val openRunway = SingleLiveEvent<String>()

    fun onRunwayItemClicked(runwayItem: RunwayItem) {
        openRunway.value = runwayItem.linkUrl
    }
}