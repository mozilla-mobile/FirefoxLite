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
import org.mozilla.rocket.msrp.domain.HasUnreadMissionsUseCase
import org.mozilla.rocket.msrp.domain.LoadMissionsUseCase
import org.mozilla.rocket.msrp.domain.ReadMissionUseCase
import org.mozilla.rocket.msrp.domain.RedeemUseCase
import org.mozilla.rocket.msrp.ui.adapter.MissionUiModel
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.TimeUtils
import org.mozilla.rocket.util.getNotNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MissionViewModel(
    private val loadMissionsUseCase: LoadMissionsUseCase,
    private val readMissionUseCase: ReadMissionUseCase,
    private val hasUnreadMissionsUseCase: HasUnreadMissionsUseCase,
    private val redeemUseCase: RedeemUseCase
) : ViewModel() {

    val redeemResult: LiveData<RewardCouponDoc>
        get() = _redeemResult
    private val _redeemResult = MediatorLiveData<RewardCouponDoc>()

    val challengeListViewState: LiveData<State>
        get() = _missionViewState
    private val _missionViewState = MediatorLiveData<State>()

    val hasUnreadMissions: LiveData<Boolean>
        get() = _hasUnreadMissions
    private val _hasUnreadMissions = MediatorLiveData<Boolean>()

    val redeemListViewState: LiveData<State>
        get() = _redeemListViewState
    private val _redeemListViewState = MediatorLiveData<State>()

    private var missionsLiveData: LiveData<Result<Pair<List<Mission>, List<Mission>>, LoadMissionsUseCase.Error>>? = null

    init {
        loadMissions()
    }

    fun onRetryButtonClicked() {
        loadMissions()
    }

    private fun loadMissions() = viewModelScope.launch {
        _missionViewState.value = State.Loading
        _redeemListViewState.value = State.Loading

        val oldSource = missionsLiveData
        val newSource = loadMissionsUseCase()
        missionsLiveData = newSource

        oldSource?.let {
            _missionViewState.removeSource(it)
            _redeemListViewState.removeSource(it)
            _hasUnreadMissions.removeSource(it)
        }

        _missionViewState.addSource(newSource) { result ->
            viewModelScope.launch {
                _missionViewState.value = parseChallengeListResult(result)
            }
        }
        _redeemListViewState.addSource(newSource) { result ->
            viewModelScope.launch {
                _redeemListViewState.value = parseRedeemListResult(result)
            }
        }
        _hasUnreadMissions.addSource(newSource) { result ->
            val challengeList = result.data?.first ?: emptyList()
            _hasUnreadMissions.value = hasUnreadMissionsUseCase(challengeList)
        }
    }

    private suspend fun parseChallengeListResult(
        result: Result<Pair<List<Mission>, List<Mission>>, LoadMissionsUseCase.Error>
    ): State {
        val (challengeList, _) = result.getNotNull { error ->
            return when (error) {
                is LoadMissionsUseCase.Error.NoConnectionError -> State.NoConnectionError
                is LoadMissionsUseCase.Error.UnknownError -> State.UnknownError
            }
        }
        return if (challengeList.isNotEmpty()) {
            State.Loaded(challengeList.toUiModel())
        } else {
            State.Empty
        }
    }

    private suspend fun parseRedeemListResult(
        result: Result<Pair<List<Mission>, List<Mission>>, LoadMissionsUseCase.Error>
    ): State {
        val (_, redeemList) = result.getNotNull { error ->
            return when (error) {
                is LoadMissionsUseCase.Error.NoConnectionError -> State.NoConnectionError
                is LoadMissionsUseCase.Error.UnknownError -> State.UnknownError
            }
        }
        return if (redeemList.isNotEmpty()) {
            State.Loaded(redeemList.toUiModel())
        } else {
            State.Empty
        }
    }

    fun onMissionRead(missionId: String) = viewModelScope.launch {
        readMissionUseCase(missionId)
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
        expirationText = joinEndDate.toDateString(),
        showRedDot = unread,
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