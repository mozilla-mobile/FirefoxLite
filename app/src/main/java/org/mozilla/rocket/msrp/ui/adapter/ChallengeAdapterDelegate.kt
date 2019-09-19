package org.mozilla.rocket.msrp.ui.adapter

import android.view.View
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.msrp_challenge_mission.*
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class ChallengeAdapterDelegate : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder =
            ChallengeViewHolder(view)
}

class ChallengeViewHolder(override val containerView: View) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {
        uiModel as ChallengeUiModel

        challenge_title.text = uiModel.title
        challenge_expiration_text.text = uiModel.expirationText
        challenge_progress.progress = uiModel.progress
        challenge_percentage_text.text = uiModel.progress.toString() + "%"

        Glide.with(containerView.context)
                .load(uiModel.imageUrl)
                .apply(requestOptions)
                .into(challenge_image)
    }

    companion object {
        var requestOptions = RequestOptions().apply { transforms(CenterCrop(), RoundedCorners(16)) }
    }
}

data class ChallengeUiModel(
    val title: String,
    val expirationText: String,
    val showRedDot: Boolean,
    val imageUrl: String,
    val progress: Int
) : DelegateAdapter.UiModel()