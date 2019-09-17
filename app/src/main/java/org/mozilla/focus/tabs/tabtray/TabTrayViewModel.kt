package org.mozilla.focus.tabs.tabtray

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.rocket.shopping.search.data.ShoppingSearchMode

class TabTrayViewModel : ViewModel() {
    private var hasPrivateTab = MutableLiveData<Boolean>()
    private val _uiModel = MutableLiveData<ShoppingSearchTabItemUiModel>()
    val uiModel: LiveData<ShoppingSearchTabItemUiModel>
        get() = _uiModel

    fun hasPrivateTab(): MutableLiveData<Boolean> {
        return hasPrivateTab
    }

    fun checkShoppingSearchMode(context: Context) {
        ShoppingSearchMode.getInstance(context).let {
            emitUiModel(it.hasShoppingSearchActivity(), it.retrieveKeyword() ?: "")
        }
    }

    fun finishShoppingSearchMode(context: Context) {
        ShoppingSearchMode.getInstance(context).finish()
        emitUiModel(false, "")
    }

    private fun emitUiModel(showShoppingSearch: Boolean, keyword: String) {
        _uiModel.value = ShoppingSearchTabItemUiModel(showShoppingSearch, keyword)
    }
}

data class ShoppingSearchTabItemUiModel(
    val showShoppingSearch: Boolean,
    val keyword: String
)