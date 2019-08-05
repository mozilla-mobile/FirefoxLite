package org.mozilla.rocket.chrome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.helper.StorageHelper
import org.mozilla.rocket.privately.PrivateMode

class ChromeViewModelFactory(
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
}
