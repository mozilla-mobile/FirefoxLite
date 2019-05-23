/*
 * Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mozilla.rocket.content.news

import org.mozilla.rocket.content.MediatorUseCase
import org.mozilla.rocket.content.Result
import java.util.Random

class FakeNewsCategoryRepository

open class LoadNewsCategoryUseCase(private val repository: FakeNewsCategoryRepository) :

    MediatorUseCase<LoadNewsCategoryByLangParameter, LoadNewsCategoryByLangResult>() {
    override fun execute(parameters: LoadNewsCategoryByLangParameter) {
        // TODO: replace with repository.loadCategories(), now simulate 90% the API call will success.
        val categories = if (Random().nextInt(10) > 1) {
            val sample = listOf(
                "movie-reviews",
                "politics",
                "career",
                "education",
                "entertainment",
                "regional",
                "videos",
                "astrology",
                "india",
                "photos",
                "automobile",
                "world",
                "crime",
                "events",
                "top-news",
                "sports",
                "business",
                "health",
                "technology",
                "City",
                "food",
                "lifestyle",
                "cricket",
                "science",
                "travel",
                "jokes"
            )
            LoadNewsCategoryByLangResult(sample)
        } else {
            null
        }
        if (categories == null) {
            result.postValue(Result.Error(NewsCategoryNotFoundException()))
        } else {
            result.postValue(Result.Success(categories))
        }
    }
}

class NewsCategoryNotFoundException : Exception()

data class LoadNewsCategoryByLangResult(

    val categories: List<String>
)

data class LoadNewsCategoryByLangParameter(

    val language: String
)
