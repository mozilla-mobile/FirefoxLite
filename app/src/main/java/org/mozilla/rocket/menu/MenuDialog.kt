package org.mozilla.rocket.menu

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat.startActivity
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.Lazy
import org.mozilla.fileutils.FileUtils
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value.MENU
import org.mozilla.focus.utils.FormatUtils
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.MenuItemAdapter
import org.mozilla.rocket.chrome.MenuViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.content.view.MenuLayout
import org.mozilla.rocket.extension.map
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.switchFrom
import org.mozilla.rocket.extension.toActivity
import org.mozilla.rocket.extension.toFragmentActivity
import org.mozilla.rocket.nightmode.AdjustBrightnessDialog
import javax.inject.Inject

class MenuDialog : BottomSheetDialog {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>
    @Inject
    lateinit var menuViewModelCreator: Lazy<MenuViewModel>

    private lateinit var menuViewModel: MenuViewModel
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var settings: Settings
    private lateinit var menuItemAdapter: MenuItemAdapter
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter

    private lateinit var contentLayout: View

    constructor(context: Context) : super(context)
    constructor(context: Context, @StyleRes theme: Int) : super(context, theme)

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        val activity = context.toFragmentActivity()
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
        menuViewModel = getActivityViewModel(menuViewModelCreator)
        settings = Settings.getInstance(context)

        initLayout()
        initMenu()
        initBottomBar()

        chromeViewModel.updateMenu.observe(activity, Observer { updateMenu() })
        chromeViewModel.showAdjustBrightness.observe(activity, Observer { showAdjustBrightness() })
    }

    private fun initLayout() {
        contentLayout = layoutInflater.inflate(R.layout.bottom_sheet_main_menu, null)
        setContentView(contentLayout)
    }

    private fun initMenu() {
        val menuLayout = contentLayout.findViewById<MenuLayout>(R.id.menu_layout)
        menuLayout.setOnItemClickListener(object : MenuLayout.OnItemClickListener {
            override fun onItemClick(type: Int, position: Int) {
                cancel()
                when (type) {
                    MenuItemAdapter.TYPE_BOOKMARKS -> {
                        chromeViewModel.showBookmarks.call()
                        TelemetryWrapper.clickMenuBookmark()
                    }
                    MenuItemAdapter.TYPE_DOWNLOADS -> {
                        chromeViewModel.showDownloadPanel.call()
                        TelemetryWrapper.clickMenuDownload()
                    }
                    MenuItemAdapter.TYPE_HISTORY -> {
                        chromeViewModel.showHistory.call()
                        TelemetryWrapper.clickMenuHistory()
                    }
                    MenuItemAdapter.TYPE_SCREENSHOTS -> chromeViewModel.showScreenshots()
                    MenuItemAdapter.TYPE_TURBO_MODE -> chromeViewModel.onTurboModeToggled()
                    MenuItemAdapter.TYPE_PRIVATE_BROWSING -> {
                        chromeViewModel.togglePrivateMode.call()
                        TelemetryWrapper.togglePrivateMode(true)
                    }
                    MenuItemAdapter.TYPE_NIGHT_MODE -> chromeViewModel.onNightModeToggled()
                    MenuItemAdapter.TYPE_BLOCK_IMAGE -> chromeViewModel.onBlockImageToggled()
                    MenuItemAdapter.TYPE_FIND_IN_PAGE -> chromeViewModel.showFindInPage.call()
                    MenuItemAdapter.TYPE_CLEAR_CACHE -> {
                        onDeleteClicked()
                        TelemetryWrapper.clickMenuClearCache()
                    }
                    MenuItemAdapter.TYPE_PREFERENCES -> {
                        chromeViewModel.checkToDriveDefaultBrowser()
                        chromeViewModel.openPreference.call()
                        TelemetryWrapper.clickMenuSettings()
                    }
                    MenuItemAdapter.TYPE_EXIT_APP -> {
                        chromeViewModel.exitApp.call()
                        TelemetryWrapper.clickMenuExit()
                    }
                    else -> throw IllegalArgumentException("Unhandled menu item, type: $type")
                }
            }
        })
        menuLayout.setOnItemLongClickListener(object : MenuLayout.OnItemLongClickListener {
            override fun onItemLongClick(type: Int, position: Int): Boolean {
                when (type) {
                    MenuItemAdapter.TYPE_NIGHT_MODE -> chromeViewModel.adjustNightMode()
                    else -> return false
                }

                return true
            }
        })
        menuItemAdapter = MenuItemAdapter(menuLayout, MenuItemAdapter.Theme.Light)
        val activity = context.toFragmentActivity()
        menuViewModel.menuItems.nonNullObserve(activity, menuItemAdapter::setItems)

        chromeViewModel.isTurboModeEnabled.switchFrom(menuViewModel.menuItems)
                .observe(activity, Observer { menuItemAdapter.setTurboMode(it == true) })
        chromeViewModel.isNightMode.switchFrom(menuViewModel.menuItems)
                .observe(activity, Observer { menuItemAdapter.setNightMode(it?.isEnabled == true) })
        chromeViewModel.isBlockImageEnabled.switchFrom(menuViewModel.menuItems)
                .observe(activity, Observer { menuItemAdapter.setBlockImageEnabled(it == true) })
        chromeViewModel.hasUnreadScreenshot.switchFrom(menuViewModel.menuItems)
                .observe(activity, Observer { menuItemAdapter.setUnreadScreenshot(it == true) })
        chromeViewModel.navigationState.switchFrom(menuViewModel.menuItems)
                .map { navigationState -> navigationState.isBrowser }
                .observe(activity, Observer { menuItemAdapter.setFindInPageEnabled(it) })
        chromeViewModel.isPrivateBrowsingActive.switchFrom(menuViewModel.menuItems)
                .observe(activity, Observer { menuItemAdapter.setPrivateBrowsingActive(it == true) })
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
                    BottomBarItemAdapter.TYPE_HOME -> {
                        chromeViewModel.showNewTab.call()
                        TelemetryWrapper.clickAddTabToolbar(MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_SEARCH -> {
                        chromeViewModel.showUrlInput.call()
                        TelemetryWrapper.clickToolbarSearch(MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_CAPTURE -> chromeViewModel.onDoScreenshot(ChromeViewModel.ScreenCaptureTelemetryData(MENU, position))
                    BottomBarItemAdapter.TYPE_PIN_SHORTCUT -> {
                        chromeViewModel.pinShortcut.call()
                        TelemetryWrapper.clickAddToHome(MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_BOOKMARK -> {
                        val isActivated = bottomBarItemAdapter.getItem(BottomBarItemAdapter.TYPE_BOOKMARK)?.view?.isActivated == true
                        TelemetryWrapper.clickToolbarBookmark(!isActivated, MENU, position)
                        chromeViewModel.toggleBookmark()
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
        menuViewModel.bottomItems.nonNullObserve(activity) { bottomItems ->
            bottomBarItemAdapter.setItems(bottomItems)
            hidePinShortcutButtonIfNotSupported()
        }

        menuViewModel.isBottomBarEnabled.switchFrom(menuViewModel.bottomItems)
                .observe(activity, Observer { bottomBarItemAdapter.setEnabled(it == true) })
        chromeViewModel.tabCount.switchFrom(menuViewModel.bottomItems)
                .observe(activity, Observer { bottomBarItemAdapter.setTabCount(it ?: 0) })
        chromeViewModel.isRefreshing.switchFrom(menuViewModel.bottomItems)
                .observe(activity, Observer { bottomBarItemAdapter.setRefreshing(it == true) })
        chromeViewModel.canGoForward.switchFrom(menuViewModel.bottomItems)
                .observe(activity, Observer { bottomBarItemAdapter.setCanGoForward(it == true) })
        chromeViewModel.isCurrentUrlBookmarked.switchFrom(menuViewModel.bottomItems)
                .observe(activity, Observer { bottomBarItemAdapter.setBookmark(it == true) })
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
        chromeViewModel.onMenuShown()
    }

    // This method exists for helping the communication between chromeViewMode and menuViewModel
    // Need to see if there's a better way to deal with this
    private fun updateMenu() {
        val hasFocus = chromeViewModel.navigationState.value?.isBrowser == true
        menuViewModel.onTabFocusChanged(hasFocus)
        bottomBarItemAdapter.setCanGoForward(hasFocus && chromeViewModel.canGoForward.value == true)

        menuViewModel.refresh()
    }

    private fun showAdjustBrightness() {
        startActivity(context, AdjustBrightnessDialog.Intents.getStartIntentFromMenu(context), null)
    }
}