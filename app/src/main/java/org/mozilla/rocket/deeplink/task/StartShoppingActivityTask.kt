package org.mozilla.rocket.deeplink.task

import android.content.Context
import android.content.Intent
import org.mozilla.rocket.content.ecommerce.ui.ShoppingActivity

class StartShoppingActivityTask : Task {
    override fun execute(context: Context) {
        context.startActivity(ShoppingActivity.getStartIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}