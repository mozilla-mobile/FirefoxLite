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
import org.mozilla.rocket.content.news.data.NewsItem
import org.mozilla.rocket.content.news.data.NewsRepository

class LoadNewsUseCase(private val repository: NewsRepository) {

    suspend operator fun invoke(loadNewsParameter: LoadNewsParameter): Result<List<NewsItem>> {
        return repository.getNewsItems(
            loadNewsParameter.topic,
            loadNewsParameter.language,
            loadNewsParameter.pages,
            loadNewsParameter.pageSize
        )
    }
}

data class LoadNewsParameter(
    val topic: String,
    val language: String,
    val pages: Int,
    val pageSize: Int
)

fun LoadNewsParameter.nextPage(): LoadNewsParameter {
    return LoadNewsParameter(topic, language, pages + 1, pageSize)
}
