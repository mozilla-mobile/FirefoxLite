package org.mozilla.rocket.chrome

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModel
import android.os.Parcel
import android.os.Parcelable
import org.mozilla.focus.persistence.BookmarkModel
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.invalidate
import org.mozilla.urlutils.UrlUtils

class ChromeViewModel(
    settings: Settings,
    private val bookmarkRepo: BookmarkRepository
) : ViewModel() {
    val isNightMode = MutableLiveData<Boolean>()
    val tabCount = MutableLiveData<Int>()
    val isTabRestoredComplete = MutableLiveData<Boolean>()
    val currentUrl = MutableLiveData<String>()
    val currentTitle = MutableLiveData<String>()
    var isCurrentUrlBookmarked: LiveData<Boolean>
    val isRefreshing = MutableLiveData<Boolean>()
    val canGoBack = MutableLiveData<Boolean>()
    val canGoForward = MutableLiveData<Boolean>()
    val isHomePageUrlInputShowing = MutableLiveData<Boolean>()
    val isMyShotOnBoardingPending = MutableLiveData<Boolean>()

    val openUrl = SingleLiveEvent<OpenUrlAction>()
    val showTabTray = SingleLiveEvent<Unit>()
    val showMenu = SingleLiveEvent<Unit>()
    val showNewTab = SingleLiveEvent<Unit>()
    val showUrlInput = SingleLiveEvent<String?>()
    val dismissUrlInput = SingleLiveEvent<Unit>()
    val doScreenshot = SingleLiveEvent<ScreenCaptureTelemetryData>()
    val pinShortcut = SingleLiveEvent<Unit>()
    val toggleBookmark = SingleLiveEvent<Unit>()
    // TODO: separate to startRefresh / stopLoading
    val refreshOrStop = SingleLiveEvent<Unit>()
    val share = SingleLiveEvent<Unit>()
    val goNext = SingleLiveEvent<Unit>()
    val showDownloadPanel = SingleLiveEvent<Unit>()
    val togglePrivateMode = SingleLiveEvent<Unit>()
    val dropCurrentPage = SingleLiveEvent<Unit>()
    val updateMenu = SingleLiveEvent<Unit>()
    val clearBrowsingHistory = SingleLiveEvent<Unit>()

    init {
        isNightMode.value = settings.isNightModeEnable
        isRefreshing.value = false
        canGoBack.value = false
        canGoForward.value = false

        isCurrentUrlBookmarked = Transformations.switchMap<String, List<BookmarkModel>>(currentUrl, bookmarkRepo::getBookmarksByUrl)
                .let { urlBookmarksLiveData ->
                    Transformations.map<List<BookmarkModel>, Boolean>(urlBookmarksLiveData) { it.isNotEmpty() }
                }
    }

    fun invalidate() {
        isNightMode.invalidate()
        tabCount.invalidate()
        isRefreshing.invalidate()
        canGoBack.invalidate()
        canGoForward.invalidate()
    }

    fun onNightModeChanged(isEnabled: Boolean) {
        if (isNightMode.value != isEnabled) {
            isNightMode.value = isEnabled
        }
    }

    fun onRestoreTabCountStarted() {
        isTabRestoredComplete.value = false
    }

    fun onRestoreTabCountCompleted() {
        isTabRestoredComplete.value = true
    }

    fun onTabCountChanged(count: Int) {
        if (isTabRestoredComplete.value == true) {
            val currentCount = tabCount.value
            if (currentCount != count) {
                tabCount.value = count
            }
        }
    }

    fun onFocusedUrlChanged(url: String?) {
        if (url != currentUrl.value) {
            currentUrl.value = url
        }
    }

    fun onFocusedTitleChanged(title: String?) {
        if (title != currentTitle.value) {
            currentTitle.value = title
        }
    }

    fun onPageLoadingStarted() {
        if (isRefreshing.value != true) {
            isRefreshing.value = true
        }
    }

    fun onPageLoadingStopped() {
        if (isRefreshing.value == true) {
            isRefreshing.value = false
        }
    }

    fun onNavigationStateChanged(canGoBack: Boolean, canGoForward: Boolean) {
        if (this.canGoBack.value != canGoBack) {
            this.canGoBack.value = canGoBack
        }
        if (this.canGoForward.value != canGoForward) {
            this.canGoForward.value = canGoForward
        }
    }

    fun onShowHomePageUrlInput() {
        isHomePageUrlInputShowing.value = true
    }

    fun onDismissHomePageUrlInput() {
        isHomePageUrlInputShowing.value = false
    }

    fun showMyShotOnBoarding() {
        if (isMyShotOnBoardingPending.value != true) {
            isMyShotOnBoardingPending.value = true
        }
    }

    fun onMyShotOnBoardingDisplayed() {
        if (isMyShotOnBoardingPending.value != false) {
            isMyShotOnBoardingPending.value = false
        }
    }

    fun addBookmark(): String? {
        var bookmarkId: String? = null
        val url = currentUrl.value
        if (!url.isNullOrEmpty()) {
            val title = currentTitle.value.takeUnless { it.isNullOrEmpty() }
                    ?: UrlUtils.stripCommonSubdomains(UrlUtils.stripHttp(url))
            bookmarkId = bookmarkRepo.addBookmark(title, url)
        }

        return bookmarkId
    }

    fun deleteBookmark() {
        currentUrl.value?.let { url ->
            bookmarkRepo.deleteBookmarksByUrl(url)
        }
    }

    data class OpenUrlAction(
        val url: String,
        val withNewTab: Boolean,
        val isFromExternal: Boolean
    )

    data class ScreenCaptureTelemetryData(val mode: String, val position: Int) : Parcelable {
        constructor(source: Parcel) : this(
                source.readString()!!,
                source.readInt()
        )

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel, flags: Int) = with(dest) {
            writeString(mode)
            writeInt(position)
        }

        companion object {
            @JvmField
            val CREATOR: Parcelable.Creator<ScreenCaptureTelemetryData> = object : Parcelable.Creator<ScreenCaptureTelemetryData> {
                override fun createFromParcel(source: Parcel): ScreenCaptureTelemetryData = ScreenCaptureTelemetryData(source)
                override fun newArray(size: Int): Array<ScreenCaptureTelemetryData?> = arrayOfNulls(size)
            }
        }
    }
}
