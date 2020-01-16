package org.mozilla.rocket.deeplink.task

import android.content.Context
import android.content.Intent
import org.mozilla.rocket.content.news.ui.NewsActivity

class StartNewsActivityTask : Task {
    override fun execute(context: Context) {
        context.startActivity(NewsActivity.getStartIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}