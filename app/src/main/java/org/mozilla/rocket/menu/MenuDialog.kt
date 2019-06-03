package org.mozilla.rocket.menu

import android.arch.lifecycle.Observer
import android.content.Context
import android.content.Intent
import android.support.annotation.StyleRes
import android.support.design.widget.BottomSheetDialog
import android.support.v4.content.ContextCompat.startActivity
import android.support.v4.content.pm.ShortcutManagerCompat
import android.view.View
import android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
import android.widget.Toast
import org.mozilla.fileutils.FileUtils
import org.mozilla.focus.Inject
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value.MENU
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.focus.utils.DialogUtils
import org.mozilla.focus.utils.FormatUtils
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.MenuViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.toActivity
import org.mozilla.rocket.extension.toFragmentActivity
import org.mozilla.rocket.nightmode.AdjustBrightnessDialog
import org.mozilla.rocket.privately.PrivateMode
import org.mozilla.rocket.privately.PrivateModeActivity

class MenuDialog : BottomSheetDialog {

    private val menuViewModel: MenuViewModel
    private val chromeViewModel: ChromeViewModel
    private var settings: Settings
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter

    private lateinit var contentLayout: View

    private lateinit var myshotIndicator: View
    private lateinit var myshotButton: View
    private lateinit var nightModeButton: View
    private lateinit var turboModeButton: View
    private lateinit var blockImageButton: View
    private lateinit var privateModeIndicator: View

    constructor(context: Context) : super(context)
    constructor(context: Context, @StyleRes theme: Int) : super(context, theme)

    init {
        val activity = context.toFragmentActivity()
        chromeViewModel = Inject.obtainChromeViewModel(activity)
        menuViewModel = Inject.obtainMenuViewModel(activity)
        settings = Settings.getInstance(context)

        initLayout()
        initMenu()
        initBottomBar()

        chromeViewModel.updateMenu.observe(activity, Observer { updateMenu() })
    }

    private fun initLayout() {
        contentLayout = layoutInflater.inflate(R.layout.bottom_sheet_main_menu, null)
        setContentView(contentLayout)
    }

    private fun initMenu() {
        myshotIndicator = contentLayout.findViewById<View>(R.id.menu_my_shot_unread)
        privateModeIndicator = contentLayout.findViewById<View>(R.id.menu_private_mode_indicator)
        myshotButton = contentLayout.findViewById<View>(R.id.menu_screenshots)

        turboModeButton = contentLayout.findViewById<View>(R.id.menu_turbomode)
        turboModeButton.isSelected = settings.shouldUseTurboMode()

        blockImageButton = contentLayout.findViewById<View>(R.id.menu_blockimg)
        blockImageButton.isSelected = settings.shouldBlockImages()

        nightModeButton = contentLayout.findViewById<View>(R.id.menu_night_mode)
        nightModeButton.setOnLongClickListener(onLongClickListener)
        nightModeButton.isSelected = settings.isNightModeEnable
    }

    private fun initBottomBar() {
        val bottomBar = contentLayout.findViewById<BottomBar>(R.id.menu_bottom_bar)
        bottomBar.setOnItemClickListener(object : BottomBar.OnItemClickListener {
            override fun onItemClick(type: Int, position: Int) {
                cancel()
                when (type) {
                    BottomBarItemAdapter.TYPE_TAB_COUNTER -> {
                        chromeViewModel.showTabTray.call()
                        TelemetryWrapper.showTabTrayToolbar(MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_MENU -> {
                        chromeViewModel.showMenu.call()
                        TelemetryWrapper.showMenuToolbar(MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_NEW_TAB -> {
                        chromeViewModel.showNewTab.call()
                        TelemetryWrapper.clickAddTabToolbar(MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_SEARCH -> {
                        chromeViewModel.showUrlInput.call()
                        TelemetryWrapper.clickToolbarSearch(MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_CAPTURE -> chromeViewModel.doScreenshot.setValue(ChromeViewModel.ScreenCaptureTelemetryData(MENU, position))
                    BottomBarItemAdapter.TYPE_PIN_SHORTCUT -> {
                        chromeViewModel.pinShortcut.call()
                        TelemetryWrapper.clickAddToHome(MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_BOOKMARK -> {
                        val isActivated = bottomBarItemAdapter.getItem(BottomBarItemAdapter.TYPE_BOOKMARK)?.view?.isActivated == true
                        TelemetryWrapper.clickToolbarBookmark(!isActivated, MENU, position)
                        chromeViewModel.toggleBookmark.call()
                    }
                    BottomBarItemAdapter.TYPE_REFRESH -> {
                        chromeViewModel.refreshOrStop.call()
                        TelemetryWrapper.clickToolbarReload(MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_SHARE -> {
                        chromeViewModel.share.call()
                        TelemetryWrapper.clickToolbarShare(MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_NEXT -> {
                        chromeViewModel.goNext.call()
                        TelemetryWrapper.clickToolbarForward(MENU, position)
                    }
                    else -> throw IllegalArgumentException("Unhandled bottom bar item, type: $type")
                } // move Telemetry to ScreenCaptureTask doInBackground() cause we need to init category first.
            }
        })
        val activity = context.toFragmentActivity()
        bottomBarItemAdapter = BottomBarItemAdapter(bottomBar, BottomBarItemAdapter.Theme.Light)
        menuViewModel.bottomItems.nonNullObserve(context.toFragmentActivity()) { bottomItems ->
            bottomBarItemAdapter.setItems(bottomItems)
            hidePinShortcutButtonIfNotSupported()
        }
        menuViewModel.isBottomBarEnabled.observe(activity, Observer { bottomBarItemAdapter.setEnabled(it == true) })

        chromeViewModel.tabCount.observe(activity, Observer { bottomBarItemAdapter.setTabCount(it ?: 0) })
        chromeViewModel.isRefreshing.observe(activity, Observer { bottomBarItemAdapter.setRefreshing(it == true) })
        chromeViewModel.canGoForward.observe(activity, Observer { bottomBarItemAdapter.setCanGoForward(it == true) })
        chromeViewModel.isCurrentUrlBookmarked.observe(activity, Observer { bottomBarItemAdapter.setBookmark(it == true) })
    }

    private fun hidePinShortcutButtonIfNotSupported() {
        val requestPinShortcutSupported = ShortcutManagerCompat.isRequestPinShortcutSupported(context)
        if (!requestPinShortcutSupported) {
            val pinShortcutItem = bottomBarItemAdapter.getItem(BottomBarItemAdapter.TYPE_PIN_SHORTCUT)
            pinShortcutItem?.view?.apply {
                visibility = View.GONE
            }
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        updateMenu()
    }

    private fun updateMenu() {
        val settings = settings

        turboModeButton.isSelected = settings.shouldUseTurboMode()
        blockImageButton.isSelected = settings.shouldBlockImages()
        nightModeButton.isSelected = settings.isNightModeEnable

        val showUnread = AppConfigWrapper.getMyshotUnreadEnabled() && settings.hasUnreadMyShot()
        myshotIndicator.visibility = if (showUnread) View.VISIBLE else View.GONE
        privateModeIndicator.visibility = if (PrivateMode.hasPrivateSession(context)) View.VISIBLE else View.GONE

        if (settings.showNightModeSpotlight()) {
            settings.setNightModeSpotlight(false)
            nightModeButton.post {
                DialogUtils.showSpotlight(
                        context.toActivity(),
                        nightModeButton,
                        {},
                        R.string.night_mode_on_boarding_message
                )
            }
        }

        val hasFocus = chromeViewModel.navigationState.value?.isBrowser == true
        menuViewModel.onTabFocusChanged(hasFocus)
        bottomBarItemAdapter.setCanGoForward(hasFocus && chromeViewModel.canGoForward.value == true)

        val hasNewConfig = menuViewModel.refresh()
        if (hasNewConfig) {
            chromeViewModel.invalidate()
        }
    }

    private var onLongClickListener: View.OnLongClickListener = View.OnLongClickListener { v ->
        cancel()
        when (v.id) {
            R.id.menu_night_mode -> {
                chromeViewModel.onNightModeChanged(true)
                settings.setNightMode(true)
                showAdjustBrightness()

                true
            }
            else -> throw RuntimeException("Unknown id in menu, OnLongClickListener() is only for known ids")
        }
    }

    private fun showAdjustBrightness() {
        startActivity(context, AdjustBrightnessDialog.Intents.getStartIntentFromMenu(context), null)
    }

    private fun showAdjustBrightnessIfNeeded(settings: Settings) {
        val currentBrightness = settings.nightModeBrightnessValue
        if (currentBrightness == BRIGHTNESS_OVERRIDE_NONE) {
            // First time turn on
            settings.nightModeBrightnessValue = AdjustBrightnessDialog.Constants.DEFAULT_BRIGHTNESS
            showAdjustBrightness()
            settings.setNightModeSpotlight(true)
        }
    }

    fun onMenuItemClicked(v: View) {
        val stringResource: Int
        if (!v.isEnabled) {
            return
        }
        when (v.id) {
            R.id.menu_blockimg -> {
                //  Toggle
                val blockingImages = !settings.shouldBlockImages()
                settings.setBlockImages(blockingImages)

                v.isSelected = blockingImages
                stringResource = if (blockingImages) R.string.message_enable_block_image else R.string.message_disable_block_image
                Toast.makeText(context, stringResource, Toast.LENGTH_SHORT).show()

                TelemetryWrapper.menuBlockImageChangeTo(blockingImages)
            }
            R.id.menu_turbomode -> {
                //  Toggle
                val turboEnabled = !settings.shouldUseTurboMode()
                settings.setTurboMode(!settings.shouldUseTurboMode())

                v.isSelected = turboEnabled
                stringResource = if (turboEnabled) R.string.message_enable_turbo_mode else R.string.message_disable_turbo_mode
                Toast.makeText(context, stringResource, Toast.LENGTH_SHORT).show()

                TelemetryWrapper.menuTurboChangeTo(turboEnabled)
            }
            R.id.btn_private_browsing -> {
                val intent = Intent(context, PrivateModeActivity::class.java)
                startActivity(context, intent, null)
                overridePendingTransition(R.anim.tab_transition_fade_in, R.anim.tab_transition_fade_out)
                TelemetryWrapper.togglePrivateMode(true)
            }
            R.id.menu_night_mode -> {
                val settings = settings
                val nightModeEnabled = !settings.isNightModeEnable
                v.isSelected = nightModeEnabled

                chromeViewModel.onNightModeChanged(nightModeEnabled)
                settings.setNightMode(nightModeEnabled)
                showAdjustBrightnessIfNeeded(settings)

                TelemetryWrapper.menuNightModeChangeTo(nightModeEnabled)
            }
            R.id.menu_find_in_page -> chromeViewModel.showFindInPage.call()
            R.id.menu_delete -> {
                onDeleteClicked()
                TelemetryWrapper.clickMenuClearCache()
            }
            R.id.menu_download -> {
                chromeViewModel.showDownloadPanel.call()
                TelemetryWrapper.clickMenuDownload()
            }
            R.id.menu_history -> {
                chromeViewModel.showHistory.call()
                TelemetryWrapper.clickMenuHistory()
            }
            R.id.menu_screenshots -> {
                settings.setHasUnreadMyShot(false)
                chromeViewModel.showScreenshots.call()
                TelemetryWrapper.clickMenuCapture()
            }
            R.id.menu_preferences -> {
                chromeViewModel.driveDefaultBrowser.call()
                chromeViewModel.openPreference.call()
                TelemetryWrapper.clickMenuSettings()
            }
            R.id.menu_exit -> {
                chromeViewModel.exitApp.call()
                TelemetryWrapper.clickMenuExit()
            }
            R.id.menu_bookmark -> {
                chromeViewModel.showBookmarks.call()
                TelemetryWrapper.clickMenuBookmark()
            }
            else -> throw RuntimeException("Unknown id in menu, onMenuItemClicked() is only for" + " known ids")
        }
        cancel()
    }

    private fun overridePendingTransition(enterAnim: Int, exitAnim: Int) {
        context.toActivity().overridePendingTransition(enterAnim, exitAnim)
    }

    private fun onDeleteClicked() {
        val diff = FileUtils.clearCache(context)
        val stringId = if (diff < 0) R.string.message_clear_cache_fail else R.string.message_cleared_cached
        val msg = context.getString(stringId, FormatUtils.getReadableStringFromFileSize(diff))
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }
}