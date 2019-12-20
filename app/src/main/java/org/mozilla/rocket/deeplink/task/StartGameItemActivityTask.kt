package org.mozilla.rocket.deeplink.task

import android.content.Context
import org.mozilla.rocket.content.game.ui.GameModeActivity

class StartGameItemActivityTask(val url: String) : Task {
    override fun execute(context: Context) {
        context.startActivity(GameModeActivity.getStartIntent(context, url))
    }
}