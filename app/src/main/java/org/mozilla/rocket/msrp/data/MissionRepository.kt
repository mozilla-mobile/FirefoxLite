package org.mozilla.rocket.msrp.data

import androidx.lifecycle.LiveData
import org.mozilla.rocket.util.Result

open class MissionRepository(
    private val missionLocalDataSource: MissionLocalDataSource,
    private val missionRemoteDataSource: MissionRemoteDataSource
) {

    fun isMsrpAvailable(): Boolean = missionRemoteDataSource.isMsrpAvailable()

    suspend fun getMissions(userToken: String?): Result<List<Mission>, RewardServiceError> =
            missionRemoteDataSource.getMissions(userToken)

    fun getReadMissionIdsLiveData(): LiveData<List<String>> =
            missionLocalDataSource.getReadMissionIdsLiveData()

    suspend fun addReadMissionId(missionId: String) {
        missionLocalDataSource.addReadMissionId(missionId)
    }

    suspend fun redeem(userToken: String?, redeemUrl: String): Result<RewardCouponDoc, RedeemServiceError> =
            missionRemoteDataSource.redeem(userToken, redeemUrl)
}
