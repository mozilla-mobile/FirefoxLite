package org.mozilla.rocket.deeplink.task

import android.content.Context
import org.mozilla.rocket.content.travel.ui.TravelActivity

class StartTravelActivityTask : Task {
    override fun execute(context: Context) {
        context.startActivity(TravelActivity.getStartIntent(context))
    }
}