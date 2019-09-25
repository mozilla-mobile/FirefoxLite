package org.mozilla.rocket.msrp.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_expired_mission.expiration_text
import kotlinx.android.synthetic.main.item_expired_mission.title
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class ExpiredMissionAdapterDelegate : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            ExpiredMissionsViewHolder(view)
}

class ExpiredMissionsViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as MissionUiModel.ExpiredMission

        title.text = uiModel.title
        expiration_text.text = uiModel.expirationText
    }
}