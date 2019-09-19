package org.mozilla.rocket.msrp.ui.adapter

import android.view.View
import kotlinx.android.synthetic.main.msrp_redeem_mission.*
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class RedeemAdapterDelegate : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            RedeemViewHolder(view)
}

class RedeemViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as RedeemUiModel

        redeem_title.text = uiModel.title
        redeem_description.text = uiModel.descriptionText
        redeem_btn.visibility = if (uiModel.showRedeemBtn) View.VISIBLE else View.INVISIBLE
    }
}

data class RedeemUiModel(
    val title: String,
    val descriptionText: String,
    val showRedeemBtn: Boolean
) : DelegateAdapter.UiModel()