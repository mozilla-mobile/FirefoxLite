package org.mozilla.rocket.shopping.search.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
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
    }

    fun onItemToggled(index: Int, toggledOn: Boolean) {
        val shoppingSites = requireNotNull(shoppingSites.value)
        shoppingSites[index].isChecked = toggledOn
        updateShoppingSitesUseCase(shoppingSites)
    }
}