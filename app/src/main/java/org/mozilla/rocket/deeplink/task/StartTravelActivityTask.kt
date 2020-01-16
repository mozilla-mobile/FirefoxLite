package org.mozilla.rocket.deeplink.task

import android.content.Context
import android.content.Intent
import org.mozilla.rocket.content.travel.ui.TravelActivity

class StartTravelActivityTask : Task {
    override fun execute(context: Context) {
        context.startActivity(TravelActivity.getStartIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}