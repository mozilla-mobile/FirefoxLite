package org.mozilla.rocket.msrp.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.item_redeemable_mission.expiration_text
import kotlinx.android.synthetic.main.item_redeemable_mission.title
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.msrp.ui.MissionViewModel

class RedeemableMissionAdapterDelegate(
    private val missionViewModel: MissionViewModel
) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            RedeemableMissionsViewHolder(missionViewModel, view)
}

class RedeemableMissionsViewHolder(
    private val missionViewModel: MissionViewModel,
    override val containerView: View
) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as MissionUiModel.RedeemableMission

        title.text = uiModel.title
        expiration_text.text = itemView.resources.getString(R.string.msrp_voucher_expire, uiModel.expirationTime)

        itemView.setOnClickListener {
            missionViewModel.onRedeemItemClicked(adapterPosition)
        }
    }
}