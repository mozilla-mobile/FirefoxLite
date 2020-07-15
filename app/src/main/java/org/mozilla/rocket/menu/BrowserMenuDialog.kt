package org.mozilla.rocket.menu

import android.content.Context
import android.graphics.Outline
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ScrollView
import android.widget.Toast
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.Lazy
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.block_images_switch
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.content_layout
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.img_screenshots
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.menu_blockimg
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.menu_bookmark
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.menu_delete
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.menu_download
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.menu_exit
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.menu_find_in_page
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.menu_history
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.menu_night_mode
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.menu_pin_shortcut
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.menu_preferences
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.menu_screenshots
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.menu_turbomode
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.night_mode_switch
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.scroll_view
import kotlinx.android.synthetic.main.bottom_sheet_browser_menu.view.turbomode_switch
import org.mozilla.fileutils.FileUtils
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.FormatUtils
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.MenuViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.switchFrom
import org.mozilla.rocket.extension.toFragmentActivity
import org.mozilla.rocket.nightmode.AdjustBrightnessDialog
import javax.inject.Inject

class BrowserMenuDialog : BottomSheetDialog {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>
    @Inject
    lateinit var menuViewModelCreator: Lazy<MenuViewModel>

    private lateinit var menuViewModel: MenuViewModel
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter

    private lateinit var rootView: View

    private val uiHandler = Handler(Looper.getMainLooper())

    constructor(context: Context) : super(context)
    constructor(context: Context, @StyleRes theme: Int) : super(context, theme)

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
        menuViewModel = getActivityViewModel(menuViewModelCreator)

        initLayout()
        observeChromeAction()
        setCancelable(false)
        setCanceledOnTouchOutside(true)
    }

    override fun dismiss() {
        if (::rootView.isInitialized) {
            rootView.scroll_view.fullScroll(ScrollView.FOCUS_UP)
        }
        super.dismiss()
    }

    private fun initLayout() {
        rootView = layoutInflater.inflate(R.layout.bottom_sheet_browser_menu, null)
        rootView.content_layout.apply {
            outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setRoundRect(0, 0, view.width, view.height, resources.getDimension(R.dimen.menu_corner_radius))
                }
            }
            clipToOutline = true
        }
        initMenuTabs(rootView)
        initMenuItems(rootView)
        initBottomBar()
        setContentView(rootView)
    }

    private fun initMenuTabs(contentLayout: View) {
        val activity = context.toFragmentActivity()
        contentLayout.apply {
            chromeViewModel.hasUnreadScreenshot.observe(activity, Observer {
                img_screenshots.isActivated = it
            })

            menu_screenshots.setOnClickListener {
                postDelayClickEvent {
                    cancel()
                    chromeViewModel.showScreenshots()
                }
            }
            menu_bookmark.setOnClickListener {
                postDelayClickEvent {
                    cancel()
                    chromeViewModel.showBookmarks.call()
                    TelemetryWrapper.clickMenuBookmark()
                }
            }
            menu_history.setOnClickListener {
                postDelayClickEvent {
                    cancel()
                    chromeViewModel.showHistory.call()
                    TelemetryWrapper.clickMenuHistory()
                }
            }
            menu_download.setOnClickListener {
                postDelayClickEvent {
                    cancel()
                    chromeViewModel.showDownloadPanel.call()
                    TelemetryWrapper.clickMenuDownload()
                }
            }
        }
    }

    private fun initMenuItems(contentLayout: View) {
        val activity = context.toFragmentActivity()
        contentLayout.apply {
            chromeViewModel.isTurboModeEnabled.observe(activity, Observer {
                turbomode_switch.isChecked = it
            })
            chromeViewModel.isBlockImageEnabled.observe(activity, Observer {
                block_images_switch.isChecked = it
            })
            chromeViewModel.isNightMode.observe(activity, Observer { nightModeSettings ->
                night_mode_switch.isChecked = nightModeSettings.isEnabled
            })

            menu_find_in_page.setOnClickListener {
                postDelayClickEvent {
                    cancel()
                    chromeViewModel.showFindInPage.call()
                }
            }
            menu_pin_shortcut.setOnClickListener {
                postDelayClickEvent {
                    cancel()
                    chromeViewModel.pinShortcut.call()
                    TelemetryWrapper.clickMenuPinShortcut()
                }
            }
            menu_night_mode.setOnClickListener {
                chromeViewModel.adjustNightMode()
            }
            menu_turbomode.setOnClickListener { turbomode_switch.toggle() }
            turbomode_switch.setOnCheckedChangeListener { _, isChecked ->
                val needToUpdate = isChecked != (chromeViewModel.isTurboModeEnabled.value == true)
                if (needToUpdate) {
                    chromeViewModel.onTurboModeToggled()
                }
            }
            menu_blockimg.setOnClickListener { block_images_switch.toggle() }
            block_images_switch.setOnCheckedChangeListener { _, isChecked ->
                val needToUpdate = isChecked != (chromeViewModel.isBlockImageEnabled.value == true)
                if (needToUpdate) {
                    chromeViewModel.onBlockImageToggled()
                }
            }
            night_mode_switch.setOnCheckedChangeListener { _, isChecked ->
                val needToUpdate = isChecked != (chromeViewModel.isNightMode.value?.isEnabled == true)
                if (needToUpdate) {
                    chromeViewModel.onNightModeToggled()
                }
            }
            menu_preferences.setOnClickListener {
                postDelayClickEvent {
                    cancel()
                    chromeViewModel.checkToDriveDefaultBrowser()
                    chromeViewModel.openPreference.call()
                    TelemetryWrapper.clickMenuSettings()
                }
            }
            menu_delete.setOnClickListener {
                postDelayClickEvent {
                    cancel()
                    onDeleteClicked()
                    TelemetryWrapper.clickMenuClearCache()
                }
            }
            menu_exit.setOnClickListener {
                postDelayClickEvent {
                    cancel()
                    chromeViewModel.exitApp.call()
                    TelemetryWrapper.clickMenuExit()
                }
            }
        }
    }

    private fun onDeleteClicked() {
        val diff = FileUtils.clearCache(context)
        val stringId = if (diff < 0) R.string.message_clear_cache_fail else R.string.message_cleared_cached
        val msg = context.getString(stringId, FormatUtils.getReadableStringFromFileSize(diff))
        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
    }

    private fun observeChromeAction() {
        val activity = context.toFragmentActivity()
        chromeViewModel.showAdjustBrightness.observe(activity, Observer { showAdjustBrightness() })
    }

    private fun showAdjustBrightness() {
        ContextCompat.startActivity(context, AdjustBrightnessDialog.Intents.getStartIntentFromMenu(context), null)
    }

    private fun initBottomBar() {
        val bottomBar = rootView.findViewById<BottomBar>(R.id.menu_bottom_bar)
        bottomBar.setOnItemClickListener(object : BottomBar.OnItemClickListener {
            override fun onItemClick(type: Int, position: Int) {
                cancel()
                when (type) {
                    BottomBarItemAdapter.TYPE_TAB_COUNTER -> {
                        chromeViewModel.showTabTray.call()
                        TelemetryWrapper.showTabTrayToolbar(TelemetryWrapper.Extra_Value.MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_MENU -> {
                        chromeViewModel.showBrowserMenu.call()
                        TelemetryWrapper.showMenuToolbar(TelemetryWrapper.Extra_Value.MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_HOME -> {
                        chromeViewModel.showNewTab.call()
                        TelemetryWrapper.clickAddTabToolbar(TelemetryWrapper.Extra_Value.MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_SEARCH -> {
                        chromeViewModel.showUrlInput.call()
                        TelemetryWrapper.clickToolbarSearch(TelemetryWrapper.Extra_Value.MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_CAPTURE -> chromeViewModel.onDoScreenshot(ChromeViewModel.ScreenCaptureTelemetryData(TelemetryWrapper.Extra_Value.MENU, position))
                    BottomBarItemAdapter.TYPE_PIN_SHORTCUT -> {
                        chromeViewModel.pinShortcut.call()
                        TelemetryWrapper.clickAddToHome(TelemetryWrapper.Extra_Value.MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_BOOKMARK -> {
                        val isActivated = bottomBarItemAdapter.getItem(BottomBarItemAdapter.TYPE_BOOKMARK)?.view?.isActivated == true
                        TelemetryWrapper.clickToolbarBookmark(!isActivated, TelemetryWrapper.Extra_Value.MENU, position)
                        chromeViewModel.toggleBookmark()
                    }
                    BottomBarItemAdapter.TYPE_REFRESH -> {
                        chromeViewModel.refreshOrStop.call()
                        TelemetryWrapper.clickToolbarReload(TelemetryWrapper.Extra_Value.MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_SHARE -> {
                        chromeViewModel.share.call()
                        TelemetryWrapper.clickToolbarShare(TelemetryWrapper.Extra_Value.MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_NEXT -> {
                        chromeViewModel.goNext.call()
                        TelemetryWrapper.clickToolbarForward(TelemetryWrapper.Extra_Value.MENU, position)
                    }
                    BottomBarItemAdapter.TYPE_BACK -> {
                        chromeViewModel.goBack.call()
                        TelemetryWrapper.clickToolbarBack(position)
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

        chromeViewModel.tabCount.switchFrom(menuViewModel.bottomItems)
                .observe(activity, Observer { bottomBarItemAdapter.setTabCount(it ?: 0) })
        chromeViewModel.isRefreshing.switchFrom(menuViewModel.bottomItems)
                .observe(activity, Observer { bottomBarItemAdapter.setRefreshing(it == true) })
        chromeViewModel.canGoForward.switchFrom(menuViewModel.bottomItems)
                .observe(activity, Observer { bottomBarItemAdapter.setCanGoForward(it == true) })
        chromeViewModel.canGoBack.switchFrom(menuViewModel.bottomItems)
                .observe(activity, Observer { bottomBarItemAdapter.setCanGoBack(it == true) })
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

    /**
     * Post delay click event to wait the clicking feedback shows
     */
    private fun postDelayClickEvent(action: () -> Unit) {
        uiHandler.postDelayed({
            action()
        }, 150)
    }

    fun destroy() {
        uiHandler.removeCallbacksAndMessages(null)
    }
}