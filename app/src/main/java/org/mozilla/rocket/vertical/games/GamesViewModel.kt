package org.mozilla.rocket.vertical.games

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class GamesViewModel : ViewModel() {

    val browserGamesItems = MutableLiveData<List<Item>>()

    sealed class Item
}