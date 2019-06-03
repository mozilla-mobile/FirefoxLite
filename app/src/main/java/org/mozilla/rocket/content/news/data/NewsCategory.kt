package org.mozilla.rocket.content.news.data

import org.mozilla.focus.R

sealed class NewsCategory(
    val categoryId: String,
    val stringResourceId: Int,
    val order: Int,
    var isSelected: Boolean = false
) {
    object TOP_NEWS : NewsCategory("top-news", R.string.news_category_option_top_news, 1, true)
    object INDIA : NewsCategory("india", R.string.news_category_option_india, 2, true)
    object WORLD : NewsCategory("world", R.string.news_category_option_world, 3, true)
    object TECHNOLOGY : NewsCategory("technology", R.string.news_category_option_technology, 4, true)
    object EDUCATION : NewsCategory("education", R.string.news_category_option_education, 5, true)
    object POLITICS : NewsCategory("politics", R.string.news_category_option_politics, 6, true)
    object BUSINESS : NewsCategory("business", R.string.news_category_option_business, 7, true)
    object CAREER : NewsCategory("career", R.string.news_category_option_career, 8, true)
    object HEALTH : NewsCategory("health", R.string.news_category_option_health, 9, true)
    object SPORTS : NewsCategory("sports", R.string.news_category_option_sports, 10, true)
    object CRICKET : NewsCategory("cricket", R.string.news_category_option_cricket, 11, true)
    object ENTERTAINMENT : NewsCategory("entertainment", R.string.news_category_option_entertainment, 12, true)
    object MOVIE_REVIEWS : NewsCategory("movie-reviews", R.string.news_category_option_movie_reviews, 13, true)
    object AUTOMOBILE : NewsCategory("automobile", R.string.news_category_option_automobile, 14, true)
    object REGIONAL : NewsCategory("regional", R.string.news_category_option_regional, 15)
    object TRAVEL : NewsCategory("travel", R.string.news_category_option_travel, 16)
    object LIFESTYLE : NewsCategory("lifestyle", R.string.news_category_option_lifestyle, 17)
    object FOOD : NewsCategory("food", R.string.news_category_option_food, 18)
    object VIDEOS : NewsCategory("videos", R.string.news_category_option_video, 19)
    object CITY : NewsCategory("City", R.string.news_category_option_city, 20)
    object EVENTS : NewsCategory("events", R.string.news_category_option_events, 21)
    object CRIME : NewsCategory("crime", R.string.news_category_option_crime, 22)
    object ASTROLOGY : NewsCategory("astrology", R.string.news_category_option_astrology, 23)
    object SCIENCE : NewsCategory("science", R.string.news_category_option_science, 24)
    object JOKES : NewsCategory("jokes", R.string.news_category_option_jokes, 25)

    companion object {
        private val mapping by lazy {
            setOf(
                TOP_NEWS,
                INDIA,
                WORLD,
                TECHNOLOGY,
                EDUCATION,
                POLITICS,
                BUSINESS,
                CAREER,
                HEALTH,
                SPORTS,
                CRICKET,
                ENTERTAINMENT,
                MOVIE_REVIEWS,
                AUTOMOBILE,
                REGIONAL,
                TRAVEL,
                LIFESTYLE,
                FOOD,
                VIDEOS,
                CITY,
                EVENTS,
                CRIME,
                ASTROLOGY,
                SCIENCE,
                JOKES
            ).associateBy(NewsCategory::categoryId)
        }

        fun getCategoryById(categoryId: String): NewsCategory? {
            return mapping[categoryId]
        }
    }

    override fun toString(): String {
        return "categoryId=$categoryId, stringResourceId=$stringResourceId, isSelected=$isSelected"
    }
}
