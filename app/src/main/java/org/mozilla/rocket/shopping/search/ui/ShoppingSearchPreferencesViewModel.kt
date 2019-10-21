package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.extension.swap
import org.mozilla.rocket.extension.switchMap
import org.mozilla.rocket.shopping.search.domain.GetShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.domain.UpdateShoppingSitesUseCase
import org.mozilla.rocket.shopping.search.ui.adapter.ShoppingSiteItem

class ShoppingSearchPreferencesViewModel(
    getShoppingSitesUseCase: GetShoppingSitesUseCase,
    private val updateShoppingSitesUseCase: UpdateShoppingSitesUseCase
) : ViewModel() {

    val shoppingSites: LiveData<List<ShoppingSiteItem>>

    private val hasSettingsChanged: MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply { value = false }
    private var isEditMode: MutableLiveData<Boolean> = MutableLiveData<Boolean>().apply { value = false }
    private val savedShoppingSites: LiveData<List<ShoppingSiteItem>> = getShoppingSitesUseCase()
    private val editingShoppingSites: MutableLiveData<List<ShoppingSiteItem>> = MutableLiveData()

    init {
        shoppingSites = isEditMode.switchMap { isEditMode ->
            if (isEditMode) {
                editingShoppingSites
            } else {
                savedShoppingSites
            }
        }
    }

    fun onEditModeStart() {
        editingShoppingSites.value = savedShoppingSites.value
        isEditMode.value = true
    }

    fun onEditModeEnd() {
        updateShoppingSitesUseCase(requireNotNull(editingShoppingSites.value))
        isEditMode.value = false
    }

    fun onItemMoved(fromPosition: Int, toPosition: Int) {
        editingShoppingSites.value = requireNotNull(editingShoppingSites.value)
                .swap(fromPosition, toPosition)
        hasSettingsChanged.value = true
    }

    fun onItemToggled(index: Int, toggledOn: Boolean) {
        val shoppingSites = requireNotNull(shoppingSites.value)
        shoppingSites[index].isChecked = toggledOn
        updateShoppingSitesUseCase(shoppingSites)
        hasSettingsChanged.value = true
    }

    fun onExitSettings() {
        if (hasSettingsChanged.value == true) {
            val currentSettings = shoppingSites.value
                ?.filter { item -> item.isChecked }
                ?.map { it -> it.title }
                ?.toString()
                ?: ""
            TelemetryWrapper.changeTabSwipeSettings(currentSettings)
        }
    }
}
