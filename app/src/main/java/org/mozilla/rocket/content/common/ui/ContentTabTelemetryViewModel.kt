package org.mozilla.rocket.content.common.ui

import androidx.lifecycle.ViewModel
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.common.data.ContentTabTelemetryData

class ContentTabTelemetryViewModel : ViewModel() {

    private var telemetryDataModel: ContentTabTelemetryData? = null
    private var sessionTimeStart: Long = 0L
    private var lastUrlLoadStart = 0L
    private var lastUrlLoadEnd = 0L
    private var lastUrlLoadTime = 0L

    fun initialize(telemetryData: ContentTabTelemetryData?) {
        telemetryDataModel = telemetryData
    }

    fun onSessionStarted() {
        sessionTimeStart = System.currentTimeMillis()

        telemetryDataModel?.let {
            TelemetryWrapper.startVerticalProcess(it.vertical)
            TelemetryWrapper.startContentTab(it)
        }
    }

    fun onSessionEnded() {
        telemetryDataModel?.sessionTime = (System.currentTimeMillis() - sessionTimeStart)

        telemetryDataModel?.let {
            TelemetryWrapper.endContentTab(it)

            lastUrlLoadTime = if (lastUrlLoadEnd > lastUrlLoadStart) {
                lastUrlLoadEnd - lastUrlLoadStart
            } else {
                0L
            }
            TelemetryWrapper.endVerticalProcess(it.vertical, lastUrlLoadTime)
        }
    }

    fun onPageLoadingStarted() {
        lastUrlLoadStart = System.currentTimeMillis()
    }

    fun onPageLoadingStopped() {
        if (lastUrlLoadStart > 0L) {
            lastUrlLoadEnd = System.currentTimeMillis()
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

    fun onBackButtonClicked() {
        telemetryDataModel?.let {
            TelemetryWrapper.clickContentTabToolbar(TelemetryWrapper.Value.BACK, 0, it)
        }
    }

    fun onReloadButtonClicked() {
        telemetryDataModel?.let {
            TelemetryWrapper.clickContentTabToolbar(TelemetryWrapper.Value.RELOAD, 1, it)
        }
    }

    fun onShareButtonClicked() {
        telemetryDataModel?.let {
            TelemetryWrapper.clickContentTabToolbar(TelemetryWrapper.Value.SHARE, 2, it)
        }
    }
}
