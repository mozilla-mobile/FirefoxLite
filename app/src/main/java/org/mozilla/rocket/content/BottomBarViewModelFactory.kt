package org.mozilla.rocket.content

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

class BottomBarViewModelFactory private constructor() : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BottomBarViewModel::class.java)) {
            return BottomBarViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {

        @Volatile private var INSTANCE: BottomBarViewModelFactory? = null

        @JvmStatic
        fun getInstance(): BottomBarViewModelFactory? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: BottomBarViewModelFactory().also { INSTANCE = it }
                }
    }
}
