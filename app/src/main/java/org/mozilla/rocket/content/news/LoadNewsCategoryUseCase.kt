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

import android.content.Context
import android.support.annotation.WorkerThread
import android.support.v7.preference.PreferenceManager
import org.mozilla.focus.R
import org.mozilla.rocket.content.MediatorUseCase
import org.mozilla.rocket.content.Result
import org.mozilla.threadutils.ThreadUtils
import java.util.Random
import javax.inject.Inject

class FakeNewsCategoryRepository @Inject constructor(val context: Context) {

    @WorkerThread
    fun getNewsCatsPref(): MutableSet<String>? {
        val preferenceManager = PreferenceManager.getDefaultSharedPreferences(context)
        val key = context.getString(R.string.pref_key_s_news_categories)
        // TODO: remove this before merge. now simulate 90% the API call will success.
        if (Random().nextInt(10) >= 9) {
            return preferenceManager.getStringSet(key, null)
        }
        val default = setOf(
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
        return preferenceManager.getStringSet(key, default)
    }
}

open class LoadNewsCategoryUseCase @Inject constructor(private val repository: FakeNewsCategoryRepository) :

    MediatorUseCase<LoadNewsCategoryByLangParameter, LoadNewsCategoryByLangResult>() {
    override fun execute(parameters: LoadNewsCategoryByLangParameter) {
        // TODO: use coroutine when we have coroutineContext in androidx.core:core-ktx
        ThreadUtils.postToBackgroundThread {
            val newsCatsPref = repository.getNewsCatsPref()
            if (newsCatsPref == null) {
                result.postValue(Result.Error(NewsCategoryNotFoundException()))
            } else {
                val cats = LoadNewsCategoryByLangResult(newsCatsPref.toList())
                result.postValue(Result.Success(cats))
            }
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
