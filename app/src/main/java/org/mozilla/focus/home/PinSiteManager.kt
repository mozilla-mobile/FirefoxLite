/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import org.mozilla.focus.history.model.Site

internal class PinSiteManager(private val model: TopSitesContract.Model) {
    companion object {
        const val pinSiteViewCountInterval = 100
    }

    private val pinSiteCount: Int
        get() {
            var count = 0
            for (site in model.sites) {
                if (isPinSite(site)) {
                    count++
                }
            }
            return count
        }

    fun isPinSite(site: Site): Boolean {
        return site.viewCount >= Long.MAX_VALUE - pinSiteViewCountInterval * HomeFragment.TOP_SITES_QUERY_LIMIT
    }

    fun pinSite(site: Site) {
        val pinCount = pinSiteCount
        val nextViewCount = Long.MAX_VALUE - pinCount * pinSiteViewCountInterval
        site.viewCount = nextViewCount
    }
}
