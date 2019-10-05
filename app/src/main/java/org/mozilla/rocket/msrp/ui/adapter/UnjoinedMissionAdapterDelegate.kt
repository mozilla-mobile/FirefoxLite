package org.mozilla.rocket.msrp.ui.adapter

import android.view.View
import androidx.core.view.isVisible
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.item_unjoined_mission.expiration_text
import kotlinx.android.synthetic.main.item_unjoined_mission.image
import kotlinx.android.synthetic.main.item_unjoined_mission.red_dot
import kotlinx.android.synthetic.main.item_unjoined_mission.title
import org.mozilla.focus.R
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.extension.dpToPx
import org.mozilla.rocket.msrp.ui.MissionViewModel

class UnjoinedMissionsAdapterDelegate(
    private val missionViewModel: MissionViewModel
) : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            UnjoinedMissionsViewHolder(missionViewModel, view)
}

class UnjoinedMissionsViewHolder(
    private val missionViewModel: MissionViewModel,
    override val containerView: View
) : DelegateAdapter.ViewHolder(containerView) {

    private val imgReqOpts = RequestOptions().apply { transforms(CenterCrop(), RoundedCorners(containerView.dpToPx(4f))) }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as MissionUiModel.UnjoinedMission

        title.text = uiModel.title
        expiration_text.text = itemView.resources.getString(R.string.msrp_reward_challenge_expire, uiModel.expirationTime)
        red_dot.isVisible = uiModel.showRedDot

        Glide.with(containerView.context)
                .load(uiModel.imageUrl)
                .apply(imgReqOpts)
                .into(image)

        itemView.setOnClickListener {
            missionViewModel.onChallengeItemClicked(adapterPosition)
        }
    }
}