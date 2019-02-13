package org.mozilla.rocket.urlinput

import android.support.v7.util.DiffUtil

class SearchPortalDiffCallback : DiffUtil.ItemCallback<SearchPortal>() {

    override fun areItemsTheSame(oldItem: SearchPortal, newItem: SearchPortal): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: SearchPortal, newItem: SearchPortal): Boolean {
        return oldItem == newItem
    }
}