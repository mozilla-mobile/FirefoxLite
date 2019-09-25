package org.mozilla.rocket.msrp.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_redeemable_mission.title
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class RedeemableMissionAdapterDelegate : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            RedeemableMissionsViewHolder(view)
}

class RedeemableMissionsViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as MissionUiModel.RedeemableMission

        title.text = uiModel.title
    }
}