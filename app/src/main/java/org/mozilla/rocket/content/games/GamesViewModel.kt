package org.mozilla.rocket.content.games

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.rocket.adapter.DelegateAdapter

class GamesViewModel : ViewModel() {

    val browserGamesState = MutableLiveData<State>().apply { value = State.Idle }
    val browserGamesItems = MutableLiveData<List<DelegateAdapter.UIModel>>()

    sealed class State {
        object Idle : State()
        object Loading : State()
        class Error(val t: Throwable) : State()
    }
}