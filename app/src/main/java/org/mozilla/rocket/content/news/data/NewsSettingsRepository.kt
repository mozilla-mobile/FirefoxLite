package org.mozilla.rocket.content.news.data

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations

class NewsSettingsRepository(private val remoteDataSource: NewsSettingsDataSource) {
    fun getLanguages(): LiveData<List<NewsLanguage>> {
        return remoteDataSource.getSupportLanguages()
    }

    fun getCategoriesByLanguage(language: String): LiveData<List<NewsCategory>> {
        return Transformations.map(remoteDataSource.getSupportCategories(language)) {
            it.asSequence()
                .mapNotNull { categoryId -> NewsCategory.getCategoryById(categoryId) }
                .toList()
        }
    }
}