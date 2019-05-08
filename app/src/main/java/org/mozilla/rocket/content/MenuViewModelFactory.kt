package org.mozilla.rocket.content

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import org.mozilla.focus.Inject

class MenuViewModelFactory private constructor() : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MenuViewModel::class.java)) {
            return MenuViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {

        @Volatile private var INSTANCE: MenuViewModelFactory? = null

        @JvmStatic
        fun getInstance(): MenuViewModelFactory? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: MenuViewModelFactory().also { INSTANCE = it }
                }
    }
}
