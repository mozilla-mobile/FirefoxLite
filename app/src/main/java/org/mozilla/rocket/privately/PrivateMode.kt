package org.mozilla.rocket.privately

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.preference.PreferenceManager
import org.mozilla.focus.utils.FileUtils
import org.mozilla.focus.utils.ThreadUtils
import java.io.File


// Describe when to clear the private mode session
class PrivateMode {

    // Provide common resources, and helper functions
    companion object {
        const val PREF_KEY_PRIVATE_MODE_ENABLED = "pref_key_private_mode_enabled"
        const val PREF_KEY_SANITIZE_REMINDER = "pref_key_sanitize_reminder"

        const val INTENT_EXTRA_SANITIZE = "intent_extra_sanitize"
        const val PRIVATE_PROCESS_NAME = "private_mode"
        const val WEBVIEW_FOLDER_NAME = "webview"

        private const val PREF_KEY_PRIVATE_MODE_ENABLED_DEFAULT = false

        // Private Mode is currently behind a pref and is default off.
        // The option to enable it is on Nightly. The logic is in SettingsFragment.
        @JvmStatic
        fun isEnable(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_KEY_PRIVATE_MODE_ENABLED, PREF_KEY_PRIVATE_MODE_ENABLED_DEFAULT)
        }

        @JvmStatic
        fun sanitize(context: Context) {
            ThreadUtils.postToBackgroundThread {

                context.applicationContext.cacheDir?.let { dir ->
                    clean(dir, context)
                }

                context.applicationContext.getDir(PrivateMode.WEBVIEW_FOLDER_NAME, MODE_PRIVATE)?.let { dir ->
                    clean(dir, context)
                }
            }
        }

        private fun clean(dir: File, context: Context) {

            val delete = FileUtils.deleteDirectory(dir)
            if (!delete) {
                // TODO:remember to clear the  SANITIZE_REMINDER when the app launch next time
                PreferenceManager.getDefaultSharedPreferences(context)?.edit()?.putString(PREF_KEY_SANITIZE_REMINDER, dir.absolutePath)?.apply()
            }
        }
    }
}