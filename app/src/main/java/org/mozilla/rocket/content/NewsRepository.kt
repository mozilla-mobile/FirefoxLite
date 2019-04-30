package org.mozilla.rocket.content

import android.annotation.SuppressLint
import android.content.Context
import org.mozilla.lite.newspoint.RepositoryNewsPoint
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository

class NewsRepository {
    companion object {

        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: Repository<out NewsItem>? = null

        @JvmStatic
        fun getInstance(
            context: Context?
        ): Repository<out NewsItem> = INSTANCE ?: synchronized(this) {
            if (context == null) {
                throw IllegalStateException("can't create Content Repository with null context")
            }
            INSTANCE ?: buildRepository(context.applicationContext).also { INSTANCE = it }
        }

        @JvmStatic
        fun reset() {
            INSTANCE?.reset()
            INSTANCE = null
        }

        @JvmStatic
        fun resetSubscriptionUrl(subscriptionUrl: String) {
            INSTANCE?.setSubscriptionUrl(subscriptionUrl)
        }

        @JvmStatic
        fun isEmpty() = INSTANCE == null

        private fun buildRepository(context: Context): Repository<out NewsItem> {
            return RepositoryNewsPoint(context, NewsSourceManager.getInstance().newsSourceUrl)
        }
    }
}
