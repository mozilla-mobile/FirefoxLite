/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.home.topsites.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.history.model.Site
import org.mozilla.focus.utils.TopSitesUtils

class PinSiteManager(
    private val pinSiteDelegate: PinSiteDelegate
) : PinSiteDelegate by pinSiteDelegate

interface PinSiteDelegate {
    fun isPinned(site: Site): Boolean
    fun pin(site: Site)
    fun unpinned(site: Site)
    fun getPinSites(): List<Site>
}

class SharedPreferencePinSiteDelegate(private val context: Context) : PinSiteDelegate {

    companion object {
        private const val TAG = "PinSiteManager"

        private const val PREF_NAME = "pin_sites"
        private const val KEY_STRING_JSON = "json"

        private const val PINNED_SITE_VIEW_COUNT_INTERVAL = 100L
    }

    private val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val sites = mutableListOf<Site>()

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
            ).also { it.isDefault = site.isDefault }
        )
        save(sites)
    }

    override fun unpinned(site: Site) {
        sites.removeAll { it.id == site.id }
        save(sites)
    }

    override fun getPinSites(): List<Site> {
        load(sites)
        return sites
    }

    private fun getViewCountForPinSiteAt(index: Int): Long {
        return Long.MAX_VALUE - index * PINNED_SITE_VIEW_COUNT_INTERVAL
    }

    private fun save(sites: List<Site>) {
        sites.forEachIndexed { index, site ->
            site.viewCount = getViewCountForPinSiteAt(index)
        }
        val json = sitesToJson(sites)
        pref.edit().putString(KEY_STRING_JSON, json.toString()).apply()
        log("save")
    }

    private fun load(results: MutableList<Site>) {
        results.clear()
        log("load saved pin site pref")
        loadSavedPinnedSite(results)
    }

    private fun loadSavedPinnedSite(results: MutableList<Site>) {
        val jsonString = pref.getString(KEY_STRING_JSON, "")
        try {
            results.addAll(jsonToSites(JSONArray(jsonString), false))
        } catch (ignored: JSONException) {
        }
    }

    private fun sitesToJson(sites: List<Site>): JSONArray {
        val array = JSONArray()
        for (i in sites.indices) {
            val site = sites[i]
            val jsonSite = siteToJson(site)
            if (jsonSite != null) {
                array.put(jsonSite)
            }
        }
        return array
    }

    private fun jsonToSites(array: JSONArray, isDefaultTopSite: Boolean): List<Site> {
        val sites = ArrayList<Site>()
        val faviconPrefix = if (isDefaultTopSite) {
            TopSitesUtils.TOP_SITE_ASSET_PREFIX
        } else {
            ""
        }
        try {
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                sites.add(Site(obj.getLong(TopSitesUtils.KEY_ID),
                        obj.getString(TopSitesUtils.KEY_TITLE),
                        obj.getString(TopSitesUtils.KEY_URL),
                        obj.getLong(TopSitesUtils.KEY_VIEW_COUNT),
                        0,
                        faviconPrefix + getFaviconUrl(obj)
                    ).also { it.isDefault = obj.optBoolean(TopSitesUtils.KEY_IS_DEFAULT, false) }
                )
            }
        } catch (ignored: JSONException) {
        }

        return sites
    }

    private fun getFaviconUrl(json: JSONObject): String {
        return json.optString(TopSitesUtils.KEY_FAVICON)
    }

    private fun siteToJson(site: Site): JSONObject? {
        return try {
            val node = JSONObject()
            node.put(TopSitesUtils.KEY_ID, site.id)
            node.put(TopSitesUtils.KEY_URL, site.url)
            node.put(TopSitesUtils.KEY_TITLE, site.title)
            node.put(TopSitesUtils.KEY_FAVICON, site.favIconUri)
            node.put(TopSitesUtils.KEY_VIEW_COUNT, site.viewCount)
            node.put(TopSitesUtils.KEY_IS_DEFAULT, site.isDefault)
        } catch (e: JSONException) {
            null
        }
    }

    @SuppressLint("LogUsage")
    private fun log(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, msg)
        }
    }
}
