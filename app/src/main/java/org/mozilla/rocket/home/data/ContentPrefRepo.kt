package org.mozilla.rocket.home.data

import android.content.Context
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

    sealed class ContentPref(val id: Int) {
        object Browsing : ContentPref(0)
        object Shopping : ContentPref(1)
        object Games : ContentPref(2)
        object News : ContentPref(3)
    }

    private fun Int.toContentPref(): ContentPref = mapOf(
        Browsing.id to Browsing,
        Shopping.id to Shopping,
        Games.id to Games,
        News.id to News
    ).getOrElse(this) { Browsing }

    companion object {
        private const val PREF_NAME = "content_pref"
        private const val SHARED_PREF_KEY_CONTENT_PREF = "shared_pref_key_content_pref"
    }
}