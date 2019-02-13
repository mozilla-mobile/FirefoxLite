/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
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