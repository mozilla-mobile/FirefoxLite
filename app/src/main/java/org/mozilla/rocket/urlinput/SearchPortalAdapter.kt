/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.urlinput

import android.os.StrictMode
import android.support.v7.recyclerview.extensions.ListAdapter
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.search_portal_item.view.search_portal_img
import org.mozilla.focus.R
import org.mozilla.icon.FavIconUtils

class SearchPortalAdapter(private val clickListener: (SearchPortal) -> Unit) : ListAdapter<SearchPortal, SearchPortalAdapter.PortalViewHolder>(SearchPortalDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, i: Int): PortalViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.search_portal_item, parent, false)
        return PortalViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: PortalViewHolder, i: Int) {
        viewHolder.bind(getItem(i), clickListener)
    }

    class PortalViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var icon: ImageView = itemView.search_portal_img

        fun bind(item: SearchPortal, clickListener: (SearchPortal) -> Unit) {
            val threadPolicy = StrictMode.allowThreadDiskWrites()
            val resource = FavIconUtils.getBitmapFromUri(itemView.context, item.icon)
            icon.setImageBitmap(resource)
            StrictMode.setThreadPolicy(threadPolicy)
            itemView.setOnClickListener { clickListener(item) }
        }
    }
}
