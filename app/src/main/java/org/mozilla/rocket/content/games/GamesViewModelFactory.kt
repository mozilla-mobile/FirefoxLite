package org.mozilla.rocket.content.games

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.mozilla.rocket.content.games.data.GamesRepo

class GamesViewModelFactory private constructor(
    private val gamesRepo: GamesRepo
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GamesViewModel::class.java)) {
            return GamesViewModel(gamesRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {
        @JvmStatic
        val INSTANCE: GamesViewModelFactory by lazy { GamesViewModelFactory(GamesRepo()) }
    }
}