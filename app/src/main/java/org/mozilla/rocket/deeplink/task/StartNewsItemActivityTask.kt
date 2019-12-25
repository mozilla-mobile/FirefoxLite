package org.mozilla.rocket.deeplink.task

import android.content.Context
import android.content.Intent
import org.mozilla.rocket.content.news.ui.NewsActivity
import org.mozilla.rocket.content.news.ui.NewsActivity.DeepLink.NewsItemPage
import org.mozilla.rocket.content.news.ui.NewsActivity.DeepLink.NewsItemPage.Data

class StartNewsItemActivityTask(val url: String, val feed: String, val source: String) : Task {
    override fun execute(context: Context) {
        val deepLink = NewsItemPage(Data(url, feed, source))
        context.startActivity(NewsActivity.getStartIntent(context, deepLink).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}