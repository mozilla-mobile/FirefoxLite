package org.mozilla.rocket.chrome

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
        @JvmStatic
        val instance: BottomBarViewModelFactory by lazy { BottomBarViewModelFactory() }
    }
}
