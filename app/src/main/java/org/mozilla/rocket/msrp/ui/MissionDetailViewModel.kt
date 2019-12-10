package org.mozilla.rocket.msrp.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.domain.BindFxAccountUseCase
import org.mozilla.rocket.msrp.domain.GetApkDownloadLinkUseCase
import org.mozilla.rocket.msrp.domain.GetIsFxAccountUseCase
import org.mozilla.rocket.msrp.domain.GetUserIdUseCase
import org.mozilla.rocket.msrp.domain.IsFxAccountUseCase
import org.mozilla.rocket.msrp.domain.IsNeedJoinMissionOnboardingUseCase
import org.mozilla.rocket.msrp.domain.JoinMissionUseCase
import org.mozilla.rocket.msrp.domain.QuitMissionUseCase
import org.mozilla.rocket.msrp.domain.ReadMissionUseCase
import org.mozilla.rocket.msrp.domain.RedeemUseCase
import org.mozilla.rocket.msrp.domain.RefreshMissionsUseCase
import org.mozilla.rocket.msrp.domain.RequestContentHubClickOnboardingUseCase
import org.mozilla.rocket.msrp.ui.MissionDetailViewModel.LoginAction.Companion.REDEEM_LOGIN
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
    private val requestContentHubClickOnboardingUseCase: RequestContentHubClickOnboardingUseCase,
    getIsFxAccountUseCase: GetIsFxAccountUseCase,
    private val getApkDownloadLinkUseCase: GetApkDownloadLinkUseCase
) : ViewModel() {

    val missionStatus = MutableLiveData<Int>()
    val title = MutableLiveData<String>()
    val missionImage = MutableLiveData<String>()
    val isLoading = MutableLiveData<Boolean>()
    val isFxAccount: LiveData<Boolean> = getIsFxAccountUseCase()

    val requestFxLogin = SingleLiveEvent<LoginAction>()
    val startMissionReminder = SingleLiveEvent<Mission>()
    val stopMissionReminder = SingleLiveEvent<Mission>()
    val closePage = SingleLiveEvent<Unit>()
    val closeAllMissionPages = SingleLiveEvent<Unit>()
    val openCouponPage = SingleLiveEvent<Mission>()
    val showToast = SingleLiveEvent<ToastMessage>()
    val openFaqPage = SingleLiveEvent<Unit>()
    val openTermsOfUsePage = SingleLiveEvent<Unit>()
    val showForceUpdateDialog = SingleLiveEvent<ForceUpdateInfo>()
    val openAppOnGooglePlay = SingleLiveEvent<Unit>()
    val openApkDownloadLink = SingleLiveEvent<String>()

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

    fun onJoinMissionButtonClicked() = viewModelScope.launch {
        isLoading.value = true
        TelemetryWrapper.clickChallengePageJoin()

        if (!isVersionValid()) {
            isLoading.value = false
            showForceUpdateDialog()
            return@launch
        }

        val joinResult = joinMissionUseCase(mission)
        if (joinResult.isSuccess) {
            startMissionReminder.value = mission
            refreshMissionsUseCase()
            if (isNeedJoinMissionOnboardingUseCase()) {
                requestContentHubClickOnboardingUseCase(couponName = mission.description)
                closeAllMissionPages.call()
            } else {
                closePage.call()
            }
        } else {
            showToast.value = when (joinResult.error!!) {
                JoinMissionUseCase.Error.NetworkError -> ToastMessage(R.string.msrp_reward_challenge_nointernet)
                JoinMissionUseCase.Error.NoQuota -> ToastMessage(R.string.msrp_voucher_nostock)
                JoinMissionUseCase.Error.AccountDisabled,
                JoinMissionUseCase.Error.UnknownError -> ToastMessage(R.string.msrp_reward_challenge_error)
            }
        }
        isLoading.value = false
    }

    private fun isVersionValid() = BuildConfig.VERSION_CODE >= mission.minVersion

    private fun showForceUpdateDialog() {
        showForceUpdateDialog.value = ForceUpdateInfo(
            title = mission.minVerDialogTitle.takeIf { it.isNotEmpty() },
            description = mission.minVerDialogMessage.takeIf { it.isNotEmpty() },
            imageUrl = mission.minVerDialogImage.takeIf { it.isNotEmpty() }
        )
    }

    fun onLeaveMissionConfirmed() = viewModelScope.launch {
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
        if (isFxAccountUseCase()) {
            val redeemResult = redeemUseCase(mission)
            if (redeemResult.isSuccess) {
                refreshMissionsUseCase()
                openCouponPage.value = mission.copy(rewardExpiredDate = redeemResult.data?.expire_date ?: 0)
            } else {
                showToast.value = when (redeemResult.error!!) {
                    RedeemUseCase.Error.NetworkError -> ToastMessage(R.string.msrp_reward_challenge_nointernet)
                    RedeemUseCase.Error.UnknownError -> ToastMessage(R.string.msrp_reward_challenge_error)
                }
            }
        } else {
            val uid = getUserIdUseCase()
            requestFxLogin.value = LoginAction.RedeemLoginAction(uid)
        }
        isLoading.value = false
    }

    fun onFxLoginToCompleted(actionId: Int, jwt: String?) = viewModelScope.launch {
        if (bindFxAccountUseCase(jwt).isSuccess) {
            TelemetryWrapper.accountSignIn()
            if (actionId == REDEEM_LOGIN) {
                redeem(mission)
            }
        } else {
            showToast.value = ToastMessage(R.string.msrp_reward_challenge_nointernet)
        }
    }

    fun onFaqButtonClick() {
        openFaqPage.call()
    }

    fun onTermsOfUseButtonClick() {
        openTermsOfUsePage.call()
    }

    fun onLoginButtonClicked() {
        if (isFxAccountUseCase()) {
            return
        }
        val uid = getUserIdUseCase()
        requestFxLogin.value = LoginAction.PureLoginAction(uid)
    }

    fun onForceUpdateDialogShown() {
        TelemetryWrapper.showUpdateMessage(mission.missionName)
    }

    fun onUpdateAppButtonClicked() {
        TelemetryWrapper.clickUpdateMessage(mission.missionName, TelemetryWrapper.Extra_Value.UPDATE)
        openAppOnGooglePlay.call()
    }

    fun onForceUpdateLaterButtonClicked() {
        TelemetryWrapper.clickUpdateMessage(mission.missionName, TelemetryWrapper.Extra_Value.LATER)
    }

    fun onForceUpdateCloseButtonClicked() {
        TelemetryWrapper.clickUpdateMessage(mission.missionName, TelemetryWrapper.Extra_Value.CLOSE)
    }

    fun onForceUpdateDialogCanceled() {
        TelemetryWrapper.clickUpdateMessage(mission.missionName, TelemetryWrapper.Extra_Value.DISMISS)
    }

    fun onOpenAppOnGooglePlayFailed() {
        openApkDownloadLink.value = getApkDownloadLinkUseCase()
    }

    sealed class LoginAction(val actionId: Int, val uid: String) {
        class PureLoginAction(uid: String) : LoginAction(PURE_LOGIN, uid)
        class RedeemLoginAction(uid: String) : LoginAction(REDEEM_LOGIN, uid)

        companion object {
            const val PURE_LOGIN = 0
            const val REDEEM_LOGIN = 1
        }
    }

    data class ForceUpdateInfo(
        val title: String?,
        val description: String?,
        val imageUrl: String?
    )
}