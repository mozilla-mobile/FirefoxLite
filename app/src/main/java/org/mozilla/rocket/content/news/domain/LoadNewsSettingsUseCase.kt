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

package org.mozilla.rocket.content.news.domain

import org.mozilla.rocket.content.Result
import org.mozilla.rocket.content.news.data.NewsCategory
import org.mozilla.rocket.content.news.data.NewsLanguage
import org.mozilla.rocket.content.news.data.NewsSettingsRepository
import org.mozilla.rocket.content.succeeded

open class LoadNewsSettingsUseCase(private val repository: NewsSettingsRepository) {
    companion object {
        private const val DEFAULT_CATEGORY_ID = "top-news"
        private val DEFAULT_CATEGORY_LIST = listOf(
            NewsCategory.getCategoryById(DEFAULT_CATEGORY_ID)!!
        )
    }

    suspend operator fun invoke(): Result<Pair<NewsLanguage, List<NewsCategory>>> {
        val result = repository.getNewsSettings()
        return if (result.succeeded) {
            result
        } else {
            Result.Success(Pair(LoadNewsLanguagesUseCase.DEFAULT_LANGUAGE_LIST[0], DEFAULT_CATEGORY_LIST))
        }
    }
}