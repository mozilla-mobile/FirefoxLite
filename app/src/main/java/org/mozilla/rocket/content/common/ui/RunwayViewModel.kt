package org.mozilla.rocket.content.common.ui

import androidx.lifecycle.ViewModel
import org.mozilla.rocket.content.common.adapter.RunwayItem
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData
import org.mozilla.rocket.download.SingleLiveEvent

class RunwayViewModel : ViewModel() {

    val openRunway = SingleLiveEvent<OpenLinkAction>()

    fun onRunwayItemClicked(runwayItem: RunwayItem) {
        val telemetryData = ContentTabTelemetryData(
            "",
            runwayItem.source,
            runwayItem.source,
            runwayItem.category,
            runwayItem.componentId,
            runwayItem.subCategoryId,
            0L
        )
        openRunway.value = OpenLinkAction(runwayItem.linkUrl, runwayItem.linkType, telemetryData)
    }

    data class OpenLinkAction(val url: String, val type: Int, val telemetryData: ContentTabTelemetryData)
}