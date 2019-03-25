/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.home

import android.content.Context
import org.mozilla.focus.history.model.Site
import org.mozilla.focus.utils.TopSitesUtils

internal class PinSiteManager(
        private val pinSiteDelegate: PinSiteDelegate
) : PinSiteDelegate by pinSiteDelegate

interface PinSiteDelegate {
    fun isPinned(site: Site): Boolean
    fun pin(site: Site)
    fun unpinned(site: Site)
    fun getPinSites() : List<Site>
}

class SharedPreferencePinSiteDelegate(context: Context) : PinSiteDelegate {

    companion object {
        private const val PREF_NAME = "pin_sites"
        private const val KEY_JSON = "json"

        private const val viewCountInterval = 100L
    }
    private val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val sites = mutableListOf<Site>()

    init {
        load(sites)
    }

    override fun isPinned(site: Site): Boolean {
        return sites.any { it.id == site.id }
    }

    override fun pin(site: Site) {
        sites.add(Site(
                site.id,
                site.title,
                site.url,
                site.viewCount,
                site.lastViewTimestamp,
                site.favIconUri
        ))
        save(sites)
    }

    override fun unpinned(site: Site) {
        sites.removeAll { it.id == site.id }
        save(sites)
    }

    override fun getPinSites(): List<Site> {
        return sites
    }

    private fun getViewCountForPinSiteAt(index: Int): Long {
        return Long.MAX_VALUE - index * viewCountInterval
    }

    private fun save(sites: List<Site>) {
        sites.forEachIndexed { index, site ->
            site.viewCount = getViewCountForPinSiteAt(index)
        }
        val json = TopSitesUtils.sitesToJson(sites)
        pref.edit().putString(KEY_JSON, json.toString()).apply()
    }

    private fun load(results: MutableList<Site>) {
        results.clear()

        pref.getString(KEY_JSON, "")?.let {
            results.addAll(TopSitesUtils.jsonToSites(it))
        }
    }
}

