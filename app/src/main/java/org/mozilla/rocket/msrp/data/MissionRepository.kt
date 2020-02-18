package org.mozilla.rocket.msrp.data

import android.content.Context
import androidx.collection.LruCache
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.preference.stringLiveData
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.isSuccess
import org.mozilla.rocket.util.map
import org.mozilla.strictmodeviolator.StrictModeViolation

open class MissionRepository(
    appContext: Context,
    private val missionLocalDataSource: MissionLocalDataSource,
    private val missionRemoteDataSource: MissionRemoteDataSource
) {

    private val preference = StrictModeViolation.tempGrant({ builder ->
        builder.permitDiskReads()
    }, {
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    })

    private val couponCache = LruCache<String, MissionCoupon>(COUPON_CACHE_SIZE) // mission unique id -> coupon
    private val missionsLiveData: MutableLiveData<List<Mission>> by lazy {
        MutableLiveData<List<Mission>>().apply {
            if (!isMsrpAvailable()) {
                postValue(emptyList())
            }
        }
    }
    private val showContentHubClickOnboarding = SingleLiveEvent<String>()

    fun isMsrpAvailable(): Boolean = missionRemoteDataSource.isMsrpAvailable()

    fun getMissions(): LiveData<List<Mission>> = missionsLiveData

    suspend fun refreshMissions(userToken: String?): Result<List<Mission>, RewardServiceError> =
            missionRemoteDataSource.getMissions(userToken).also {
                if (it.isSuccess) {
                    missionsLiveData.postValue(it.data!!)
                } else if (it.error is RewardServiceError.AccountDisabled) {
                    // clear mission list once notified account got banned
                    missionsLiveData.postValue(emptyList())
                }
            }

    fun getReadMissionIdsLiveData(): LiveData<List<String>> =
            missionLocalDataSource.getReadMissionIdsLiveData()

    suspend fun addReadMissionId(missionId: String) {
        missionLocalDataSource.addReadMissionId(missionId)
    }

    suspend fun joinMission(userToken: String?, mission: Mission): Result<JoinMissionResult, RewardServiceError> =
            missionRemoteDataSource.joinMission(mission, userToken)

    suspend fun quitMission(userToken: String?, mission: Mission): Result<QuitMissionResult, RewardServiceError> =
            missionRemoteDataSource.quitMission(mission, userToken)

    suspend fun checkInMission(userToken: String?, ping: String): Result<List<Mission>, RewardServiceError> =
            missionRemoteDataSource.checkInMission(ping, userToken)

    suspend fun redeem(userToken: String?, missionId: String, redeemEndPoint: String): Result<RewardCouponDoc, RedeemServiceError> =
            missionRemoteDataSource.redeem(userToken, redeemEndPoint).also { result ->
                result.data?.let {
                    couponCache.put(missionId, MissionCoupon(it.code, it.open_link))
                }
            }

    suspend fun getCoupon(userToken: String?, missionId: String, redeemUrl: String): Result<MissionCoupon, RedeemServiceError> {
        val cachedCouponCode = couponCache[missionId]
        return if (cachedCouponCode != null) {
            Result.success(cachedCouponCode)
        } else {
            missionRemoteDataSource.redeem(userToken, redeemUrl).map {
                MissionCoupon(it.code, it.open_link).also { coupon ->
                    couponCache.put(missionId, coupon)
                }
            }
        }
    }

    fun hasJoinedAnyMissionBefore(): Boolean = missionLocalDataSource.hasJoinedAnyMissionBefore()

    fun setJoinedAnyMissionBefore() {
        missionLocalDataSource.setJoinedAnyMissionBefore()
    }

    fun requestContentHubClickOnboarding(couponName: String) {
        showContentHubClickOnboarding.value = couponName
    }

    fun getContentHubClickOnboardingEvent(): SingleLiveEvent<String> = showContentHubClickOnboarding

    fun getNotificationMission(): LiveData<Mission?> =
            missionsLiveData.map {
                val importantMission = it.firstOrNull(Mission::important)
                importantMission?.takeIf { mission -> mission.uniqueId != getLastReadNotificationId() }
            }

    private fun getLastReadNotificationId(): String? =
            preference.getString(SHARED_PREF_KEY_READ_NOTIFICATION_ID, null)

    fun getLastReadNotificationIdLiveData(): LiveData<String> =
            preference.stringLiveData(SHARED_PREF_KEY_READ_NOTIFICATION_ID, "")

    fun saveLastReadNotificationId(readId: String) {
        preference.edit().putString(SHARED_PREF_KEY_READ_NOTIFICATION_ID, readId).apply()
    }

    companion object {
        private const val PREF_NAME = "msrp_notification"
        private const val SHARED_PREF_KEY_READ_NOTIFICATION_ID = "shared_pref_key_read_notification_id"

        private const val COUPON_CACHE_SIZE = 10
    }
}

data class MissionCoupon(
    val couponCode: String,
    val websiteUrl: String
)
