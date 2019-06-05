package org.mozilla.rocket.chrome

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.privately.PrivateMode

class ChromeViewModelFactory private constructor(
    private val settings: Settings,
    private val bookmarkRepo: BookmarkRepository,
    private val privateMode: PrivateMode
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChromeViewModel::class.java)) {
            return ChromeViewModel(settings, bookmarkRepo, privateMode) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {

        @Volatile private var INSTANCE: ChromeViewModelFactory? = null

        @JvmStatic
        fun getInstance(
            settings: Settings,
            bookmarkRepo: BookmarkRepository,
            privateMode: PrivateMode
        ): ChromeViewModelFactory? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: ChromeViewModelFactory(settings, bookmarkRepo, privateMode).also { INSTANCE = it }
                }
    }
}
