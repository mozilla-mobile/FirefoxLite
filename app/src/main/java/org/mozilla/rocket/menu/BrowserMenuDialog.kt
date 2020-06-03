package org.mozilla.rocket.menu

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.StyleRes
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.Lazy
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.chrome.BottomBarItemAdapter
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.MenuViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.view.BottomBar
import org.mozilla.rocket.extension.nonNullObserve
import org.mozilla.rocket.extension.switchFrom
import org.mozilla.rocket.extension.toFragmentActivity
import javax.inject.Inject

class BrowserMenuDialog : BottomSheetDialog {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>
    @Inject
    lateinit var menuViewModelCreator: Lazy<MenuViewModel>

    private lateinit var menuViewModel: MenuViewModel
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var bottomBarItemAdapter: BottomBarItemAdapter

    private lateinit var contentLayout: View

    constructor(context: Context) : super(context)
    constructor(context: Context, @StyleRes theme: Int) : super(context, theme)

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
        menuViewModel = getActivityViewModel(menuViewModelCreator)

        initLayout()
    }

    private fun initLayout() {
        contentLayout = layoutInflater.inflate(R.layout.bottom_sheet_browser_menu, null)
        initBottomBar()
        setContentView(contentLayout)
    }

    private fun initBottomBar() {
        val bottomBar = contentLayout.findViewById<BottomBar>(R.id.menu_bottom_bar)
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
}