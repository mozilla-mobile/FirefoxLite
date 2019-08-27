package org.mozilla.rocket.msrp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.RewardServiceException
import org.mozilla.rocket.msrp.domain.LoadMissionsUseCase
import org.mozilla.rocket.msrp.domain.LoadMissionsUseCaseParameter

class MissionViewModel(private val loadMissionsUseCase: LoadMissionsUseCase) : ViewModel() {

    val missions: LiveData<List<Mission>>
        get() = _missions
    private val _missions = MediatorLiveData<List<Mission>>()

    val missionViewState: LiveData<State>
        get() = _missionViewState
    private val _missionViewState = MediatorLiveData<State>()

    fun loadMissions(missionGroupURI: String) {
        launchDataLoad {
            loadMissionsUseCase.execute(LoadMissionsUseCaseParameter(missionGroupURI)).items.let {
                if (it.isEmpty()) {
                    _missionViewState.postValue(State.Empty)
                } else {
                    _missionViewState.postValue(State.Idle)
                }
                _missions.postValue(it)
            }
        }
    }

    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch {
            try {
                _missionViewState.value = State.Loading
                block()
                _missionViewState.value = State.Idle
            } catch (t: RewardServiceException) {
                val errorState = when (t) {
                    is RewardServiceException.ServerErrorException -> State.AuthError
                    is RewardServiceException.AuthorizationException -> State.ServerError
                }
                _missionViewState.postValue(errorState)
            }
        }
    }

    sealed class State {
        object Idle : State()
        object Empty : State()
        object Loading : State()
        object AuthError : State()
        object ServerError : State()
    }
}