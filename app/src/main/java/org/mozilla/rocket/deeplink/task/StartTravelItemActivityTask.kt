package org.mozilla.rocket.deeplink.task

import android.content.Context
import android.content.Intent
import org.mozilla.rocket.content.travel.ui.TravelActivity
import org.mozilla.rocket.content.travel.ui.TravelActivity.DeepLink.TravelItemPage
import org.mozilla.rocket.content.travel.ui.TravelActivity.DeepLink.TravelItemPage.Data

class StartTravelItemActivityTask(val url: String, val feed: String, val source: String) : Task {
    override fun execute(context: Context) {
        val deepLink = TravelItemPage(Data(url, feed, source))
        context.startActivity(TravelActivity.getStartIntent(context, deepLink).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}