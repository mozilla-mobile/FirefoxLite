package org.mozilla.rocket.theme

import android.app.Dialog
import android.view.View
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.FragmentActivity
import org.mozilla.focus.R
import org.mozilla.focus.utils.Settings
import org.mozilla.rocket.home.HomeViewModel

class ThemeSettingDialogBuilder(
    private val activity: FragmentActivity,
    private val homeViewModel: HomeViewModel
) {

    private lateinit var themeButton1: ImageView
    private lateinit var themeButton2: ImageView
    private lateinit var themeButton3: ImageView
    private lateinit var themeButton4: ImageView
    private lateinit var themeButton5: ImageView

    fun show() {
        create().show()
    }

    private fun create(): Dialog {
        val customContentView = View.inflate(activity, R.layout.dialog_theme_setting, null)

        return AlertDialog.Builder(activity, R.style.ThemeSettingDialog)
            .setView(customContentView)
            .create()
            .also { dialog ->
                val currentTheme = ThemeManager.getCurrentThemeName(activity)
                val isDarkThemeEnable = Settings.getInstance(activity).isDarkThemeEnable
                customContentView.setOnClickListener { dialog.dismiss() }
                themeButton1 = customContentView.findViewById<ImageView>(R.id.theme_image_view_1).apply {
                    if (currentTheme == ThemeManager.ThemeSet.Default.name && !isDarkThemeEnable) {
                        isSelected = true
                    }
                    setOnClickListener { view ->
                        onThemeButtonClicked(view)
                    }
                }
                themeButton2 = customContentView.findViewById<ImageView>(R.id.theme_image_view_2).apply {
                    if (currentTheme == ThemeManager.ThemeSet.Theme2.name && !isDarkThemeEnable) {
                        isSelected = true
                    }
                    setOnClickListener { view ->
                        onThemeButtonClicked(view)
                    }
                }
                themeButton3 = customContentView.findViewById<ImageView>(R.id.theme_image_view_3).apply {
                    if (currentTheme == ThemeManager.ThemeSet.Theme3.name && !isDarkThemeEnable) {
                        isSelected = true
                    }
                    setOnClickListener { view ->
                        onThemeButtonClicked(view)
                    }
                }
                themeButton4 = customContentView.findViewById<ImageView>(R.id.theme_image_view_4).apply {
                    if (currentTheme == ThemeManager.ThemeSet.Theme4.name && !isDarkThemeEnable) {
                        isSelected = true
                    }
                    setOnClickListener { view ->
                        onThemeButtonClicked(view)
                    }
                }
                themeButton5 = customContentView.findViewById<ImageView>(R.id.theme_image_view_5).apply {
                    if (isDarkThemeEnable) {
                        isSelected = true
                    }
                    setOnClickListener { view ->
                        onThemeButtonClicked(view)
                    }
                }
                customContentView.findViewById<View>(R.id.action_done).apply {
                    setOnClickListener {
                        dialog.dismiss()
                    }
                }
            }
    }

    private fun onThemeButtonClicked(clickedThemeButton: View) {
        listOf(themeButton1, themeButton2, themeButton3, themeButton4, themeButton5).forEach { themeButton ->
            if (themeButton == clickedThemeButton) {
                if (!themeButton.isSelected) {
                    when (clickedThemeButton) {
                        themeButton1 -> homeViewModel.onThemeClicked(false, ThemeManager.ThemeSet.Default, THEME_DEFAULT_NAME)
                        themeButton2 -> homeViewModel.onThemeClicked(false, ThemeManager.ThemeSet.Theme2, THEME_2_NAME)
                        themeButton3 -> homeViewModel.onThemeClicked(false, ThemeManager.ThemeSet.Theme3, THEME_3_NAME)
                        themeButton4 -> homeViewModel.onThemeClicked(false, ThemeManager.ThemeSet.Theme4, THEME_4_NAME)
                        themeButton5 -> homeViewModel.onThemeClicked(true, ThemeManager.ThemeSet.Default, THEME_5_NAME)
                    }
                }
                themeButton.isSelected = true
            } else {
                themeButton.isSelected = false
            }
        }
    }

    companion object {
        private const val THEME_DEFAULT_NAME = "aqua"
        private const val THEME_2_NAME = "cyan"
        private const val THEME_3_NAME = "raspberry"
        private const val THEME_4_NAME = "iris"
        private const val THEME_5_NAME = "night"
    }
}