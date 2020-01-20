package org.mozilla.rocket.content.common.ui

import android.os.SystemClock
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
        sessionTimeStart = SystemClock.elapsedRealtime()

        telemetryDataModel?.let {
            TelemetryWrapper.startVerticalProcess(it.vertical)
            TelemetryWrapper.startContentTab(it)
        }
    }

    fun onSessionEnded() {
        telemetryDataModel?.sessionTime = (SystemClock.elapsedRealtime() - sessionTimeStart)

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
        lastUrlLoadStart = SystemClock.elapsedRealtime()
    }

    fun onPageLoadingStopped() {
        if (lastUrlLoadStart > 0L) {
            lastUrlLoadEnd = SystemClock.elapsedRealtime()
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
            TelemetryWrapper.clickContentTabToolbarBack(0, it)
        }
    }

    fun onReloadButtonClicked() {
        telemetryDataModel?.let {
            TelemetryWrapper.clickContentTabToolbarReload(1, it)
        }
    }

    fun onShareButtonClicked() {
        telemetryDataModel?.let {
            TelemetryWrapper.clickContentTabToolbarShare(2, it)
        }
    }
}
