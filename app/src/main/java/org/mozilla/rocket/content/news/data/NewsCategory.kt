package org.mozilla.rocket.content.news.data

import org.mozilla.focus.R

sealed class NewsCategory(
    val categoryId: String,
    val stringResourceId: Int,
    val isSelected: Boolean = false
) {
    object TOP_NEWS : NewsCategory("top-news", R.string.news_category_option_top_news, true)
    object INDIA : NewsCategory("india", R.string.news_category_option_india, true)
    object WORLD : NewsCategory("world", R.string.news_category_option_world, true)
    object TECHNOLOGY : NewsCategory("technology", R.string.news_category_option_technology, true)
    object EDUCATION : NewsCategory("education", R.string.news_category_option_education, true)
    object POLITICS : NewsCategory("politics", R.string.news_category_option_politics, true)
    object BUSINESS : NewsCategory("business", R.string.news_category_option_business, true)
    object CAREER : NewsCategory("career", R.string.news_category_option_career, true)
    object HEALTH : NewsCategory("health", R.string.news_category_option_health, true)
    object SPORTS : NewsCategory("sports", R.string.news_category_option_sports, true)
    object CRICKET : NewsCategory("cricket", R.string.news_category_option_cricket, true)
    object ENTERTAINMENT : NewsCategory("entertainment", R.string.news_category_option_entertainment, true)
    object MOVIE_REVIEWS : NewsCategory("movie-reviews", R.string.news_category_option_movie_reviews, true)
    object AUTOMOBILE : NewsCategory("automobile", R.string.news_category_option_automobile, true)
    object REGIONAL : NewsCategory("regional", R.string.news_category_option_regional)
    object TRAVEL : NewsCategory("travel", R.string.news_category_option_travel)
    object LIFESTYLE : NewsCategory("lifestyle", R.string.news_category_option_lifestyle)
    object FOOD : NewsCategory("food", R.string.news_category_option_food)
    object VIDEOS : NewsCategory("videos", R.string.news_category_option_video)
    object CITY : NewsCategory("City", R.string.news_category_option_city)
    object EVENTS : NewsCategory("events", R.string.news_category_option_events)
    object CRIME : NewsCategory("crime", R.string.news_category_option_crime)
    object ASTROLOGY : NewsCategory("astrology", R.string.news_category_option_astrology)
    object SCIENCE : NewsCategory("science", R.string.news_category_option_science)
    object JOKES : NewsCategory("jokes", R.string.news_category_option_jokes)

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
}
