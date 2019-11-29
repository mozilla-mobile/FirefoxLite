package org.mozilla.rocket.content.travel.ui.adapter

import android.view.View
import org.mozilla.rocket.adapter.AdapterDelegate
import org.mozilla.rocket.adapter.DelegateAdapter

class ExploreLoadingAdapterDelegate() : AdapterDelegate {
    override fun onCreateViewHolder(view: View): DelegateAdapter.ViewHolder = ExploreLoadingViewHolder(view)
}

class ExploreLoadingViewHolder(
    override val containerView: View
) : DelegateAdapter.ViewHolder(containerView) {
    override fun bind(uiModel: DelegateAdapter.UiModel) {}
}

class LoadingUiModel : DelegateAdapter.UiModel()
