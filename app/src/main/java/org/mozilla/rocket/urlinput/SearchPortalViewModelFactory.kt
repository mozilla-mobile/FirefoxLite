package org.mozilla.rocket.urlinput

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

class SearchPortalViewModelFactory(private val repository: SearchPortalRepository) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>) = SearchPortalViewModel(repository) as T
}
