package org.mozilla.rocket.msrp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.mozilla.rocket.msrp.data.LoadMissionsResult
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.RedeemResult
import org.mozilla.rocket.msrp.data.RewardServiceException
import org.mozilla.rocket.msrp.domain.LoadMissionsUseCase
import org.mozilla.rocket.msrp.domain.LoadMissionsUseCaseParameter
import org.mozilla.rocket.msrp.domain.RedeemRequest
import org.mozilla.rocket.msrp.domain.RedeemUseCase

class MissionViewModel(
    private val loadMissionsUseCase: LoadMissionsUseCase,
    private val redeemUseCase: RedeemUseCase
) : ViewModel() {

    val redeemResult: LiveData<RedeemResult>
        get() = _redeemResult
    private val _redeemResult = MediatorLiveData<RedeemResult>()

    val missions: LiveData<List<Mission>>
        get() = _missions
    private val _missions = MediatorLiveData<List<Mission>>()

    val missionViewState: LiveData<State>
        get() = _missionViewState
    private val _missionViewState = MediatorLiveData<State>()

    init {
        loadMissions()
    }

    fun loadMissions() {
        launchDataLoad {
            System.out.println("here")
            when (val result = loadMissionsUseCase.execute(LoadMissionsUseCaseParameter())) {
                is LoadMissionsResult.Success -> {
                    _missionViewState.postValue(if (result.missions.isEmpty()) {
                        State.Idle
                    } else {
                        State.Empty
                    })
                    System.out.println("here 1")
                    _missions.postValue(result.missions)
                }

                is LoadMissionsResult.Failure -> {
                    System.out.println("here 2")
                    _missionViewState.postValue(State.ServerError)
                }

                else -> {
                    System.out.println("here 3, result=$result")
                }
            }
        }
    }

    fun redeem(redeemUrl: String) {
        launchDataLoad {
            _redeemResult.postValue(redeemUseCase.execute(RedeemRequest(redeemUrl)))
        }
    }

    private fun launchDataLoad(block: suspend () -> Unit): Job {
        return viewModelScope.launch(Dispatchers.IO) {
            try {
                _missionViewState.postValue(State.Loading)
                block()
                _missionViewState.postValue(State.Idle)
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

    companion object {
        const val FAKE_URL = "http://fake_url"
    }
}