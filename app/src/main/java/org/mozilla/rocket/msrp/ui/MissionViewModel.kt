package org.mozilla.rocket.msrp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionProgress
import org.mozilla.rocket.msrp.data.RewardCouponDoc
import org.mozilla.rocket.msrp.domain.LoadMissionsUseCase
import org.mozilla.rocket.msrp.domain.LoadMissionsUseCaseParameter
import org.mozilla.rocket.msrp.domain.RedeemUseCase
import org.mozilla.rocket.msrp.ui.adapter.MissionUiModel
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.TimeUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MissionViewModel(
    private val loadMissionsUseCase: LoadMissionsUseCase,
    private val redeemUseCase: RedeemUseCase
) : ViewModel() {

    val redeemResult: LiveData<RewardCouponDoc>
        get() = _redeemResult
    private val _redeemResult = MediatorLiveData<RewardCouponDoc>()

    val challengeListViewState: LiveData<State>
        get() = _missionViewState
    private val _missionViewState = MediatorLiveData<State>()

    val redeemListViewState: LiveData<State>
        get() = _redeemListViewState
    private val _redeemListViewState = MediatorLiveData<State>()

    init {
        loadMissions()
    }
    fun onRetryButtonClicked() {
        loadMissions()
    }

    private fun loadMissions() = viewModelScope.launch {
        _missionViewState.value = State.Loading
        _redeemListViewState.value = State.Loading
        val result = loadMissionsUseCase.execute(LoadMissionsUseCaseParameter())
        when (result.status) {
            Result.Status.Success -> {
                val (challengeList, redeemList) = result.data ?: (emptyList<Mission>() to emptyList())
                _missionViewState.value = if (challengeList.isNotEmpty()) {
                    State.Loaded(challengeList.toUiModel())
                } else {
                    State.Empty
                }
                _redeemListViewState.value = if (redeemList.isNotEmpty()) {
                    State.Loaded(redeemList.toUiModel())
                } else {
                    State.Empty
                }
            }
            Result.Status.Error -> when (result.error) {
                is LoadMissionsUseCase.Error.NoConnectionError -> {
                    _missionViewState.value = State.NoConnectionError
                    _redeemListViewState.value = State.NoConnectionError
                }
                is LoadMissionsUseCase.Error.UnknownError -> {
                    _missionViewState.value = State.UnknownError
                    _redeemListViewState.value = State.UnknownError
                }
            }
        }
    }

    // TODO: Evan
//    fun redeem(redeemUrl: String) = viewModelScope.launch {
//        _redeemResult.value = redeemUseCase.execute(RedeemRequest(redeemUrl)).getNotNull {
//            when (error) {
//                RedeemUseCase.Error.UnknownError -> {
//                    // TODO: Evan
//                }
//            }
//            return@launch
//        }
//    }

    sealed class State {
        data class Loaded(val data: List<MissionUiModel>) : State()
        object Empty : State()
        object Loading : State()
        object NoConnectionError : State()
        object UnknownError : State()
    }
}

private suspend fun List<Mission>.toUiModel(): List<MissionUiModel> = withContext(Dispatchers.Default) {
    map { it.toUiModel() }
}

private fun Mission.toUiModel(): MissionUiModel = when (status) {
    Mission.STATUS_NEW -> MissionUiModel.UnjoinedMission(
        title = title,
        expirationText = expiredDate.toDateString(),
        showRedDot = false, // TODO: Evan
        imageUrl = imageUrl
    )
    Mission.STATUS_JOINED -> MissionUiModel.JoinedMission(
        title = title,
        expirationText = expiredDate.toDateString(),
        imageUrl = imageUrl,
        progress = when (missionProgress) {
            is MissionProgress.TypeDaily -> { 100 * missionProgress.currentDay / missionProgress.totalDays }
            null -> error("missionProgress null")
        }
    )
    Mission.STATUS_REDEEMABLE -> {
        val expired = TimeUtils.getTimestampNow() > expiredDate
        if (expired) {
            MissionUiModel.ExpiredMission(
                title = title,
                expirationText = expiredDate.toDateString()
            )
        } else {
            MissionUiModel.RedeemableMission(
                title = title
            )
        }
    }
    Mission.STATUS_REDEEMED -> MissionUiModel.RedeemedMission(
        title = title,
        expirationTime = expiredDate.toDateString()
    )
    else -> error("unexpected mission status: $status")
}

private fun Long.toDateString(): String =
        SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault())
            .format(Date(this))