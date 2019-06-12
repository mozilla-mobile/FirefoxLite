package org.mozilla.rocket.content.news.data

import org.mozilla.focus.R

data class NewsCategory(
    val categoryId: String,
    val telemetryId: String,
    val stringResourceId: Int,
    val order: Int,
    var isSelected: Boolean = false
) {
    companion object {
        private val mapping by lazy {
            setOf(
                NewsCategory("top-news", "", R.string.news_category_option_top_news, 1, true),
                NewsCategory("india", "", R.string.news_category_option_india, 2, true),
                NewsCategory("world", "", R.string.news_category_option_world, 3, true),
                NewsCategory("technology", "", R.string.news_category_option_technology, 4, true),
                NewsCategory("education", "", R.string.news_category_option_education, 5, true),
                NewsCategory("politics", "", R.string.news_category_option_politics, 6, true),
                NewsCategory("business", "", R.string.news_category_option_business, 7, true),
                NewsCategory("career", "", R.string.news_category_option_career, 8, true),
                NewsCategory("health", "", R.string.news_category_option_health, 9, true),
                NewsCategory("sports", "", R.string.news_category_option_sports, 10, true),
                NewsCategory("cricket", "", R.string.news_category_option_cricket, 11, true),
                NewsCategory("entertainment", "", R.string.news_category_option_entertainment, 12, true),
                NewsCategory("movie-reviews", "", R.string.news_category_option_movie_reviews, 13, true),
                NewsCategory("automobile", "", R.string.news_category_option_automobile, 14, true),
                NewsCategory("regional", "", R.string.news_category_option_regional, 15),
                NewsCategory("travel", "", R.string.news_category_option_travel, 16),
                NewsCategory("lifestyle", "", R.string.news_category_option_lifestyle, 17),
                NewsCategory("food", "", R.string.news_category_option_food, 18),
                NewsCategory("videos", "", R.string.news_category_option_video, 19),
                NewsCategory("City", "", R.string.news_category_option_city, 20),
                NewsCategory("events", "", R.string.news_category_option_events, 21),
                NewsCategory("crime", "", R.string.news_category_option_crime, 22),
                NewsCategory("astrology", "", R.string.news_category_option_astrology, 23),
                NewsCategory("science", "", R.string.news_category_option_science, 24),
                NewsCategory("jokes", "", R.string.news_category_option_jokes, 25)
            ).associateBy(NewsCategory::categoryId)
        }

        fun getCategoryById(categoryId: String): NewsCategory? {
            val newsCategory = mapping[categoryId]
            return if (newsCategory != null) {
                NewsCategory(
                    newsCategory.categoryId,
                    newsCategory.telemetryId,
                    newsCategory.stringResourceId,
                    newsCategory.order,
                    newsCategory.isSelected
                )
            } else {
                null
            }
        }
    }

    // TODO: workaround to fix the Kotlin and JVM 1.8 compatible issue: https://youtrack.jetbrains.com/issue/KT-31027
    override fun hashCode(): Int = categoryId.hashCode()
}
