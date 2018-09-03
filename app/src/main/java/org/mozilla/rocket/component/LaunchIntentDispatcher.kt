package org.mozilla.rocket.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.annotation.CheckResult
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.activity.SettingsActivity
import org.mozilla.focus.notification.RocketMessagingService
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.widget.DefaultBrowserPreference

class LaunchIntentDispatcher {

    enum class Action {
        NORMAL, HANDLED
    }

    companion object {

        @JvmStatic
        @CheckResult
        fun dispatch(context: Context, intent: Intent): Action? {
            return Action.NORMAL
        }
    }

}
