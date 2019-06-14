/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.rocket.urlinput

import androidx.lifecycle.MutableLiveData
import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.R
import org.mozilla.focus.utils.IOUtils
import org.mozilla.focus.utils.TopSitesUtils
import org.mozilla.threadutils.ThreadUtils

object QuickSearchUtils {

    internal fun loadDefaultEngines(context: Context, liveData: MutableLiveData<List<QuickSearch>>) {
        loadEnginesFromAssets(context, R.raw.quick_search_engines_common, liveData)
    }

    internal fun loadEnginesByLocale(context: Context, liveData: MutableLiveData<List<QuickSearch>>) {
        loadEnginesFromAssets(context, R.raw.quick_search_engines, liveData)
    }

    private fun loadEnginesFromAssets(
        context: Context,
        resId: Int,
        liveData: MutableLiveData<List<QuickSearch>>
    ) {
        ThreadUtils.postToBackgroundThread {
            try {
                val jsonArray = IOUtils.readRawJsonArray(context, resId)
                val list = ArrayList<QuickSearch>()
                for (i in 0 until jsonArray.length()) {
                    val jsonObj = jsonArray.get(i) as JSONObject
                    val element = QuickSearch(
                            jsonObj.optString("name"),
                            TopSitesUtils.TOP_SITE_ASSET_PREFIX + jsonObj.optString("icon"),
                            jsonObj.optString("searchUrlPattern"),
                            jsonObj.optString("homeUrl"),
                            jsonObj.optString("urlPrefix"),
                            jsonObj.optString("urlSuffix"),
                            jsonObj.optBoolean("patternEncode"),
                            jsonObj.optBoolean("permitSpace", true)

                    )
                    list.add(element)
                }
                liveData.postValue(list)
            } catch (ex: JSONException) {
                throw AssertionError("Corrupt JSON asset ($resId)")
            }
        }
    }
}
