package org.mozilla.rocket.chrome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class BottomBarViewModelFactory : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BottomBarViewModel::class.java)) {
            return BottomBarViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}
