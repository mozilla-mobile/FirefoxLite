package org.mozilla.rocket.content.news.data

import org.json.JSONObject
import org.mozilla.focus.R

data class NewsCategory(
    val categoryId: String,
    val name: String,
    val stringResourceId: Int,
    val order: Int,
    var isSelected: Boolean = false
) {
    companion object {
        internal const val KEY_NAME = "name"
        internal const val KEY_STRING_RESOURCE_ID = "stringResourceId"
        internal const val KEY_ORDER = "order"

        private val mapping by lazy {
            setOf(
                NewsCategory("top-news", "top-news", R.string.news_category_option_top_news, 1, true),
                NewsCategory("india", "india", R.string.news_category_option_india, 2, true),
                NewsCategory("world", "world", R.string.news_category_option_world, 3, true),
                NewsCategory("technology", "technology", R.string.news_category_option_technology, 4, true),
                NewsCategory("education", "education", R.string.news_category_option_education, 5, true),
                NewsCategory("politics", "politics", R.string.news_category_option_politics, 6, true),
                NewsCategory("business", "business", R.string.news_category_option_business, 7, true),
                NewsCategory("career", "career", R.string.news_category_option_career, 8, true),
                NewsCategory("health", "health", R.string.news_category_option_health, 9, true),
                NewsCategory("sports", "sports", R.string.news_category_option_sports, 10, true),
                NewsCategory("cricket", "cricket", R.string.news_category_option_cricket, 11, true),
                NewsCategory("entertainment", "entertainment", R.string.news_category_option_entertainment, 12, true),
                NewsCategory("movie-reviews", "movie-reviews", R.string.news_category_option_movie_reviews, 13, true),
                NewsCategory("automobile", "automobile", R.string.news_category_option_automobile, 14, true),
                NewsCategory("regional", "regional", R.string.news_category_option_regional, 15),
                NewsCategory("travel", "travel", R.string.news_category_option_travel, 16),
                NewsCategory("lifestyle", "lifestyle", R.string.news_category_option_lifestyle, 17),
                NewsCategory("food", "food", R.string.news_category_option_food, 18),
                NewsCategory("videos", "videos", R.string.news_category_option_video, 19),
                NewsCategory("City", "City", R.string.news_category_option_city, 20),
                NewsCategory("events", "events", R.string.news_category_option_events, 21),
                NewsCategory("religion", "religion", R.string.news_category_option_religion, 22),
                NewsCategory("crime", "crime", R.string.news_category_option_crime, 23),
                NewsCategory("astrology", "astrology", R.string.news_category_option_astrology, 24),
                NewsCategory("science", "science", R.string.news_category_option_science, 25),
                NewsCategory("jokes", "jokes", R.string.news_category_option_jokes, 26)
            ).associateBy(NewsCategory::categoryId)
        }

        fun getCategoryById(categoryId: String): NewsCategory? {
            return mapping[categoryId]?.copy()
        }

        fun fromJson(jsonString: String): List<NewsCategory> {
            val result = ArrayList<NewsCategory>()
            val items = JSONObject(jsonString)
            for (id in items.keys()) {
                val item = items.getJSONObject(id)
                val name = item.optString(KEY_NAME)
                val stringResourceId = item.optInt(KEY_STRING_RESOURCE_ID)
                val order = item.optInt(KEY_ORDER)
                result.add(NewsCategory(id, name, stringResourceId, order))
            }

            return result
        }
    }
}

fun List<NewsCategory>.toJson(): JSONObject {
    val node = JSONObject()
    for (newsCategory in this) {
        val item = JSONObject()
        item.put(NewsCategory.KEY_NAME, newsCategory.name)
        item.put(NewsCategory.KEY_STRING_RESOURCE_ID, newsCategory.stringResourceId)
        item.put(NewsCategory.KEY_ORDER, newsCategory.order)
        node.put(newsCategory.categoryId, item)
    }

    return node
}
