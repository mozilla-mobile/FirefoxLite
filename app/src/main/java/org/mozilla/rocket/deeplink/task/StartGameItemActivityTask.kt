package org.mozilla.rocket.deeplink.task

import android.content.Context
import android.content.Intent
import org.mozilla.rocket.content.game.ui.GameActivity
import org.mozilla.rocket.content.game.ui.GameActivity.DeepLink.GameItemPage
import org.mozilla.rocket.content.game.ui.GameActivity.DeepLink.GameItemPage.Data

class StartGameItemActivityTask(val url: String, val feed: String, val source: String) : Task {
    override fun execute(context: Context) {
        val deepLink = GameItemPage(Data(url, feed, source))
        context.startActivity(GameActivity.getStartIntent(context, deepLink).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}