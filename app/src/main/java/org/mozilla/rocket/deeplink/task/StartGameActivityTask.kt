package org.mozilla.rocket.deeplink.task

import android.content.Context
import org.mozilla.rocket.content.games.ui.GamesActivity

class StartGameActivityTask : Task {
    override fun execute(context: Context) {
        context.startActivity(GamesActivity.getStartIntent(context))
    }
}