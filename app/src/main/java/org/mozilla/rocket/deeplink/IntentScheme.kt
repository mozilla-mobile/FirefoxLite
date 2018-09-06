package org.mozilla.rocket.deeplink

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.utils.IntentUtils

class IntentScheme {
    companion object {

        private const val TAG = "IntentScheme"
        public const val SCHEME = "intent"
        private const val EXTRA_START = "S."
        private const val DATA = "data"
        private const val PACKAGE = "package"
        private const val ACTION = "action"
        private const val CATEGORY = "category"
        private const val COMPONENT = "component"
        private const val FACLLBACK_URL = "S.browser_fallback_url"

        @JvmStatic
        fun isIntent(context: Context, url: String): Boolean {
            return parse(context, Uri.parse(url)) != null
        }

        @JvmStatic
        fun parse(context: Context, uri: Uri): Intent? {
            if (SCHEME != uri.scheme) {
                return null
            }
            val deepLink = Intent().apply {

                for (query in uri.queryParameterNames) {
                    if (query.startsWith(EXTRA_START)) {
                        val extra = query.substring(EXTRA_START.length)
                        putExtra(extra, uri.getQueryParameter(query))
                    }
                }
                uri.getQueryParameter(DATA)?.apply { data = Uri.parse(this) }
                uri.getQueryParameter(PACKAGE)?.apply { setPackage(this) }
                uri.getQueryParameter(ACTION)?.apply { action = this }
                uri.getQueryParameters(CATEGORY)?.apply {
                    this.forEach {
                        addCategory(it)
                    }
                }
                uri.getQueryParameter(COMPONENT)?.apply { component = ComponentName(context, this) }
            }
            try {
                // if the intent can be handle by other apps, it's a valid intent scheme
                val resolveActivity = deepLink.resolveActivity(context.packageManager)

                return if (resolveActivity != null && AppConstants.LAUNCHER_ACTIVITY != resolveActivity.className) {
                    // if other app can handle this, we return the intent and let caller to use it and exit early
                    deepLink
                } else {
                    // if we are the default handler, return false and continue
                    return fallback(context, uri)

                }
            } catch (e: IllegalStateException) {
                Log.e(TAG, "This intent is not supported ", e)

                return fallback(context, uri)
            }
        }

        /*
         * When an intent could not be resolved, or an external application could not be launched, then the
         * user will be redirected to the fallback URL if it was given.
         */
        private fun fallback(context: Context, uri: Uri): Intent? {
            val fallbackUrl = uri.getQueryParameter(FACLLBACK_URL)
            if (fallbackUrl == null) {
                return null
            } else {
                return Intent().apply {
                    setComponent(ComponentName(context, AppConstants.LAUNCHER_ACTIVITY))
                    setAction(Intent.ACTION_VIEW)
                    setData((Uri.parse(fallbackUrl)))
                    putExtra(IntentUtils.EXTRA_OPEN_NEW_TAB, true)
                }
            }
        }

    }
}
