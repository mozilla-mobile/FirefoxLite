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
            /**
             *  This intent is used when we want to set default browser on Android L, see [DefaultBrowserPreference]
             * */
            if (intent.getBooleanExtra(DefaultBrowserPreference.EXTRA_RESOLVE_BROWSER, false)) {
                context.startActivity(Intent(context, SettingsActivity::class.java))
                return Action.HANDLED
            }
            /**
             * This extra is passed by the Notification (either [RocketMessagingService.onRemoteMessage] or System tray
             * if we have this extra, we want to show this url in a new tab
             */
            val pushOpenUrl = intent.getStringExtra(RocketMessagingService.PUSH_OPEN_URL)
            if (pushOpenUrl != null) {

                val new = Intent(Intent.ACTION_VIEW)
                new.data = Uri.parse(pushOpenUrl)
                new.action = Intent.ACTION_VIEW
                new.setClass(context, MainActivity::class.java)
                new.putExtra(IntentUtils.EXTRA_OPEN_NEW_TAB, true)
                context.startActivity(new)
                return Action.HANDLED
            }
            return Action.NORMAL
        }
    }

}
