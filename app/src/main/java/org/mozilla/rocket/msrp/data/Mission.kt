package org.mozilla.rocket.msrp.data

data class Mission(
    val mid: String,
    val missionType: String,

    val title: String,
    val description: String,
    val imageUrl: String,

    val endpoint: String, // Use this API to request to join the mission
    val redeem: String?, // Use this API to retrieve the reward

    val events: List<String>,
    val important: Boolean,
    val minVersion: Int,

    val joinEndDate: Long,
    val expiredDate: Long,
    val redeemedDate: Long,

    val status: Int, // STATUS_NEW, STATUS_NEW, STATUS_REDEEMABLE, STATUS_REDEEMED
    val missionProgress: MissionProgress?
) {
    var unread = false

    companion object {
        const val STATUS_NEW = 0
        const val STATUS_JOINED = 1
        const val STATUS_REDEEMABLE = 2
        const val STATUS_REDEEMED = 3
    }
}

sealed class MissionProgress {
    data class TypeDaily(
        val joinDate: Long, // the date the user join this mission
        val currentDay: Int, // number of the total days accomplished
        val totalDays: Int, // number of the total days needed
        val message: String
    ) : MissionProgress()
}