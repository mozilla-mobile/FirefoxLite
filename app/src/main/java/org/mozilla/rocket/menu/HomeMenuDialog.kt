package org.mozilla.rocket.menu

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.StyleRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.Lazy
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.menu_bookmark
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.menu_download
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.menu_history
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.menu_screenshots
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class HomeMenuDialog : BottomSheetDialog {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    private lateinit var chromeViewModel: ChromeViewModel

    private lateinit var contentLayout: View

    constructor(context: Context) : super(context)
    constructor(context: Context, @StyleRes theme: Int) : super(context, theme)

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)

        initLayout()
    }

    private fun initLayout() {
        contentLayout = layoutInflater.inflate(R.layout.bottom_sheet_home_menu, null)
        initMenuTabs(contentLayout)
        setContentView(contentLayout)
    }

    private fun initMenuTabs(contentLayout: View) {
        contentLayout.apply {
            menu_screenshots.setOnClickListener {
                cancel()
                chromeViewModel.showScreenshots()
            }
            menu_bookmark.setOnClickListener {
                cancel()
                chromeViewModel.showBookmarks.call()
                TelemetryWrapper.clickMenuBookmark()
            }
            menu_history.setOnClickListener {
                cancel()
                chromeViewModel.showHistory.call()
                TelemetryWrapper.clickMenuHistory()
            }
            menu_download.setOnClickListener {
                cancel()
                chromeViewModel.showDownloadPanel.call()
                TelemetryWrapper.clickMenuDownload()
            }
        }
    }
}