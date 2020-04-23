package org.mozilla.rocket.home.topsites.data

import android.content.Context
import android.database.Cursor
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.preference.PreferenceManager
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject
import org.mozilla.fileutils.FileUtils
import org.mozilla.focus.R
import org.mozilla.focus.history.BrowsingHistoryManager
import org.mozilla.focus.history.model.Site
import org.mozilla.focus.provider.HistoryContract
import org.mozilla.focus.provider.HistoryDatabaseHelper
import org.mozilla.focus.provider.QueryHandler
import org.mozilla.focus.utils.DimenUtils
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.focus.utils.TopSitesUtils
import org.mozilla.icon.FavIconUtils
import org.mozilla.rocket.abtesting.LocalAbTesting
import org.mozilla.rocket.home.topsites.domain.GetTopSitesAbTestingUseCase
import org.mozilla.rocket.persistance.History.HistoryDatabase
import org.mozilla.rocket.util.AssetsUtils
import org.mozilla.rocket.util.toJsonArray
import org.mozilla.rocket.util.getJsonArray
import java.util.ArrayList
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TopSitesRepo(
    private val appContext: Context,
    private val pinSiteManager: PinSiteManager
) {

    private var needToCheckDbVersion = true

    fun getConfiguredFixedSites(): List<Site>? =
            FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_TOP_SITES_FIXED_ITEMS)
                    .takeIf { it.isNotEmpty() }
                    ?.jsonStringToSites()

    fun getDefaultFixedSites(): List<Site>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.fixedsites)
                    ?.jsonStringToSites()

    fun getPinnedSites(): List<Site> = pinSiteManager.getPinSites()

    fun getAbTestingSites(): List<Site>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.abtesting_topsites)
                    ?.jsonStringToSites()

    suspend fun getHistorySites(): List<Site> {
        return if (needToCheckDbVersion) {
            needToCheckDbVersion = false
            val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(appContext)
            if (sharedPreferences.contains(TOP_SITES_V2_PREF)) {
                queryHistorySites()
            } else {
                migrateHistoryDb()
                queryHistorySites()
            }
        } else {
            queryHistorySites()
        }
    }

    private suspend fun queryHistorySites(): List<Site> = suspendCoroutine { continuation ->
        BrowsingHistoryManager.getInstance()
                .queryTopSites(TOP_SITES_QUERY_LIMIT, TOP_SITES_QUERY_MIN_VIEW_COUNT) {
                    continuation.resume(it.filterIsInstance<Site>())
                }
    }

    private suspend fun migrateHistoryDb() = suspendCoroutine<Unit> { continuation ->
        Thread(MigrateHistoryRunnable(object : Handler(Looper.getMainLooper()) {
            override fun handleMessage(msg: Message) {
                if (msg.what == MSG_ID_REFRESH) {
                    continuation.resume(Unit)
                }
            }
        }, appContext)).start()
    }

    fun getChangedDefaultSites(): List<Site>? = getDefaultTopSitesJsonString()
                    ?.jsonStringToSites()
                    ?.apply { forEach { it.isDefault = true } }

    fun getConfiguredDefaultSites(): List<Site>? =
            FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_TOP_SITES_DEFAULT_ITEMS)
                    .takeIf { it.isNotEmpty() }
                    ?.jsonStringToSites()
                    ?.apply { forEach { it.isDefault = true } }

    fun getDefaultSites(): List<Site>? =
            AssetsUtils.loadStringFromRawResource(appContext, R.raw.topsites)
                    ?.jsonStringToSites()
                    ?.apply { forEach { it.isDefault = true } }

    private fun getDefaultTopSitesJsonString(): String? {
        return PreferenceManager.getDefaultSharedPreferences(appContext)
                .getString(TOP_SITES_PREF, null)
    }

    fun isPinEnabled(): Boolean = pinSiteManager.isEnabled()

    fun pin(site: Site) {
        pinSiteManager.pin(site)
    }

    suspend fun remove(site: Site) {
        pinSiteManager.unpinned(site)
        if (site.isDefault) {
            removeDefaultSite(site)
        }
        withContext(Dispatchers.IO) {
            updateTopSiteToDb(site.apply { viewCount = 1 })
        }
    }

    suspend fun removeAbTesting(site: Site) {
        pinSiteManager.unpinned(site)
        if (site.isDefault) {
            removeDefaultSiteAbTesting(site)
        }
        withContext(Dispatchers.IO) {
            updateTopSiteToDb(site.apply { viewCount = 1 })
        }
    }

    private suspend fun updateTopSiteToDb(site: Site) {
        suspendCoroutine<Unit> { continuation ->
            BrowsingHistoryManager.getInstance().updateLastEntry(site) { continuation.resume(Unit) }
        }
    }

    fun removeDefaultSite(site: Site) {
        val defaultSitesString = getDefaultTopSitesJsonString()
                ?: FirebaseHelper.getFirebase().getRcString(FirebaseHelper.STR_TOP_SITES_DEFAULT_ITEMS).takeIf { it.isNotEmpty() }
                ?: AssetsUtils.loadStringFromRawResource(appContext, R.raw.topsites)
        val defaultSitesJsonArray = defaultSitesString?.toJsonArray()
        if (defaultSitesJsonArray != null) {
            try {
                defaultSitesJsonArray.apply {
                    for (i in 0 until this.length()) {
                        val jsonObject = this.get(i) as JSONObject
                        if (site.id == jsonObject.getLong("id")) {
                            this.remove(i)
                            break
                        }
                    }
                }
                TopSitesUtils.saveDefaultSites(appContext, defaultSitesJsonArray)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    // TODO: Remove after top site AB testing finished
    fun removeDefaultSiteAbTesting(site: Site) {
        val defaultSitesJsonArray = getDefaultTopSitesJsonString()?.toJsonArray()
                ?: AssetsUtils.loadStringFromRawResource(appContext, R.raw.abtesting_topsites)?.toJsonArray()?.apply {
                    val bucket = LocalAbTesting.checkAssignedBucket(GetTopSitesAbTestingUseCase.AB_TESTING_EXPERIMENT_NAME_TOP_SITES)
                    val fixedSiteCount = GetTopSitesAbTestingUseCase.getFixedSiteCount(bucket)
                    val defaultPinCount = GetTopSitesAbTestingUseCase.getDefaultPinCount(bucket)
                    repeat(fixedSiteCount + defaultPinCount) {
                        this.remove(0)
                    }
                }
        if (defaultSitesJsonArray != null) {
            try {
                defaultSitesJsonArray.apply {
                    for (i in 0 until this.length()) {
                        val jsonObject = this.get(i) as JSONObject
                        if (site.id == jsonObject.getLong("id")) {
                            this.remove(i)
                            break
                        }
                    }
                }
                TopSitesUtils.saveDefaultSites(appContext, defaultSitesJsonArray)
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }
    }

    fun isPinned(site: Site): Boolean = pinSiteManager.isPinned(site)

    private class MigrateHistoryRunnable(
        private val handler: Handler,
        private val appContext: Context
    ) : Runnable {

        override fun run() {
            val helper = HistoryDatabase.getInstance(appContext).openHelper
            val db = helper.writableDatabase
            // We can't differentiate if this is a new install or upgrade given the db version will
            // already become the latest version here. We create a temp table if no migration is
            // needed and later delete it to prevent crashing.
            db.execSQL(HistoryDatabase.CREATE_LEGACY_IF_NOT_EXIST)
            val builder = SupportSQLiteQueryBuilder.builder(HistoryDatabaseHelper.Tables.BROWSING_HISTORY_LEGACY)
            val columns = arrayOf(HistoryContract.BrowsingHistory._ID, HistoryContract.BrowsingHistory.URL, HistoryContract.BrowsingHistory.FAV_ICON)
            builder.columns(columns)
            val query = builder.create()
            val faviconFolder = FileUtils.getFaviconFolder(appContext)
            val urls = ArrayList<String>()
            val icons = ArrayList<ByteArray>()
            db.query(query).use { cursor ->
                if (cursor.moveToFirst()) {
                    parseCursorToSite(cursor, urls, icons)
                }
                while (cursor.moveToNext()) {
                    parseCursorToSite(cursor, urls, icons)
                }
            }
            if (icons.size == 0) {
                scheduleRefresh(handler)
            } else {
                // Refresh is still scheduled implicitly in SaveBitmapsTask
                FavIconUtils.SaveBitmapsTask(faviconFolder, urls, icons, UpdateHistoryWrapper(urls, handler),
                        Bitmap.CompressFormat.PNG, DimenUtils.PNG_QUALITY_DONT_CARE).execute()
            }
            db.execSQL("DROP TABLE " + HistoryDatabaseHelper.Tables.BROWSING_HISTORY_LEGACY)
            PreferenceManager.getDefaultSharedPreferences(appContext).edit().putBoolean(TOP_SITES_V2_PREF, true).apply()
        }

        private fun parseCursorToSite(cursor: Cursor, urls: MutableList<String>, icons: MutableList<ByteArray>) {
            val url = cursor.getString(cursor.getColumnIndex(HistoryContract.BrowsingHistory.URL))
            val icon = cursor.getBlob(cursor.getColumnIndex(HistoryContract.BrowsingHistory.FAV_ICON))
            urls.add(url)
            icons.add(icon)
        }

        private fun scheduleRefresh(handler: Handler) {
            val message = handler.obtainMessage(MSG_ID_REFRESH)
            handler.dispatchMessage(message)
        }
    }

    private class UpdateHistoryWrapper(
        private val urls: List<String>,
        private val handler: Handler
    ) : FavIconUtils.Consumer<List<String>> {

        override fun accept(fileUris: List<String>) {
            val listener = QueryHandler.AsyncUpdateListener {
                scheduleRefresh(handler)
            }
            for (i in fileUris.indices) {
                if (i == fileUris.size - 1) {
                    BrowsingHistoryManager.updateHistory(null, urls[i], fileUris[i], listener)
                } else {
                    BrowsingHistoryManager.updateHistory(null, urls[i], fileUris[i])
                }
            }
        }

        private fun scheduleRefresh(handler: Handler) {
            val message = handler.obtainMessage(MSG_ID_REFRESH)
            handler.dispatchMessage(message)
        }
    }

    companion object {
        const val TOP_SITES_PREF = "topsites_pref"
        const val TOP_SITES_QUERY_LIMIT = 16
        const val TOP_SITES_QUERY_MIN_VIEW_COUNT = 2
        private const val TOP_SITES_V2_PREF = "top_sites_v2_complete"
        private const val MSG_ID_REFRESH = 8269
    }
}

private fun String.jsonStringToSites(): List<Site>? {
    return try {
        this.getJsonArray { TopSitesUtils.paresSite(it) }
    } catch (e: JSONException) {
        e.printStackTrace()
        null
    }
}