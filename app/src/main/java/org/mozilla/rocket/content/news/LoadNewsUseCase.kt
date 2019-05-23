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

import org.mozilla.lite.partner.NewsItem
import org.mozilla.lite.partner.Repository
import org.mozilla.rocket.content.MediatorUseCase
import org.mozilla.rocket.content.Result

class LoadNewsUseCase(private val repository: Repository<out NewsItem>) :
    MediatorUseCase<LoadNewsParameter, LoadNewsResult>(),
    Repository.OnDataChangedListener<NewsItem> {

    init {
        repository.setOnDataChangedListener(this)
    }

    override fun onDataChanged(itemPojoList: MutableList<NewsItem>?) {
        if (itemPojoList == null) {
            result.value = Result.Error(NewsNotFoundException())
        } else {
            result.value = Result.Success(LoadNewsResult(itemPojoList))
        }
    }

    override fun execute(parameters: LoadNewsParameter) {
        repository.loadMore()
    }
}

class NewsNotFoundException : Exception()

data class LoadNewsResult(

    val items: MutableList<NewsItem>
)

data class LoadNewsParameter(
    val category: String,
    val language: String
)
