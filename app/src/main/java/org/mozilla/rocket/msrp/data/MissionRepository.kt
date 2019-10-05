package org.mozilla.rocket.msrp.data

import androidx.collection.LruCache
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.util.Result
import org.mozilla.rocket.util.get
import org.mozilla.rocket.util.isSuccess
import org.mozilla.rocket.util.map

open class MissionRepository(
    private val missionLocalDataSource: MissionLocalDataSource,
    private val missionRemoteDataSource: MissionRemoteDataSource
) {

    private val couponCache = LruCache<String, String>(COUPON_CACHE_SIZE) // mission unique id -> coupon code
    private val missionsLiveData = MutableLiveData<List<Mission>>()
    private val showContentHubClickOnboarding = SingleLiveEvent<Unit>()

    fun isMsrpAvailable(): Boolean = missionRemoteDataSource.isMsrpAvailable()

    fun getMissions(): LiveData<List<Mission>> = missionsLiveData

    suspend fun refreshMissions(userToken: String?): Result<List<Mission>, RewardServiceError> =
            missionRemoteDataSource.getMissions(userToken).also {
                if (it.isSuccess) {
                    missionsLiveData.postValue(it.data!!)
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
                result.data?.code?.let { couponCode ->
                    couponCache.put(missionId, couponCode)
                }
            }

    suspend fun getCoupon(userToken: String?, missionId: String, redeemUrl: String): Result<String, RedeemServiceError> {
        val cachedCouponCode = couponCache[missionId]
        return if (cachedCouponCode != null) {
            Result.success(cachedCouponCode)
        } else {
            missionRemoteDataSource.redeem(userToken, redeemUrl).map {
                val couponCode = it.code ?: return Result.error(error = RedeemServiceError.Failure("Coupon code is null"))
                couponCache.put(missionId, couponCode)
                couponCode
            }
        }
    }

    fun hasJoinedAnyMissionBefore(): Boolean = missionLocalDataSource.hasJoinedAnyMissionBefore()

    fun setJoinedAnyMissionBefore() {
        missionLocalDataSource.setJoinedAnyMissionBefore()
    }

    fun requestContentHubClickOnboarding() {
        showContentHubClickOnboarding.call()
    }

    fun getContentHubClickOnboardingEvent(): SingleLiveEvent<Unit> = showContentHubClickOnboarding

    companion object {
        private const val COUPON_CACHE_SIZE = 10
    }
}
