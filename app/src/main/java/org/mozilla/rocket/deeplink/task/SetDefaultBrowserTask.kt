package org.mozilla.rocket.deeplink.task

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.utils.SupportUtils

class SetDefaultBrowserTask : Task {
    override fun execute(context: Context) {
        val intent = getPreferenceDefaultBrowserIntent(context)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    fun getPreferenceDefaultBrowserIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)
        } else {
            val fallbackTitle = context.getString(R.string.preference_default_browser).toString() + "\uD83D\uDE4C"
            InfoActivity.getIntentFor(context, SupportUtils.getSumoURLForTopic(context, "rocket-default"), fallbackTitle)
        }
    }
}