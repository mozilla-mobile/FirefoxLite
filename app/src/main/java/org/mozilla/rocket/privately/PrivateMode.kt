package org.mozilla.rocket.privately

import android.content.Context
import android.preference.PreferenceManager

class PrivateMode {
    companion object {
        const val PREF_KEY_PRIVATE_MODE_ENABLED = "pref_key_private_mode_enabled"

        private const val PREF_KEY_PRIVATE_MODE_ENABLED_DEFAULT = false

        fun isEnable(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_KEY_PRIVATE_MODE_ENABLED, PREF_KEY_PRIVATE_MODE_ENABLED_DEFAULT)
        }
    }
}