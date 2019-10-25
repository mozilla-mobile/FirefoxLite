package org.mozilla.rocket.msrp.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.msrp.data.Mission
import org.mozilla.rocket.msrp.domain.GetCouponUseCase
import org.mozilla.rocket.util.ToastMessage
import org.mozilla.rocket.util.getNotNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MissionCouponViewModel(
    private val getCouponUseCase: GetCouponUseCase
) : ViewModel() {

    val isLoading = MutableLiveData<Boolean>()
    val couponName = MutableLiveData<String>()
    val expirationTime = MutableLiveData<String>()
    val couponCode = MutableLiveData<String>()
    val missionImage = MutableLiveData<String>()

    val showToast = SingleLiveEvent<ToastMessage>()
    val copyToClipboard = SingleLiveEvent<String>()
    val openShoppingPage = SingleLiveEvent<String>()
    val openFaqPage = SingleLiveEvent<Unit>()

    private lateinit var mission: Mission
    private var couponWebsiteUrl: String? = null

    init {
        TelemetryWrapper.showCouponPage()
    }

    fun init(mission: Mission) {
        this.mission = mission
        viewModelScope.launch {
            withContext(Dispatchers.Default) {
                expirationTime.postValue(mission.rewardExpiredDate.toDateString())
            }
        }
        couponName.value = mission.description
        missionImage.value = mission.imageUrl
        fetchCoupon(mission)
    }

    private fun fetchCoupon(mission: Mission) = viewModelScope.launch {
        isLoading.value = true

        val redeemUrl = mission.redeem
        if (redeemUrl == null) {
            showToast.value = ToastMessage(R.string.msrp_reward_challenge_error)
            isLoading.value = false
            return@launch
        }
        val coupon = getCouponUseCase(mission).getNotNull {
            showToast.value = when (it) {
                GetCouponUseCase.Error.NetworkError -> ToastMessage(R.string.msrp_reward_challenge_nointernet)
                GetCouponUseCase.Error.UnknownError -> ToastMessage(R.string.msrp_reward_challenge_error)
            }
            isLoading.value = false
            return@launch
        }
        couponCode.value = coupon.couponCode
        couponWebsiteUrl = coupon.websiteUrl
        isLoading.value = false
    }

    fun onCopyCouponButtonClicked() {
        TelemetryWrapper.copyCodeOnCouponPage()
        copyToClipboard.value = couponCode.value
        showToast.value = ToastMessage(R.string.msrp_voucher_toast)
    }

    fun onGoShoppingButtonClicked() {
        TelemetryWrapper.clickGoUseOnCouponPage()
        copyToClipboard.value = couponCode.value
        showToast.value = ToastMessage(R.string.msrp_voucher_toast)
        openShoppingPage.value = couponWebsiteUrl
    }

    fun onFaqButtonClick() {
        openFaqPage.call()
    }

    private fun Long.toDateString(): String =
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    .format(Date(this))
}