package org.mozilla.rocket.menu

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.StyleRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.Lazy
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.btn_private_browsing
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.img_private_mode
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.img_screenshots
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.menu_bookmark
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.menu_delete
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.menu_download
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.menu_exit
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.menu_history
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.menu_night_mode
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.menu_preferences
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.menu_screenshots
import kotlinx.android.synthetic.main.bottom_sheet_home_menu.view.night_mode_switch
import org.mozilla.fileutils.FileUtils
import org.mozilla.focus.R
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.FormatUtils
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.extension.toFragmentActivity
import org.mozilla.rocket.nightmode.AdjustBrightnessDialog
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
        observeChromeAction()
    }

    private fun initLayout() {
        contentLayout = layoutInflater.inflate(R.layout.bottom_sheet_home_menu, null)
        initMenuTabs(contentLayout)
        initMenuItems(contentLayout)
        setContentView(contentLayout)
    }

    private fun initMenuTabs(contentLayout: View) {
        val activity = context.toFragmentActivity()
        contentLayout.apply {
            chromeViewModel.hasUnreadScreenshot.observe(activity, Observer {
                img_screenshots.isActivated = it
            })

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

    private fun initMenuItems(contentLayout: View) {
        val activity = context.toFragmentActivity()
        contentLayout.apply {
            chromeViewModel.isNightMode.observe(activity, Observer { nightModeSettings ->
                night_mode_switch.isChecked = nightModeSettings.isEnabled
            })
            chromeViewModel.isPrivateBrowsingActive.observe(activity, Observer {
                img_private_mode.isActivated = it
            })

            btn_private_browsing.setOnClickListener {
                cancel()
                chromeViewModel.togglePrivateMode.call()
                TelemetryWrapper.togglePrivateMode(true)
            }
            menu_night_mode.setOnClickListener {
                chromeViewModel.adjustNightMode()
            }
            night_mode_switch.setOnCheckedChangeListener { _, isChecked ->
                val needToUpdate = isChecked != (chromeViewModel.isNightMode.value?.isEnabled == true)
                if (needToUpdate) {
                    chromeViewModel.onNightModeToggled()
                }
            }
            menu_preferences.setOnClickListener {
                cancel()
                chromeViewModel.checkToDriveDefaultBrowser()
                chromeViewModel.openPreference.call()
                TelemetryWrapper.clickMenuSettings()
            }
            menu_delete.setOnClickListener {
                cancel()
                onDeleteClicked()
                TelemetryWrapper.clickMenuClearCache()
            }
            menu_exit.setOnClickListener {
                cancel()
                chromeViewModel.exitApp.call()
                TelemetryWrapper.clickMenuExit()
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
}