package org.mozilla.rocket.content.news.data

import android.content.Context
import org.mozilla.lite.newspoint.RepositoryNewsPoint
import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository
import java.util.Locale

class NewsRepository {
    companion object {
        const val CONFIG_URL = "url"
        const val CONFIG_CATEGORY = "category"
        const val CONFIG_LANGUAGE = "language"

        @JvmStatic
        fun newInstance(
            context: Context?,
            configurations: HashMap<String, String>
        ): Repository<out NewsItem> {
            if (context == null) {
                throw IllegalStateException("can't create Content Repository with null context")
            }

            return buildRepository(context.applicationContext, configurations)
        }

        private fun buildRepository(context: Context, configurations: HashMap<String, String>): Repository<out NewsItem> {
            val url = String.format(
                Locale.getDefault(),
                configurations[CONFIG_URL] ?: "",
                configurations[CONFIG_CATEGORY],
                configurations[CONFIG_LANGUAGE],
                "%d",
                "%d"
            )
            return RepositoryNewsPoint(context, url)
        }
    }
}
