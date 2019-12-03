package org.mozilla.rocket.deeplink.task

import android.content.Context
import org.mozilla.rocket.msrp.ui.RewardActivity

class StartRewardActivityTask : Task {
    override fun execute(context: Context) {
        context.startActivity(RewardActivity.getStartIntent(context))
    }
}