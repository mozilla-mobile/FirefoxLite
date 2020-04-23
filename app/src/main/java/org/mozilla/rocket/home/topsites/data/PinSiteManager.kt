/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.home.topsites.data

import android.annotation.SuppressLint
import android.content.Context
import android.preference.PreferenceManager
import android.util.Log
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.history.model.Site
import org.mozilla.focus.utils.TopSitesUtils
import org.mozilla.rocket.abtesting.LocalAbTesting
import org.mozilla.rocket.abtesting.LocalAbTesting.checkAssignedBucket
import org.mozilla.rocket.abtesting.LocalAbTesting.isExperimentEnabled
import org.mozilla.rocket.home.topsites.domain.GetTopSitesAbTestingUseCase
import org.mozilla.rocket.util.AssetsUtils
import org.mozilla.rocket.util.getJsonArray

class PinSiteManager(
    private val pinSiteDelegate: PinSiteDelegate
) : PinSiteDelegate by pinSiteDelegate

interface PinSiteDelegate {
    fun isEnabled(): Boolean
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
        private const val KEY_BOOLEAN_FIRST_INIT = "first_init"

        // The number of pinned sites the new user will see
        private const val DEFAULT_NEW_USER_PIN_COUNT = 0

        private const val PINNED_SITE_VIEW_COUNT_INTERVAL = 100L

        private const val JSON_KEY_BOOLEAN_IS_ENABLED = "isEnabled"
        private const val JSON_KEY_STRING_PARTNER = "partner"

        fun resetPinSiteData(context: Context) {
            context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE).apply {
                edit().putBoolean(KEY_BOOLEAN_FIRST_INIT, true).putString(KEY_STRING_JSON, "").apply()
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

        if (this.isEnabled) {
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
        if (!this.isEnabled) {
            log("load - not enabled")
            return
        }

        log("load - enabled")
        results.clear()

        val isFirstInit = isFirstInit()
        // TODO: Remove after top site AB testing finished
        val bucket = LocalAbTesting.checkAssignedBucket(GetTopSitesAbTestingUseCase.AB_TESTING_EXPERIMENT_NAME_TOP_SITES)
        if (isFirstInit && bucket != null) {
            val sites = getAbTestingSites() ?: emptyList()
            val fixedSiteCount = GetTopSitesAbTestingUseCase.getFixedSiteCount(bucket)
            val defaultPinCount = GetTopSitesAbTestingUseCase.getDefaultPinCount(bucket)
            results.addAll(0, sites.subList(fixedSiteCount, fixedSiteCount + defaultPinCount))
            log("load top site abtesting partner list")
            save(results)
        } else if (isFirstInit && partnerList.isNotEmpty()) {
            results.addAll(0, partnerList)
            log("load partner list")
            save(results)
        } else {
            log("load saved pin site pref")
            loadSavedPinnedSite(results)
        }

        if (isFirstInit) {
            log("init finished")
            onFirstInitComplete()
        }
    }

    // TODO: Remove after top site AB testing finished
    private fun getAbTestingSites(): List<Site>? =
            AssetsUtils.loadStringFromRawResource(context, R.raw.abtesting_topsites)
                    ?.jsonStringToSites()

    // TODO: Remove after top site AB testing finished
    private fun String.jsonStringToSites(): List<Site>? {
        return try {
            this.getJsonArray { TopSitesUtils.paresSite(it) }
        } catch (e: JSONException) {
            e.printStackTrace()
            null
        }
    }

    private fun initForUpdateUser(results: MutableList<Site>, partnerSites: List<Site>) {
        results.addAll(partnerSites)
    }

    private fun initForNewUser(results: MutableList<Site>, partnerSites: List<Site>) {
        results.addAll(partnerSites)

        // TODO: Remove after top site AB testing finished
        val defaultTopSiteJson = if (isExperimentEnabled(GetTopSitesAbTestingUseCase.AB_TESTING_EXPERIMENT_NAME_TOP_SITES) &&
                checkAssignedBucket(GetTopSitesAbTestingUseCase.AB_TESTING_EXPERIMENT_NAME_TOP_SITES) != null) {
            TopSitesUtils.loadDefaultSitesFromAssets(context, R.raw.abtesting_topsites)
        } else {
            TopSitesUtils.loadDefaultSitesFromAssets(context, R.raw.topsites)
        }
        val defaultTopSites = jsonToSites(JSONArray(defaultTopSiteJson), true).toMutableList()

        var remainPinCount = DEFAULT_NEW_USER_PIN_COUNT - partnerSites.size
        while (remainPinCount-- > 0 && defaultTopSites.isNotEmpty()) {
            results.add(defaultTopSites.removeAt(0))
        }
    }

    private fun loadSavedPinnedSite(results: MutableList<Site>) {
        val jsonString = pref.getString(KEY_STRING_JSON, "")
        try {
            results.addAll(jsonToSites(JSONArray(jsonString), false))
        } catch (ignored: JSONException) {
        }
    }

    private fun isFirstInit(): Boolean {
        return pref.getBoolean(KEY_BOOLEAN_FIRST_INIT, true)
    }

    private fun onFirstInitComplete() {
        pref.edit().putBoolean(KEY_BOOLEAN_FIRST_INIT, false).apply()
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
                        faviconPrefix + getFaviconUrl(obj)))
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
        } catch (e: JSONException) {
            null
        }
    }

    private fun hasTopSiteRecord(): Boolean {
        val defaultPref = PreferenceManager.getDefaultSharedPreferences(context)
        return defaultPref.getString(TopSitesRepo.TOP_SITES_PREF, "")?.isNotEmpty() ?: false
    }

    private fun isEnabled(rootNode: JSONObject): Boolean {
        return rootNode.getBoolean(JSON_KEY_BOOLEAN_IS_ENABLED)
    }

    private fun getPartnerList(rootNode: JSONObject): List<Site> {
        return jsonToSites(rootNode.getJSONArray(JSON_KEY_STRING_PARTNER), true)
    }

    @SuppressLint("LogUsage")
    private fun log(msg: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, msg)
        }
    }
}
