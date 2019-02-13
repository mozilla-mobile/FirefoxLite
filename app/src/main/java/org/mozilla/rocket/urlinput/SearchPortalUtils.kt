package org.mozilla.rocket.urlinput

import android.arch.lifecycle.MutableLiveData
import android.content.Context
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.focus.locale.Locales
import org.mozilla.focus.utils.IOUtils
import org.mozilla.focus.utils.TopSitesUtils
import org.mozilla.threadutils.ThreadUtils
import java.util.Arrays
import java.util.Locale

object SearchPortalUtils {

    private const val ASSET_PATH = "quicksearch/%s"
    private const val ASSET_FILE_NAME = "/portals.json"
    private const val ASSET_COMMON = "common"
    private const val DEFAULT_LOCALE = "id"

    internal fun loadDefaultPortals(context: Context, liveData: MutableLiveData<List<SearchPortal>>) {
        val path = String.format(ASSET_PATH, ASSET_COMMON) + ASSET_FILE_NAME
        loadPortalsFromAssets(context, path, liveData)
    }

    internal fun loadPortalsByLocale(context: Context, liveData: MutableLiveData<List<SearchPortal>>) {
        var path: String
        val locale = Locale.getDefault()
        val filesLocale = Arrays.asList(*context.assets.list(String.format(ASSET_PATH, Locales.getLanguageTag(locale))))
        val filesLanguage = Arrays.asList(*context.assets.list(String.format(ASSET_PATH, Locales.getLanguage(locale))))
        if (!filesLocale.isEmpty()) {
            path = String.format(ASSET_PATH, Locales.getLanguageTag(locale))
        } else if (!filesLanguage.isEmpty()) {
            path = String.format(ASSET_PATH, Locales.getLanguage(locale))
        } else {
            // TODO: confirm if we need to set default locale here
            path = String.format(ASSET_PATH, DEFAULT_LOCALE)
        }

        // Append asset file name
        path += ASSET_FILE_NAME

        loadPortalsFromAssets(context, path, liveData)
    }

    private fun loadPortalsFromAssets(context: Context, path: String, liveData: MutableLiveData<List<SearchPortal>>) {
        ThreadUtils.postToBackgroundThread {
            try {
                val jsonArray = IOUtils.readAssetFromJsonArray(context, path)
                val list = ArrayList<SearchPortal>()
                for (i in 0 until jsonArray.length()) {
                    val jsonObj = jsonArray.get(i) as JSONObject
                    list.add(SearchPortal(jsonObj.optString("name"), TopSitesUtils.TOP_SITE_ASSET_PREFIX + jsonObj.optString("icon"), jsonObj.optString("searchUrlPattern"), jsonObj.optString("homeUrl")))
                }
                liveData.postValue(list)
            } catch (ex: JSONException) {
                throw AssertionError("Corrupt JSON asset ($path)")
            }
        }
    }
}
