package org.mozilla.rocket.adapter

import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil

open class DelegatePagedListAdapter(
    private val delegatesManager: AdapterDelegatesManager,
    diffCallback: DiffUtil.ItemCallback<DelegateAdapter.UiModel>
) : PagedListAdapter<DelegateAdapter.UiModel, DelegateAdapter.ViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DelegateAdapter.ViewHolder =
            delegatesManager.onCreateViewHolder(parent, viewType)

    override fun getItemViewType(position: Int): Int {
        val item = getItem(position)
        return if (item != null) {
            delegatesManager.getItemViewType(item)
        } else {
            UNKNOWN_VIEW_TYPE
        }
    }

    override fun onBindViewHolder(holder: DelegateAdapter.ViewHolder, position: Int) {
        val item = getItem(position)
        if (item != null) {
            delegatesManager.onBindViewHolder(holder, item)
        }
    }

    companion object {
        private const val UNKNOWN_VIEW_TYPE = -999
    }
}