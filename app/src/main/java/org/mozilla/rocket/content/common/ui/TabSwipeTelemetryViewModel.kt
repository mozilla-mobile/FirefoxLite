package org.mozilla.rocket.content.common.ui

import androidx.lifecycle.ViewModel
import org.mozilla.focus.telemetry.TelemetryWrapper

class TabSwipeTelemetryViewModel : ViewModel() {

    private lateinit var vertical: String
    private var lastUrlLoadStart = 0L
    private var lastUrlLoadEnd = 0L
    private var lastUrlLoadTime = 0L

    fun onProcessSessionStarted(vertical: String) {
        this.vertical = vertical
        lastUrlLoadStart = 0L
        lastUrlLoadEnd = 0L
        lastUrlLoadTime = 0L
        TelemetryWrapper.startTabSwipeProcess(vertical)
    }

    fun onProcessSessionEnded() {
        lastUrlLoadTime = if (lastUrlLoadEnd > lastUrlLoadStart) {
            lastUrlLoadEnd - lastUrlLoadStart
        } else {
            0L
        }
        TelemetryWrapper.endTabSwipeProcess(vertical, lastUrlLoadTime)
    }

    fun onPageLoadingStarted() {
        lastUrlLoadStart = System.currentTimeMillis()
    }

    fun onPageLoadingStopped() {
        if (lastUrlLoadStart > 0L) {
            lastUrlLoadEnd = System.currentTimeMillis()
        }
    }
}
