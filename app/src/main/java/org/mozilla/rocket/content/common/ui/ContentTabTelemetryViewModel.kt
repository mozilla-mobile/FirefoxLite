package org.mozilla.rocket.content.common.ui

import androidx.lifecycle.ViewModel
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData

class ContentTabTelemetryViewModel : ViewModel() {

    private var telemetryDataModel: ContentTabTelemetryData? = null
    private var sessionTime: Long = 0L

    fun initialize(telemetryData: ContentTabTelemetryData?) {
        telemetryDataModel = telemetryData
    }

    fun onSessionStarted() {
        sessionTime = System.currentTimeMillis()

        telemetryDataModel?.let {
            TelemetryWrapper.startContentTab(it)
        }
    }

    fun onSessionEnded() {
        telemetryDataModel?.sessionTime = (System.currentTimeMillis() - sessionTime)

        telemetryDataModel?.let {
            TelemetryWrapper.endContentTab(it)
        }
    }

    fun onUrlOpened() {
        telemetryDataModel?.let {
            it.urlCounts += 1
        }
    }

    fun onKeyboardShown() {
        telemetryDataModel?.let {
            it.showKeyboard = true
        }
    }
}
