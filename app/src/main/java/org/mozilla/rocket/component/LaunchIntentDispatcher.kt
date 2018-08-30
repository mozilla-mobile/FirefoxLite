package org.mozilla.rocket.component

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.support.annotation.CheckResult
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.activity.SettingsActivity
import org.mozilla.focus.notification.RocketMessagingService
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.focus.utils.AppConstants
import org.mozilla.focus.widget.DefaultBrowserPreference
import org.mozilla.rocket.component.LaunchIntentDispatcher.Command.SET_DEFAULT

class LaunchIntentDispatcher {

    enum class Command(val value: String) {
        SET_DEFAULT("SET_DEFAULT")
    }

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
             * if we have this extra, we want to show this url either
             * 1. If there are other apps can handle it, use that app or
             * 2. Open the url in a new tab in Rocket
             */
            intent.getStringExtra(RocketMessagingService.PUSH_OPEN_URL)?.run {

                intent.data = Uri.parse(this)

                // If it's an app link, open it.
                return intent.tryOpen(context) {
                    intent.setClass(context, MainActivity::class.java)
                    intent.putExtra(IntentUtils.EXTRA_OPEN_NEW_TAB, true)
                }
            }
            intent.getStringExtra(RocketMessagingService.PUSH_COMMAND)?.apply {
                when (this) {
                    SET_DEFAULT.value -> {
                        if (!IntentUtils.openDefaultAppsSettings(context)) {
                            intent.action = Intent.ACTION_VIEW
                            intent.data = Uri.parse(SupportUtils.getSumoURLForTopic(context, "rocket-default"))
                        }
                    }
                }
                return Action.NORMAL
            }
            return Action.NORMAL
        }
    }

}

private fun Intent.tryOpen(context: Context, fallback: () -> Unit): LaunchIntentDispatcher.Action {
    if (this.resolveActivity(context.packageManager).className != AppConstants.LAUNCHER_ACTIVITY_ALIAS) {
        context.startActivity(this)
        return LaunchIntentDispatcher.Action.HANDLED
    }
    fallback()
    return LaunchIntentDispatcher.Action.NORMAL


}
