package org.mozilla.rocket.shopping.search.data

import android.app.ActivityManager
import android.content.Context
import android.preference.PreferenceManager
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchActivity

class ShoppingSearchMode private constructor(context: Context) {
    private val appContext: Context = context.applicationContext

    @Suppress("deprecation")
    fun hasShoppingSearchActivity(): Boolean {
        val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (task in manager.getRunningTasks(Integer.MAX_VALUE)) {
            if (ShoppingSearchActivity::class.java.name == task.topActivity?.className) {
                return true
            }
        }

        return false
    }

    fun finish() {
        val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (task in manager.appTasks) {
            val className = task.taskInfo.baseIntent.component?.className
            if (ShoppingSearchActivity::class.java.name == className) {
                task.finishAndRemoveTask()
            }
        }
    }

    fun saveKeyword(keyword: String) {
        PreferenceManager.getDefaultSharedPreferences(appContext)?.edit()?.putString(PREF_KEY_SHOPPING_SEARCH_KEYWORD, keyword)?.apply()
    }

    fun retrieveKeyword() = PreferenceManager.getDefaultSharedPreferences(appContext)?.getString(PREF_KEY_SHOPPING_SEARCH_KEYWORD, "")

    fun deleteKeyword() {
        PreferenceManager.getDefaultSharedPreferences(appContext)?.edit()?.remove(PREF_KEY_SHOPPING_SEARCH_KEYWORD)?.apply()
    }

    companion object {
        const val PREF_KEY_SHOPPING_SEARCH_KEYWORD = "pref_key_shopping_search_keyword"

        @Volatile
        private var INSTANCE: ShoppingSearchMode? = null

        @JvmStatic
        fun getInstance(context: Context): ShoppingSearchMode =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ShoppingSearchMode(context).also { INSTANCE = it }
            }
    }
}