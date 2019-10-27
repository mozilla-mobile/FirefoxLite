package org.mozilla.rocket.content.common.ui

import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.focus.telemetry.TelemetryWrapper

class VerticalTelemetryViewModel : ViewModel() {

    private lateinit var vertical: String
    private var category: String = ""
    private var versionId: Long = 0L
    private val impression = HashMap<String, Int>()

    fun onSessionStarted(vertical: String) {
        this.vertical = vertical
        TelemetryWrapper.startVerticalProcess(vertical)
    }

    fun onSessionEnded() {
        // Load time will be recorded in the content tab end lifecycle
        TelemetryWrapper.endVerticalProcess(vertical, 0L)

        triggerCategoryImpressionTelemetryEvent()
    }

    fun onCategorySelected(newCategory: String) {
        triggerCategoryImpressionTelemetryEvent()
        if (category != newCategory) {
            category = newCategory
            versionId = 0L
            impression.clear()
        }

        TelemetryWrapper.openCategory(vertical, newCategory)
    }

    fun onRefreshClicked() {
        triggerCategoryImpressionTelemetryEvent()
        category = ""
        versionId = 0L
    }

    fun updateVersionId(version: Long) {
        versionId = version
    }

    fun updateImpression(subCategoryId: String, maxIndex: Int) {
        val index = impression[subCategoryId] ?: 0
        if (index < maxIndex) {
            impression[subCategoryId] = maxIndex
        }
    }

    private fun triggerCategoryImpressionTelemetryEvent() {
        if (category.isNotEmpty() && versionId != 0L) {
            TelemetryWrapper.categoryImpression(
                versionId.toString(),
                category,
                impression.toString().replace("=", ":")
            )
        }
    }
}

fun RecyclerView.updateImpression(telemetryViewModel: VerticalTelemetryViewModel, subCategoryId: String) {
    if (layoutManager is LinearLayoutManager) {
        telemetryViewModel.updateImpression(
            subCategoryId,
            (layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
        )
    }
}

fun RecyclerView.monitorScrollImpression(telemetryViewModel: VerticalTelemetryViewModel, subCategoryId: String) {
    this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                if (recyclerView.layoutManager is LinearLayoutManager) {
                    telemetryViewModel.updateImpression(
                        subCategoryId,
                        (recyclerView.layoutManager as LinearLayoutManager).findLastVisibleItemPosition()
                    )
                }
            }
        }
    })
}
