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
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import javax.inject.Inject

open class LoadNewsCategoryUseCase @Inject constructor(private val repository: NewsSettingsRepository) :
    MediatorUseCase<LoadNewsCategoryByLangParameter, LoadNewsCategoryByLangResult>() {
    override fun execute(parameters: LoadNewsCategoryByLangParameter) {
        val categoriesLiveData = repository.getCategoriesByLanguage(parameters.language)
        result.removeSource(categoriesLiveData)
        result.addSource(categoriesLiveData) { newsCategories ->
            if (newsCategories == null) {
                result.postValue(Result.Error(NewsCategoryNotFoundException()))
            } else {
                val cats = LoadNewsCategoryByLangResult(newsCategories)
                result.postValue(Result.Success(cats))
            }
        }
    }
}

class NewsCategoryNotFoundException : Exception()

data class LoadNewsCategoryByLangResult(
    val categories: List<NewsCategory>
)

data class LoadNewsCategoryByLangParameter(
    val language: String
)
