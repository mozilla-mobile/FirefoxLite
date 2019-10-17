package org.mozilla.rocket.msrp.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.domain.BindFxAccountUseCase
import org.mozilla.rocket.msrp.domain.GetUserIdUseCase
import org.mozilla.rocket.msrp.domain.IsFxAccountUseCase
import org.mozilla.rocket.msrp.domain.IsNeedJoinMissionOnboardingUseCase
import org.mozilla.rocket.msrp.domain.JoinMissionUseCase
import org.mozilla.rocket.msrp.domain.QuitMissionUseCase
import org.mozilla.rocket.msrp.domain.ReadMissionUseCase
import org.mozilla.rocket.msrp.domain.RedeemUseCase
import org.mozilla.rocket.msrp.domain.RefreshMissionsUseCase
import org.mozilla.rocket.msrp.domain.RequestContentHubClickOnboardingUseCase
import org.mozilla.rocket.util.ToastMessage
import org.mozilla.rocket.util.isSuccess

class MissionDetailViewModel(
    private val readMissionUseCase: ReadMissionUseCase,
    private val joinMissionUseCase: JoinMissionUseCase,
    private val quitMissionUseCase: QuitMissionUseCase,
    private val refreshMissionsUseCase: RefreshMissionsUseCase,
    private val redeemUseCase: RedeemUseCase,
    private val isFxAccountUseCase: IsFxAccountUseCase,
    private val getUserIdUseCase: GetUserIdUseCase,
    private val bindFxAccountUseCase: BindFxAccountUseCase,
    private val isNeedJoinMissionOnboardingUseCase: IsNeedJoinMissionOnboardingUseCase,
    private val requestContentHubClickOnboardingUseCase: RequestContentHubClickOnboardingUseCase
) : ViewModel() {

    val missionStatus = MutableLiveData<Int>()
    val title = MutableLiveData<String>()
    val missionImage = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()

    val requestFxLogin = SingleLiveEvent<String>()
    val startMissionReminder = SingleLiveEvent<Mission>()
    val stopMissionReminder = SingleLiveEvent<Mission>()
    val closePage = SingleLiveEvent<Unit>()
    val closeAllMissionPages = SingleLiveEvent<Unit>()
    val openCouponPage = SingleLiveEvent<Mission>()
    val showToast = SingleLiveEvent<ToastMessage>()

    private lateinit var mission: Mission

    fun init(mission: Mission) {
        this.mission = mission
        missionStatus.value = mission.status
        title.value = mission.title
        missionImage.value = mission.imageUrl
    }

    fun onMissionDetailViewed() = viewModelScope.launch {
        if (mission.unread) {
            mission.unread = false
            readMissionUseCase(mission.uniqueId)
        }
    }

    fun onJoinMissionButtonClicked() {
        TelemetryWrapper.clickChallengePageJoin()
        join(mission)
    }

    private fun join(mission: Mission) = viewModelScope.launch {
        isLoading.value = true
        if (isFxAccountUseCase()) {
            val joinResult = joinMissionUseCase(mission)
            if (joinResult.isSuccess) {
                startMissionReminder.value = mission
                refreshMissionsUseCase()
                if (isNeedJoinMissionOnboardingUseCase()) {
                    requestContentHubClickOnboardingUseCase()
                    closeAllMissionPages.call()
                } else {
                    closePage.call()
                }
            } else {
                showToast.value = when (joinResult.error!!) {
                    JoinMissionUseCase.Error.NetworkError -> ToastMessage(R.string.msrp_reward_challenge_nointernet)
                    JoinMissionUseCase.Error.AccountDisabled,
                    JoinMissionUseCase.Error.UnknownError -> ToastMessage(R.string.msrp_reward_challenge_error)
                }
            }
        } else {
            val uid = getUserIdUseCase()
            requestFxLogin.value = uid
        }
        isLoading.value = false
    }

    fun onQuitMissionButtonClicked() = viewModelScope.launch {
        isLoading.value = true
        val quitResult = quitMissionUseCase(mission)
        if (quitResult.isSuccess) {
            stopMissionReminder.value = mission
            refreshMissionsUseCase()
            closePage.call()
        } else {
            showToast.value = when (quitResult.error!!) {
                QuitMissionUseCase.Error.NetworkError -> ToastMessage(R.string.msrp_reward_challenge_nointernet)
                QuitMissionUseCase.Error.AccountDisabled,
                QuitMissionUseCase.Error.UnknownError -> ToastMessage(R.string.msrp_reward_challenge_error)
            }
        }
        isLoading.value = false
    }

    fun onRedeemButtonClicked() {
        TelemetryWrapper.clickChellengePageLogin()
        redeem(mission)
    }

    private fun redeem(mission: Mission) = viewModelScope.launch {
        isLoading.value = true
        val redeemResult = redeemUseCase(mission)
        if (redeemResult.isSuccess) {
            refreshMissionsUseCase()
            openCouponPage.value = mission
        } else {
            showToast.value = when (redeemResult.error!!) {
                RedeemUseCase.Error.NetworkError -> ToastMessage(R.string.msrp_reward_challenge_nointernet)
                RedeemUseCase.Error.UnknownError -> ToastMessage(R.string.msrp_reward_challenge_error)
            }
        }
        isLoading.value = false
    }

    fun onFxLoginCompleted(jwt: String?) = viewModelScope.launch {
        if (bindFxAccountUseCase(jwt).isSuccess) {
            TelemetryWrapper.accountSignIn()
            join(mission)
        } else {
            showToast.value = ToastMessage(R.string.msrp_reward_challenge_nointernet)
        }
    }
}