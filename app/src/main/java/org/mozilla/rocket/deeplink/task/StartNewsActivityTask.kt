package org.mozilla.rocket.deeplink.task

import android.content.Context
import org.mozilla.rocket.content.news.ui.NewsActivity

class StartNewsActivityTask : Task {
    override fun execute(context: Context) {
        context.startActivity(NewsActivity.getStartIntent(context))
    }
}