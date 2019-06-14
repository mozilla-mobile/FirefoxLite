/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.urlinput

import android.os.StrictMode
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import kotlinx.android.synthetic.main.quick_search_item.view.quick_search_img
import org.mozilla.focus.R
import org.mozilla.icon.FavIconUtils
import org.mozilla.strictmodeviolator.StrictModeViolation

class QuickSearchAdapter(private val clickListener: (QuickSearch) -> Unit) : ListAdapter<QuickSearch, QuickSearchAdapter.EngineViewHolder>(QuickSearchDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, i: Int): EngineViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.quick_search_item, parent, false)
        return EngineViewHolder(itemView)
    }

    override fun onBindViewHolder(viewHolder: EngineViewHolder, i: Int) {
        viewHolder.bind(getItem(i), clickListener)
    }

    class EngineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var icon: ImageView = itemView.quick_search_img

        fun bind(item: QuickSearch, clickListener: (QuickSearch) -> Unit) {
            StrictModeViolation.tempGrant({ obj: StrictMode.ThreadPolicy.Builder -> obj.permitDiskWrites() }) {
                val resource = FavIconUtils.getBitmapFromUri(itemView.context, item.icon)
                icon.setImageBitmap(resource)
            }
            itemView.setOnClickListener { clickListener(item) }
        }
    }
}
