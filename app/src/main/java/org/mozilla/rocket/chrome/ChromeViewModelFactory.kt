package org.mozilla.rocket.chrome

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.helper.StorageHelper
import org.mozilla.rocket.privately.PrivateMode

class ChromeViewModelFactory private constructor(
    private val settings: Settings,
    private val bookmarkRepo: BookmarkRepository,
    private val privateMode: PrivateMode,
    private val browsers: Browsers,
    private val storageHelper: StorageHelper
) : ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChromeViewModel::class.java)) {
            return ChromeViewModel(
                    settings,
                    bookmarkRepo,
                    privateMode,
                    browsers,
                    storageHelper
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: " + modelClass.name)
    }

    companion object {

        @Volatile private var INSTANCE: ChromeViewModelFactory? = null

        @JvmStatic
        fun getInstance(
            settings: Settings,
            bookmarkRepo: BookmarkRepository,
            privateMode: PrivateMode,
            browsers: Browsers,
            storageHelper: StorageHelper
        ): ChromeViewModelFactory? =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: ChromeViewModelFactory(
                            settings,
                            bookmarkRepo,
                            privateMode,
                            browsers,
                            storageHelper
                    ).also { INSTANCE = it }
                }
    }
}
