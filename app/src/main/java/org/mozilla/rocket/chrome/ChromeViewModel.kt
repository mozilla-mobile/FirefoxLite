package org.mozilla.rocket.chrome

import android.os.Parcel
import android.os.Parcelable
import android.os.SystemClock
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.repository.BookmarkRepository
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.NewFeatureNotice
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.extension.switchMap
import org.mozilla.rocket.helper.StorageHelper
import org.mozilla.rocket.nightmode.AdjustBrightnessDialog
import org.mozilla.rocket.privately.PrivateMode
import org.mozilla.rocket.util.ToastMessage
import org.mozilla.rocket.util.ToastMessage.Companion.LENGTH_LONG
import org.mozilla.urlutils.UrlUtils
import kotlin.concurrent.thread

class ChromeViewModel(
    private val settings: Settings,
    private var newFeatureNotice: NewFeatureNotice,
    private val bookmarkRepo: BookmarkRepository,
    private val privateMode: PrivateMode,
    private val browsers: Browsers,
    private val storageHelper: StorageHelper
) : ViewModel() {
    val isNightMode: LiveData<NightModeSettings> = settings.isNightModeEnablLiveData.map { NightModeSettings(it, settings.nightModeBrightnessValue) }
    val tabCount = MutableLiveData<Int>()
    val isTabRestoredComplete = MutableLiveData<Boolean>()
    val navigationState = MutableLiveData<ScreenNavigator.NavigationState>()
    val currentUrl = MutableLiveData<String>()
    val currentTitle = MutableLiveData<String>()
    var isCurrentUrlBookmarked: LiveData<Boolean> = currentUrl.switchMap(bookmarkRepo::getBookmarksByUrl).map { it.isNotEmpty() }
    val isRefreshing = MutableLiveData<Boolean>()
    val canGoBack = MutableLiveData<Boolean>()
    val canGoForward = MutableLiveData<Boolean>()
    val isHomePageUrlInputShowing = MutableLiveData<Boolean>()
    val isMyShotOnBoardingPending = MutableLiveData<Boolean>()
    val isTurboModeEnabled: LiveData<Boolean> = settings.shouldUseTurboModeLiveData()
    val isBlockImageEnabled: LiveData<Boolean> = settings.shouldBlockImagesLiveData()
    val isBlockJavaScriptEnabled: LiveData<Boolean> = settings.shouldBlockJavaScriptLiveData()
    val hasUnreadScreenshot: LiveData<Boolean> = settings.hasUnreadMyShotLiveData()
    val isPrivateBrowsingActive = MutableLiveData<Boolean>()

    val shouldShowFirstrun: Boolean
        get() = newFeatureNotice.shouldShowLiteUpdate()

    val showToast = SingleLiveEvent<ToastMessage>()
    val openUrl = SingleLiveEvent<OpenUrlAction>()
    val showTabTray = SingleLiveEvent<Unit>()
    val showMenu = SingleLiveEvent<Unit>()
    val showNewTab = SingleLiveEvent<Unit>()
    val showUrlInput = SingleLiveEvent<String?>()
    val dismissUrlInput = SingleLiveEvent<Unit>()
    val doScreenshot = SingleLiveEvent<ScreenCaptureTelemetryData>()
    val pinShortcut = SingleLiveEvent<Unit>()
    val bookmarkAdded = SingleLiveEvent<String>()
    // TODO: separate to startRefresh / stopLoading
    val refreshOrStop = SingleLiveEvent<Unit>()
    val share = SingleLiveEvent<Unit>()
    val goBack = SingleLiveEvent<Unit>()
    val goNext = SingleLiveEvent<Unit>()
    val showDownloadPanel = SingleLiveEvent<Unit>()
    val togglePrivateMode = SingleLiveEvent<Unit>()
    val dropCurrentPage = SingleLiveEvent<Unit>()
    val updateMenu = SingleLiveEvent<Unit>()
    val clearBrowsingHistory = SingleLiveEvent<Unit>()
    val driveDefaultBrowser = SingleLiveEvent<Unit>()
    val exitApp = SingleLiveEvent<Unit>()
    val showBookmarks = SingleLiveEvent<Unit>()
    val showHistory = SingleLiveEvent<Unit>()
    val showScreenshots = SingleLiveEvent<Unit>()
    val openPreference = SingleLiveEvent<Unit>()
    val showFindInPage = SingleLiveEvent<Unit>()
    val showAdjustBrightness = SingleLiveEvent<Unit>()
    val showNightModeOnBoarding = SingleLiveEvent<Unit>()

    private var lastUrlLoadStart = 0L
    private var lastUrlLoadTime = 0L

    init {
        isPrivateBrowsingActive.value = privateMode.hasPrivateSession()
        isRefreshing.value = false
        canGoBack.value = false
        canGoForward.value = false
        isHomePageUrlInputShowing.value = false
        isMyShotOnBoardingPending.value = false

        thread { checkRemovableStorage() }
    }

    /**
     * To check existence of removable storage, and write result to preference
     */
    private fun checkRemovableStorage() {
        settings.removableStorageStateOnCreate = storageHelper.hasRemovableStorage()
    }

    fun adjustNightMode() {
        updateNightMode(true)
        showAdjustBrightness.call()
    }

    fun onNightModeToggled() {
        updateNightMode(!settings.isNightModeEnable)
        showAdjustBrightnessIfNeeded()
    }

    private fun updateNightMode(isEnabled: Boolean) {
        settings.setNightMode(isEnabled)
        TelemetryWrapper.menuNightModeChangeTo(isEnabled)
    }

    private fun showAdjustBrightnessIfNeeded() {
        val currentBrightness = settings.nightModeBrightnessValue
        if (currentBrightness == BRIGHTNESS_OVERRIDE_NONE) {
            // First time turn on
            settings.nightModeBrightnessValue = AdjustBrightnessDialog.Constants.DEFAULT_BRIGHTNESS
            showAdjustBrightness.call()
            settings.setNightModeSpotlight(true)
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
        lastUrlLoadStart = SystemClock.elapsedRealtime()
        if (isRefreshing.value != true) {
            isRefreshing.value = true
        }
    }

    fun onPageLoadingStopped() {
        lastUrlLoadTime = SystemClock.elapsedRealtime() - lastUrlLoadStart
        if (isRefreshing.value == true) {
            isRefreshing.value = false
        }
        updateMenu()
    }

    fun onMenuShown() {
        updateMenu()
    }

    private fun updateMenu() {
        updateMenu.call()
        checkIfShowPrivateBrowsingOnBoarding()
    }

    private fun checkIfShowPrivateBrowsingOnBoarding() {
        if (settings.showNightModeSpotlight()) {
            settings.setNightModeSpotlight(false)
            showNightModeOnBoarding.call()
        }
    }

    fun checkIfPrivateBrowsingActive() {
        val hasPrivateSession = privateMode.hasPrivateSession()
        if (isPrivateBrowsingActive.value != hasPrivateSession) {
            isPrivateBrowsingActive.value = hasPrivateSession
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

    fun checkToShowMyShotOnBoarding() {
        if (!settings.eventHistory.contains(Settings.Event.ShowMyShotOnBoardingDialog)) {
            settings.eventHistory.add(Settings.Event.ShowMyShotOnBoardingDialog)
            if (isMyShotOnBoardingPending.value != true) {
                isMyShotOnBoardingPending.value = true
            }
        }
    }

    fun onMyShotOnBoardingDisplayed() {
        settings.setNightModeSpotlight(false)
        if (isMyShotOnBoardingPending.value != false) {
            isMyShotOnBoardingPending.value = false
        }
    }

    fun toggleBookmark() {
        if (isCurrentUrlBookmarked.value == true) {
            deleteBookmark()
            showToast.value = ToastMessage(R.string.bookmark_removed, duration = LENGTH_LONG)
        } else {
            val itemId = addBookmark()
            if (itemId != null) {
                bookmarkAdded.value = itemId
            }
        }
    }

    private fun deleteBookmark() {
        currentUrl.value?.let { url ->
            bookmarkRepo.deleteBookmarksByUrl(url)
        }
    }

    private fun addBookmark(): String? {
        var bookmarkId: String? = null
        val url = currentUrl.value
        if (!url.isNullOrEmpty()) {
            val title = currentTitle.value.takeUnless { it.isNullOrEmpty() }
                    ?: UrlUtils.stripCommonSubdomains(UrlUtils.stripHttp(url))
            bookmarkId = bookmarkRepo.addBookmark(title, url)
        }

        return bookmarkId
    }

    fun onTurboModeToggled() {
        val toEnable = !settings.shouldUseTurboMode()
        settings.setTurboMode(toEnable)
        showToast.value = ToastMessage(if (toEnable) R.string.message_enable_turbo_mode else R.string.message_disable_turbo_mode)
        TelemetryWrapper.menuTurboChangeTo(toEnable)
    }

    fun onBlockImageToggled() {
        val toEnable = !settings.shouldBlockImages()
        settings.setBlockImages(toEnable)
        showToast.value = ToastMessage(if (toEnable) R.string.message_enable_block_image else R.string.message_disable_block_image)
        TelemetryWrapper.menuBlockImageChangeTo(toEnable)
    }

    fun onDoScreenshot(telemetryData: ScreenCaptureTelemetryData) {
        doScreenshot.value = telemetryData
    }

    fun showScreenshots() {
        settings.setHasUnreadMyShot(false)
        showScreenshots.call()
        TelemetryWrapper.clickMenuCapture()
    }

    fun onSurveyNotificationPosted() {
        settings.eventHistory.add(Settings.Event.PostSurveyNotification)
    }

    fun checkToDriveDefaultBrowser() {
        if (settings.isDefaultBrowserSettingDidShow) {
            // We don't need to accumulate the count after we've displayed the default browser promotion
            return
        }
        settings.addMenuPreferenceClickCount()

        val count = settings.menuPreferenceClickCount
        val threshold = AppConfigWrapper.getDriveDefaultBrowserFromMenuSettingThreshold()
        // even if user above threshold and not set-as-default-browser, still don't show notification.
        if (count == threshold && !browsers.isDefaultBrowser) {
            driveDefaultBrowser.call()
        }
    }

    fun onSessionStarted() {
        TelemetryWrapper.startVerticalProcess(TelemetryWrapper.Extra_Value.ALL)
    }

    fun onSessionEnded() {
        TelemetryWrapper.endVerticalProcess(TelemetryWrapper.Extra_Value.ALL, lastUrlLoadTime)
        lastUrlLoadTime = 0
    }

    data class NightModeSettings(
        val isEnabled: Boolean,
        val brightness: Float
    )

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
