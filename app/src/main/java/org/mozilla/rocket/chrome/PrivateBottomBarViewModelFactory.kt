package org.mozilla.rocket.chrome

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

class PrivateBottomBarViewModelFactory private constructor() : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PrivateBottomBarViewModel::class.java)) {
            return PrivateBottomBarViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {
        @JvmStatic
        val instance: PrivateBottomBarViewModelFactory by lazy { PrivateBottomBarViewModelFactory() }
    }
}
