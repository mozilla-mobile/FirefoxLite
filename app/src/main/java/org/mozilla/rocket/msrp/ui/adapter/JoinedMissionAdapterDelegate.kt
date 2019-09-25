package org.mozilla.rocket.msrp.ui.adapter

import android.annotation.SuppressLint
import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.item_joined_mission.expiration_text
import kotlinx.android.synthetic.main.item_joined_mission.image
import kotlinx.android.synthetic.main.item_joined_mission.progress
import kotlinx.android.synthetic.main.item_joined_mission.progress_text
import kotlinx.android.synthetic.main.item_joined_mission.title
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.extension.dpToPx

class JoinedMissionsAdapterDelegate : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            JoinedMissionsViewHolder(view)
}

class JoinedMissionsViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {

    private val imgReqOpts = RequestOptions().apply { transforms(CenterCrop(), RoundedCorners(containerView.dpToPx(4f))) }

    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as MissionUiModel.JoinedMission

        title.text = uiModel.title
        expiration_text.text = uiModel.expirationText
        progress.progress = uiModel.progress
        @SuppressLint("SetTextI18n")
        progress_text.text = "${uiModel.progress}%"

        Glide.with(containerView.context)
                .load(uiModel.imageUrl)
                .apply(imgReqOpts)
                .into(image)
    }
}