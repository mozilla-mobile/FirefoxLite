package org.mozilla.rocket.msrp.ui.adapter

import org.mozilla.rocket.adapter.DelegateAdapter

sealed class MissionUiModel : DelegateAdapter.UiModel() {
    data class UnjoinedMission(
        val title: String,
        val expirationTime: String,
        val imageUrl: String,
        val showRedDot: Boolean
    ) : MissionUiModel()

    data class JoinedMission(
        val title: String,
        val expirationTime: String,
        val imageUrl: String,
        val progress: Int
    ) : MissionUiModel()

    data class RedeemableMission(
        val title: String,
        val expirationTime: String
    ) : MissionUiModel()

    data class RedeemedMission(
        val title: String,
        val expirationTime: String
    ) : MissionUiModel()

    data class ExpiredMission(
        val title: String
    ) : MissionUiModel()
}