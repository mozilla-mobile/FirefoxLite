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

import androidx.lifecycle.LiveData
import androidx.paging.Config
import androidx.paging.PagedList
import androidx.paging.toLiveData
import org.mozilla.rocket.adapter.DelegateAdapter
import org.mozilla.rocket.content.news.data.NewsRepository
import org.mozilla.rocket.content.news.ui.NewsMapper

class LoadNewsUseCase(
    private val repository: NewsRepository
    // TODO: Evan: add source icon item back
//    private val getAdditionalSourceInfoUseCase: GetAdditionalSourceInfoUseCase
) {

    operator fun invoke(loadNewsParameter: LoadNewsParameter): LiveData<PagedList<DelegateAdapter.UiModel>> {
        return repository.getNewsItemsDataSourceFactory().apply {
            category = loadNewsParameter.topic
            language = loadNewsParameter.language
        }.mapByPage { newItems ->
            newItems.map { NewsMapper.toNewsUiModel(it) as DelegateAdapter.UiModel }
        }.toLiveData(
            config = Config(
                pageSize = loadNewsParameter.pageSize,
                initialLoadSizeHint = loadNewsParameter.pageSize
            )
        )
    }
}

data class LoadNewsParameter(
    val topic: String,
    val language: String,
    val pageSize: Int
)
