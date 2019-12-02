package org.mozilla.rocket.msrp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.data.MissionProgress
import org.mozilla.rocket.msrp.domain.GetChallengeMissionsUseCase
import org.mozilla.rocket.msrp.domain.GetRedeemMissionsUseCase
import org.mozilla.rocket.msrp.domain.RefreshMissionsUseCase
import org.mozilla.rocket.msrp.ui.adapter.MissionUiModel
import org.mozilla.rocket.util.isSuccess
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MissionViewModel(
    getChallengeMissionsUseCase: GetChallengeMissionsUseCase,
    getRedeemMissionsUseCase: GetRedeemMissionsUseCase,
    private val refreshMissionsUseCase: RefreshMissionsUseCase
) : ViewModel() {

    val challengeList: LiveData<List<MissionUiModel>>
    val isChallengeListEmpty = MutableLiveData<Boolean>()
    val challengeListViewState = MutableLiveData<State>()
    val redeemList: LiveData<List<MissionUiModel>>
    val isRedeemListEmpty = MutableLiveData<Boolean>()
    val redeemListViewState = MutableLiveData<State>()

    val openMissionDetailPage = SingleLiveEvent<Mission>()
    val openRedeemPage = SingleLiveEvent<Mission>()

    private var challengeMissions: List<Mission> = emptyList()
    private var redeemMissions: List<Mission> = emptyList()

    init {
        challengeList = getChallengeMissionsUseCase().map {
            challengeMissions = it
            isChallengeListEmpty.value = it.isEmpty()
            it.toUiModel()
        }
        redeemList = getRedeemMissionsUseCase().map {
            redeemMissions = it
            isRedeemListEmpty.value = it.isEmpty()
            it.toUiModel()
        }

        refreshMissions()
    }

    fun onRetryButtonClicked() {
        refreshMissions()
    }

    private fun refreshMissions() = viewModelScope.launch {
        challengeListViewState.value = State.Loading
        redeemListViewState.value = State.Loading

        val refreshResult = refreshMissionsUseCase()
        val state = if (refreshResult.isSuccess) {
            State.Loaded
        } else {
            when (refreshResult.error!!) {
                is RefreshMissionsUseCase.Error.AccountDisabled -> State.Loaded
                is RefreshMissionsUseCase.Error.NetworkError -> State.NoConnectionError
                is RefreshMissionsUseCase.Error.UnknownError -> State.UnknownError
            }
        }
        challengeListViewState.value = state
        redeemListViewState.value = state
    }

    fun onChallengeItemClicked(position: Int) {
        val mission = challengeMissions[position]
        TelemetryWrapper.clickContentHomeItem(TelemetryWrapper.Extra_Value.REWARDS, TelemetryWrapper.Extra_Value.MISSION, mission.uniqueId, mission.missionName)
        openMissionDetailPage.value = mission
    }

    fun onRedeemItemClicked(position: Int) {
        val mission = redeemMissions[position]
        TelemetryWrapper.clickContentHomeItem(TelemetryWrapper.Extra_Value.REWARDS, TelemetryWrapper.Extra_Value.GIFT, mission.uniqueId, mission.missionName)
        when (mission.status) {
            Mission.STATUS_REDEEMABLE -> {
                val expired = System.currentTimeMillis() > mission.redeemEndDate
                if (!expired) {
                    openMissionDetailPage.value = mission
                }
            }
            Mission.STATUS_REDEEMED -> {
                val expired = System.currentTimeMillis() > mission.rewardExpiredDate
                if (!expired) {
                    openRedeemPage.value = mission
                }
            }
        }
    }

    sealed class State {
        object Loaded : State()
        object Loading : State()
        object NoConnectionError : State()
        object UnknownError : State()
    }
}

private fun List<Mission>.toUiModel(): List<MissionUiModel> = map { it.toUiModel() }

private fun Mission.toUiModel(): MissionUiModel = when (status) {
    Mission.STATUS_NEW -> MissionUiModel.UnjoinedMission(
        title = title,
        expirationTime = joinEndDate.toDateString(),
        showRedDot = unread,
        imageUrl = imageUrl
    )
    Mission.STATUS_JOINED -> MissionUiModel.JoinedMission(
        title = title,
        expirationTime = expiredDate.toDateString(),
        imageUrl = imageUrl,
        progress = when (missionProgress) {
            is MissionProgress.TypeDaily -> 100 * missionProgress.currentDay / missionProgress.totalDays
            null -> error("missionProgress null")
        }
    )
    Mission.STATUS_REDEEMABLE -> {
        val expired = System.currentTimeMillis() > redeemEndDate
        if (!expired) {
            MissionUiModel.RedeemableMission(
                title = title,
                expirationTime = redeemEndDate.toDateString()
            )
        } else {
            MissionUiModel.ExpiredMission(
                title = title
            )
        }
    }
    Mission.STATUS_REDEEMED -> {
        val expired = System.currentTimeMillis() > rewardExpiredDate
        if (!expired) {
            MissionUiModel.RedeemedMission(
                title = title,
                expirationTime = rewardExpiredDate.toDateString()
            )
        } else {
            MissionUiModel.ExpiredMission(
                title = title
            )
        }
    }
    else -> error("unexpected mission status: $status")
}

private fun Long.toDateString(): String =
        SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.getDefault())
                .format(Date(this))
