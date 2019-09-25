package org.mozilla.rocket.msrp.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_redeemed_mission.expiration_text
import kotlinx.android.synthetic.main.item_redeemed_mission.title
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class RedeemedMissionAdapterDelegate : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            RedeemedMissionsViewHolder(view)
}

class RedeemedMissionsViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as MissionUiModel.RedeemedMission

        title.text = uiModel.title
        expiration_text.text = uiModel.expirationTime
    }
}