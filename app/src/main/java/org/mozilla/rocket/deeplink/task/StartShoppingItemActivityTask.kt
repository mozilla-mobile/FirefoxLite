package org.mozilla.rocket.deeplink.task

import android.content.Context
import android.content.Intent
import org.mozilla.rocket.content.ecommerce.ui.ShoppingActivity
import org.mozilla.rocket.content.ecommerce.ui.ShoppingActivity.DeepLink.ShoppingItemPage
import org.mozilla.rocket.content.ecommerce.ui.ShoppingActivity.DeepLink.ShoppingItemPage.Data

class StartShoppingItemActivityTask(val url: String, val feed: String, val source: String) : Task {
    override fun execute(context: Context) {
        val deepLink = ShoppingItemPage(Data(url, feed, source))
        context.startActivity(ShoppingActivity.getStartIntent(context, deepLink).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        })
    }
}