package org.mozilla.rocket.privately

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.preference.PreferenceManager
import org.mozilla.focus.utils.FileUtils.WEBVIEW_DIRECTORY
import org.mozilla.rocket.tabs.Tab
import java.io.File

// Describe how to clear the private mode session
interface PrivateModeListener {
    fun onFirstObserver()
    fun onZeroObserver(footprint: Footprint)
}

// Describe what to clear when clearing private mode session
class Footprint {
    val filePath = mutableListOf<File>()

    companion object {
        fun generate(context: Context): Footprint {
            val footprint = Footprint()

            context.cacheDir?.let { dir ->
                footprint.filePath.add(dir)
            }

            context.getDir(WEBVIEW_DIRECTORY, MODE_PRIVATE)?.let { dir ->
                footprint.filePath.add(dir)
            }
            return footprint
        }
    }
}

// Describe when to clear the private mode session
class PrivateMode(private val privateModeListener: PrivateModeListener) {

    val tabs: HashMap<String, Footprint> = HashMap()

    // Provide common resources, and helper functions
    companion object {
        const val PREF_KEY_PRIVATE_MODE_ENABLED = "pref_key_private_mode_enabled"
        const val PREF_KEY_SANITIZE_REMINDER = "pref_key_sanitize_reminder"

        const val INTENT_EXTRA_SANITIZE = "intent_extra_sanitize"
        const val PRIVATE_PROCESS_NAME = "private_mode"
        private const val PREF_KEY_PRIVATE_MODE_ENABLED_DEFAULT = false

        // Private Mode is currently behind a pref and is default off.
        // The option to enable it is on Nightly. The logic is in SettingsFragment.
        @JvmStatic
        fun isEnable(context: Context): Boolean {
            return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(PREF_KEY_PRIVATE_MODE_ENABLED, PREF_KEY_PRIVATE_MODE_ENABLED_DEFAULT)
        }

    }

    // A new private mode session added
    fun register(tab: Tab?, footprint: Footprint) {
        tab?.apply {
            tabs[this.id] = footprint
            // show the notification is the first private session registered
            if (tabs.size == 1) {
                privateModeListener.onFirstObserver()
            }
        }


    }

    // A new private mode session ended. Base on current design, we clear the cache when all sessions are ended
    fun unregister(tab: Tab?) {
        tab?.apply {
            val footprint = tabs[this.id]
            if (footprint != null) {
                tabs.remove(this.id)
                if (tabs.size == 0) {
                    privateModeListener.onZeroObserver(footprint)
                }
            }
        }

    }


}