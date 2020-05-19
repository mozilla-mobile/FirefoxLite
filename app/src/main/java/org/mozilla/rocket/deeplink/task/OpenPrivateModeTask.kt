package org.mozilla.rocket.deeplink.task

import android.content.Context
import org.mozilla.rocket.privately.PrivateModeActivity

class OpenPrivateModeTask : Task {
    override fun execute(context: Context) {
        context.startActivity(PrivateModeActivity.getStartIntent(context))
    }
}