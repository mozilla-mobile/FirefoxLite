package org.mozilla.rocket.content.common.ui

import android.os.Looper
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper

class VerticalTelemetryViewModel : ViewModel() {

    private lateinit var vertical: String
    private var category: String = ""
    private var versionIdMap = HashMap<String, Long>()
    private val impressionMap = HashMap<String, HashMap<String, Int>>()

    private val _impression = MutableLiveData<Impression>()
    val impression: LiveData<Impression>
        get() = _impression

    fun onSessionStarted(vertical: String) {
        this.vertical = vertical
        TelemetryWrapper.startVerticalProcess(vertical)
    }

    fun onSessionEnded() {
        triggerCategoryImpressionTelemetryEvent()
        // Load time will be recorded in the content tab end lifecycle
        TelemetryWrapper.endVerticalProcess(vertical, 0L)
    }

    fun onCategorySelected(newCategory: String) {
        triggerCategoryImpressionTelemetryEvent()
        if (category.isNotEmpty()) {
            impressionMap[category]?.let {
                _impression.value = Impression(category, it, true)
            }
        }

        category = newCategory
        impressionMap[category]?.let {
            _impression.value = Impression(category, it, true)
        }
        TelemetryWrapper.openCategory(vertical, newCategory)
    }

    fun onRefreshClicked() {
        triggerCategoryImpressionTelemetryEvent()
        category = ""
        versionIdMap.clear()
        impressionMap.clear()
    }

    fun updateVersionId(category: String, version: Long) {
        versionIdMap[category] = version
    }

    fun updateImpression(category: String, subCategoryId: String, maxIndex: Int) {
        val index = impressionMap[category]?.get(subCategoryId) ?: 0
        if (index < maxIndex) {
            val isFirstImpression = (impressionMap[category] == null)
            if (impressionMap[category] == null) {
                impressionMap[category] = hashMapOf((subCategoryId to maxIndex))
            } else {
                impressionMap[category]?.set(subCategoryId, maxIndex)
            }

            if (this.category == category) {
                impressionMap[category]?.let {
                    _impression.value = Impression(category, it, isFirstImpression)
                }
            }
        }
    }

    private fun triggerCategoryImpressionTelemetryEvent() {
        if (category.isNotEmpty() && versionIdMap[category] != 0L && impressionMap[category]?.isNotEmpty() == true) {
            TelemetryWrapper.categoryImpression(
                versionIdMap[category].toString(),
                category,
                impressionMap[category].toString().replace("=", ":")
            )
        }
    }
}

fun RecyclerView.firstImpression(telemetryViewModel: VerticalTelemetryViewModel, category: String, subCategoryId: String) {
    setTag(R.id.telemetry_category_name, category)
    setTag(R.id.telemetry_subcategory_id, subCategoryId)

    Looper.myQueue().addIdleHandler {
        updateImpression(
            telemetryViewModel,
            category,
            subCategoryId)
        false
    }
}

fun RecyclerView.monitorScrollImpression(telemetryViewModel: VerticalTelemetryViewModel) {
    this.addOnScrollListener(object : RecyclerView.OnScrollListener() {
        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                val category = getTag(R.id.telemetry_category_name) as? String ?: ""
                val subCategoryId = getTag(R.id.telemetry_subcategory_id) as? String ?: ""
                updateImpression(telemetryViewModel, category, subCategoryId)
            }
        }
    })
}

private fun RecyclerView.updateImpression(telemetryViewModel: VerticalTelemetryViewModel, category: String, subCategoryId: String) {
    val lastVisibleItemPosition = if (layoutManager is LinearLayoutManager) {
        (layoutManager as LinearLayoutManager).findLastVisibleItemPosition() + 1
    } else {
        0
    }

    if (category.isNotEmpty() && subCategoryId.isNotEmpty()) {
        telemetryViewModel.updateImpression(
            category,
            subCategoryId,
            lastVisibleItemPosition
        )
    }
}

data class Impression(
    val category: String,
    val positionMap: HashMap<String, Int>,
    val significant: Boolean = false // is it the first or last impression?
)