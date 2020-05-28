package org.mozilla.rocket.menu

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.annotation.StyleRes
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.mozilla.focus.R

class HomeMenuDialog : BottomSheetDialog {

    private lateinit var contentLayout: View

    constructor(context: Context) : super(context)
    constructor(context: Context, @StyleRes theme: Int) : super(context, theme)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initLayout()
    }

    private fun initLayout() {
        contentLayout = layoutInflater.inflate(R.layout.bottom_sheet_home_menu, null)
        setContentView(contentLayout)
    }
}