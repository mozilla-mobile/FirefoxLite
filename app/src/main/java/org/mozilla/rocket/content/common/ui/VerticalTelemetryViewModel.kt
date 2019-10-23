package org.mozilla.rocket.content.common.ui

import androidx.lifecycle.ViewModel
import org.mozilla.focus.telemetry.TelemetryWrapper

class VerticalTelemetryViewModel : ViewModel() {

    fun onSessionStarted(vertical: String) {
        TelemetryWrapper.startVerticalProcess(vertical)
    }

    fun onSessionEnded(vertical: String) {
        // Load time will be recorded in the content tab end lifecycle
        TelemetryWrapper.endVerticalProcess(vertical, 0L)
    }
}