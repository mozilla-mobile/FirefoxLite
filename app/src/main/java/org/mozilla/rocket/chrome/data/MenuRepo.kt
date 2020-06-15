package org.mozilla.rocket.chrome.data

import android.content.Context
import org.mozilla.strictmodeviolator.StrictModeViolation

class MenuRepo(appContext: Context) {

    private val preference = StrictModeViolation.tempGrant({ builder ->
        builder.permitDiskReads()
    }, {
        appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    })

    fun getReadMenuItemVersion(): Int =
            preference.getInt(SHARED_PREF_KEY_MENU_NEW_ITEM_VERSION, -1)

    fun saveReadMenuItemVersion(version: Int) {
        preference.edit().putString(SHARED_PREF_KEY_MENU_NEW_ITEM_VERSION, version.toString()).apply()
    }

    companion object {
        private const val PREF_NAME = "menu"
        private const val SHARED_PREF_KEY_MENU_NEW_ITEM_VERSION = "shared_pref_key_menu_new_item_version"

        const val MENU_ITEM_VERSION = 0
    }
}