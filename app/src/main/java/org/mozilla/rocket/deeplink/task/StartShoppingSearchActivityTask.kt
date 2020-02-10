package org.mozilla.rocket.deeplink.task

import android.content.Context
import android.content.Intent
import org.mozilla.rocket.shopping.search.ui.ShoppingSearchActivity

class StartShoppingSearchActivityTask : Task {
    override fun execute(context: Context) {
        context.startActivity(ShoppingSearchActivity.getStartIntent(context).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}