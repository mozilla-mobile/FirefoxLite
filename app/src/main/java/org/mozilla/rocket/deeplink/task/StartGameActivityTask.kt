package org.mozilla.rocket.deeplink.task

import android.content.Context
import org.mozilla.rocket.content.games.ui.GameActivity

class StartGameActivityTask : Task {
    override fun execute(context: Context) {
        context.startActivity(GameActivity.getStartIntent(context))
    }
}