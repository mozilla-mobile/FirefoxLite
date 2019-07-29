package org.mozilla.rocket.vertical.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class GamesViewModelFactory private constructor() : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GamesViewModel::class.java)) {
            return GamesViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {
        @JvmStatic
        val INSTANCE: GamesViewModelFactory by lazy { GamesViewModelFactory() }
    }
}