package org.mozilla.rocket.home.data

import android.content.Context
import org.mozilla.focus.R
import org.mozilla.rocket.home.data.ContentPrefRepo.ContentPref.Browsing
import org.mozilla.rocket.home.data.ContentPrefRepo.ContentPref.Games
import org.mozilla.rocket.home.data.ContentPrefRepo.ContentPref.News
import org.mozilla.rocket.home.data.ContentPrefRepo.ContentPref.Shopping
import org.mozilla.strictmodeviolator.StrictModeViolation

class ContentPrefRepo(private val appContext: Context) {

    private val preference = StrictModeViolation.tempGrant({ builder ->
        builder.permitDiskReads()
    }, {
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    })

    fun getContentPref(): ContentPref =
            preference.getInt(SHARED_PREF_KEY_CONTENT_PREF, Browsing.id).toContentPref()

    fun setContentPref(contentPref: ContentPref) {
        preference.edit().putInt(SHARED_PREF_KEY_CONTENT_PREF, contentPref.id).apply()
    }

    sealed class ContentPref(val id: Int, val topSitesResId: Int) {
        object Browsing : ContentPref(0, R.raw.topsites_browsing)
        object Shopping : ContentPref(1, R.raw.topsites_shopping)
        object Games : ContentPref(2, R.raw.topsites_games)
        object News : ContentPref(3, R.raw.topsites_news)
    }

    private fun Int.toContentPref(): ContentPref = mapOf(
        Browsing.id to Browsing,
        Shopping.id to Shopping,
        Games.id to Games,
        News.id to News
    ).getOrElse(this) { error("Invalid content preference id") }

    companion object {
        private const val PREF_NAME = "content_pref"
        private const val SHARED_PREF_KEY_CONTENT_PREF = "shared_pref_key_content_pref"
    }
}