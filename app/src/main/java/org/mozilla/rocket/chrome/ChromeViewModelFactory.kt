package org.mozilla.rocket.chrome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.utils.Settings

class ChromeViewModelFactory private constructor(
    private val settings: Settings,
    private val bookmarkRepo: BookmarkRepository
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChromeViewModel::class.java)) {
            return ChromeViewModel(settings, bookmarkRepo) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {

        @Volatile private var INSTANCE: ChromeViewModelFactory? = null

        @JvmStatic
        fun getInstance(settings: Settings, bookmarkRepo: BookmarkRepository): ChromeViewModelFactory? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: ChromeViewModelFactory(settings, bookmarkRepo).also { INSTANCE = it }
                }
    }
}
