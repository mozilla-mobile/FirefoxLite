package org.mozilla.rocket.content.common.ui

import androidx.lifecycle.ViewModel
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.content.common.data.TabSwipeTelemetryData

class TabSwipeTelemetryViewModel : ViewModel() {

    // process scope
    private lateinit var vertical: String
    private var lastUrlLoadStart = 0L
    private var lastUrlLoadEnd = 0L
    private var lastUrlLoadTime = 0L

    // tab scope
    private var telemetryDataModel: TabSwipeTelemetryData? = null
    private var tabSessionStart: Long = 0L

    fun onProcessSessionStarted(vertical: String) {
        this.vertical = vertical
        lastUrlLoadStart = 0L
        lastUrlLoadEnd = 0L
        lastUrlLoadTime = 0L
        TelemetryWrapper.startTabSwipeProcess(vertical)
    }

    fun onProcessSessionEnded() {
        onTabSessionEnded()

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

    fun onTabSelected(feed: String, source: String) {
        onTabSessionEnded()
        onTabSessionStarted(feed, source)
    }

    private fun onTabSessionStarted(feed: String, source: String) {
        telemetryDataModel = TabSwipeTelemetryData(vertical, feed, source)
        tabSessionStart = System.currentTimeMillis()

        telemetryDataModel?.let {
            TelemetryWrapper.startTabSwipe(it)
        }
    }

    private fun onTabSessionEnded() {
        telemetryDataModel?.let {
            it.sessionTime = (System.currentTimeMillis() - tabSessionStart)
            TelemetryWrapper.endTabSwipe(it)
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
