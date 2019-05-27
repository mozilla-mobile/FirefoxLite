package org.mozilla.rocket.content.news

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

class NewsViewModelFactory constructor(
    private val repository: FakeNewsCategoryRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(NewsViewModel::class.java)) {
            return NewsViewModel(LoadNewsCategoryUseCase(repository)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }
}