/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.urlinput

import androidx.recyclerview.widget.DiffUtil

class QuickSearchDiffCallback : DiffUtil.ItemCallback<QuickSearch>() {

    override fun areItemsTheSame(oldItem: QuickSearch, newItem: QuickSearch): Boolean {
        return oldItem.name == newItem.name
    }

    override fun areContentsTheSame(oldItem: QuickSearch, newItem: QuickSearch): Boolean {
        return oldItem == newItem
    }
}