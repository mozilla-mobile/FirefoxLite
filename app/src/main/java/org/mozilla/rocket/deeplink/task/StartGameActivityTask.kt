package org.mozilla.rocket.deeplink.task

import android.content.Context
import android.content.Intent
import org.mozilla.rocket.content.game.ui.GameActivity

class StartGameActivityTask : Task {
    override fun execute(context: Context) {
        context.startActivity(GameActivity.getStartIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}