package org.mozilla.rocket.menu

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.StyleRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import dagger.Lazy
import org.mozilla.focus.R
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.MenuViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class BrowserMenuDialog : BottomSheetDialog {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>
    @Inject
    lateinit var menuViewModelCreator: Lazy<MenuViewModel>

    private lateinit var menuViewModel: MenuViewModel
    private lateinit var chromeViewModel: ChromeViewModel

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
        setContentView(contentLayout)
    }
}