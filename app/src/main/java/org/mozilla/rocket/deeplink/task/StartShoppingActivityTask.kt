package org.mozilla.rocket.deeplink.task

import android.content.Context
import org.mozilla.rocket.content.ecommerce.ui.ShoppingActivity

class StartShoppingActivityTask : Task {
    override fun execute(context: Context) {
        context.startActivity(ShoppingActivity.getStartIntent(context))
    }
}