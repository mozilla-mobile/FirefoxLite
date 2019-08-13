package org.mozilla.focus.home.repository

import android.content.Context
import android.preference.PreferenceManager
import org.mozilla.focus.home.HomeFragment

open class TopSitesRepo(private val appContext: Context) {

    // open for mocking during testing
    open fun getDefaultTopSitesJsonString(): String? {
        return PreferenceManager.getDefaultSharedPreferences(appContext)
                .getString(HomeFragment.TOPSITES_PREF, null)
    }
}