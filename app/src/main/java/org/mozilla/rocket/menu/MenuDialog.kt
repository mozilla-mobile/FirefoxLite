package org.mozilla.rocket.menu

import android.arch.lifecycle.Observer
import android.content.Context
import android.support.annotation.StyleRes
import android.support.design.widget.BottomSheetDialog
import android.support.v4.content.pm.ShortcutManagerCompat
import android.view.View
import org.mozilla.focus.Inject
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value.MENU
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.MenuViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.toFragmentActivity

class MenuDialog : BottomSheetDialog {

    private lateinit var menuViewModel: MenuViewModel
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter

    private lateinit var contentLayout: View

    constructor(context: Context) : super(context) {
        init()
    }

    constructor(context: Context, @StyleRes theme: Int) : super(context, theme) {
        init()
    }

    private fun init() {
        val activity = context.toFragmentActivity()
        chromeViewModel = Inject.obtainChromeViewModel(activity)
        menuViewModel = Inject.obtainMenuViewModel(activity)

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
        // TODO: init menu items
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
                        val isActivated = bottomBarItemAdapter.getItem(BottomBarItemAdapter.TYPE_BOOKMARK)!!.view!!.isActivated()
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
                }// move Telemetry to ScreenCaptureTask doInBackground() cause we need to init category first.
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

    private fun updateMenu() {
        val mainActivity = context.toFragmentActivity() as MainActivity
        val browserFragment = mainActivity.visibleBrowserFragment
        val hasFocus = browserFragment != null
        menuViewModel.onTabFocusChanged(hasFocus)
        bottomBarItemAdapter.setCanGoForward(chromeViewModel.canGoForward.value!!)

        val hasNewConfig = menuViewModel.refresh()
        if (hasNewConfig) {
            chromeViewModel.invalidate()
        }
    }
}