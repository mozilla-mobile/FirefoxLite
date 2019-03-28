/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.home.pinsite

import android.content.Context
import android.preference.PreferenceManager
import mozilla.components.support.base.log.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.history.model.Site
import org.mozilla.focus.utils.TopSitesUtils

internal class PinSiteManager(
    private val pinSiteDelegate: PinSiteDelegate
) : PinSiteDelegate by pinSiteDelegate

interface PinSiteDelegate {
    fun isEnabled(): Boolean
    fun isPinned(site: Site): Boolean
    fun pin(site: Site)
    fun unpinned(site: Site)
    fun getPinSites(): List<Site>
    fun isFirstTimeEnable(): Boolean
}

class SharedPreferencePinSiteDelegate(private val context: Context) : PinSiteDelegate {

    companion object {
        private const val TAG = "PinSiteManager"

        private const val PREF_NAME = "pin_sites"
        private const val KEY_JSON = "json"
        private const val KEY_FIRST_INIT = "first_init"

        // The number of pinned sites the new user will see
        private const val DEFAULT_NEW_USER_PIN_COUNT = 2

        private const val viewCountInterval = 100L

        private const val JSON_KEY_IS_ENABLED = "isEnabled"
        private const val JSON_KEY_PARTNER = "partner"

        fun resetPinSiteData(context: Context) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).apply {
                edit().putBoolean(KEY_FIRST_INIT, true).putString(KEY_JSON, "").apply()
            }
        }
    }

    private val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val sites = mutableListOf<Site>()

    private val rootNode: JSONObject
    private var isEnabled = false

    private val partnerList = mutableListOf<Site>()

    init {
        val jsonString = TopSitesUtils.loadDefaultSitesFromAssets(context, R.raw.pin_sites)
        this.rootNode = JSONObject(jsonString)
        this.isEnabled = isEnabled(rootNode)

        log("isEnable: $isEnabled")
        log("isFirstInit: ${isFirstInit()}")

        if (this.isEnabled && isFirstInit()) {
            val partnerSites = getPartnerList(rootNode)
            if (hasTopSiteRecord()) {
                log("init for update user")
                initForUpdateUser(partnerList, partnerSites)
            } else {
                log("init for new user")
                initForNewUser(partnerList, partnerSites)
            }
        } else {
            log("no initialization needed")
        }
    }

    override fun isEnabled(): Boolean {
        return isEnabled
    }

    override fun isPinned(site: Site): Boolean {
        return sites.any { it.id == site.id }
    }

    override fun pin(site: Site) {
        sites.add(0, Site(
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
        load(sites)
        return sites
    }

    override fun isFirstTimeEnable(): Boolean {
        return isFirstInit()
    }

    private fun getViewCountForPinSiteAt(index: Int): Long {
        return Long.MAX_VALUE - index * viewCountInterval
    }

    private fun save(sites: List<Site>) {
        sites.forEachIndexed { index, site ->
            site.viewCount = getViewCountForPinSiteAt(index)
        }
        val json = sitesToJson(sites)
        pref.edit().putString(KEY_JSON, json.toString()).apply()
        log("save")
    }

    private fun load(results: MutableList<Site>) {
        if (!this.isEnabled) {
            log("load - no enabled")
            return
        }

        log("load - enabled")
        results.clear()

        if (partnerList.isNotEmpty()) {
            results.addAll(0, partnerList)
            partnerList.clear()
            log("load partner list")
            save(results)
        } else {
            log("load saved pin site pref")
            loadSavedPinnedSite(results)
        }

        if (isFirstInit()) {
            log("init finished")
            onFirstInitComplete()
        }
    }

    private fun initForUpdateUser(results: MutableList<Site>, partnerSites: List<Site>) {
        results.addAll(partnerSites)
    }

    private fun initForNewUser(results: MutableList<Site>, partnerSites: List<Site>) {
        results.addAll(partnerSites)

        val defaultTopSiteJson = TopSitesUtils.loadDefaultSitesFromAssets(context, R.raw.topsites)
        val defaultTopSites = jsonToSites(JSONArray(defaultTopSiteJson), true).toMutableList()

        var remainPinCount = DEFAULT_NEW_USER_PIN_COUNT - partnerSites.size
        while (remainPinCount-- > 0 && defaultTopSites.isNotEmpty()) {
            results.add(defaultTopSites.removeAt(0))
        }

//        save(results)
//        onFirstInitComplete()
    }

    private fun loadSavedPinnedSite(results: MutableList<Site>) {
        pref.getString(KEY_JSON, "")?.let {
            results.addAll(jsonToSites(JSONArray(it), false))
        }
    }

    private fun isFirstInit(): Boolean {
        return pref.getBoolean(KEY_FIRST_INIT, true)
    }

    private fun onFirstInitComplete() {
        pref.edit().putBoolean(KEY_FIRST_INIT, false).apply()
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
                        faviconPrefix + obj.getString(TopSitesUtils.KEY_FAVICON)))
            }
        } catch (ignored: JSONException) {
            if (BuildConfig.DEBUG) {
                throw ignored
            }
        }

        return sites
    }

    private fun siteToJson(site: Site): JSONObject? {
        return try {
            val node = JSONObject()
            node.put(TopSitesUtils.KEY_ID, site.id)
            node.put(TopSitesUtils.KEY_URL, site.url)
            node.put(TopSitesUtils.KEY_TITLE, site.title)
            node.put(TopSitesUtils.KEY_FAVICON, site.favIconUri)
            node.put(TopSitesUtils.KEY_VIEW_COUNT, site.viewCount)
        } catch (e: JSONException) {
            if (BuildConfig.DEBUG) {
                throw e
            }
            null
        }
    }

    private fun hasTopSiteRecord(): Boolean {
        val defaultPref = PreferenceManager.getDefaultSharedPreferences(context)
        return defaultPref.getString("topsites_pref", "")?.isNotEmpty() ?: false
    }

    private fun isEnabled(rootNode: JSONObject): Boolean {
        return rootNode.getBoolean(JSON_KEY_IS_ENABLED)
    }

    private fun getPartnerList(rootNode: JSONObject): List<Site> {
        return jsonToSites(rootNode.getJSONArray(JSON_KEY_PARTNER), true)
    }

    private fun log(msg: String) {
        Log.log(tag = TAG, message = msg)
    }
}
