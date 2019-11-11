package org.mozilla.focus.autobot

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.mozilla.focus.R
import org.mozilla.focus.utils.AndroidTestUtils
import tools.fastlane.screengrab.Screengrab

interface MenuAutomation {

    fun clickHomeMenu()

    fun clickBrowserMenu()

    /** Click menu item **/
    fun clickMenuBookmarks()

    fun clickMenuDownloads()
    fun clickMenuHistory()
    fun clickMenuMyShots()
    fun clickMenuTurboMode()
    fun clickMenuBlockImages()
    fun clickMenuClearCache()
    fun clickMenuSettings()
    fun clickExitApp()

    /** Click panel item **/
    fun clickPanelDownload()

    fun clickPanelHistory()
    fun clickPanelMyShots()

    fun pressBack()

    fun takeScreenshotViaFastlane(fileName: String)
}

/** Some common menu relevant actions **/
open class MenuRobot : MenuAutomation {

    /** Open menu **/
    override fun clickHomeMenu() = AndroidTestUtils.tapHomeMenuButton()

    override fun clickBrowserMenu() = AndroidTestUtils.tapBrowserMenuButton()

    /** Click menu item **/
    override fun clickMenuBookmarks() {
        onView(withId(R.id.menu_bookmark))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
    }

    override fun clickMenuDownloads() {
        onView(withId(R.id.menu_download))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
    }

    override fun clickMenuHistory() {
        onView(withId(R.id.menu_history))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
    }

    override fun clickMenuMyShots() {
        onView(withId(R.id.menu_screenshots))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
    }

    override fun clickMenuTurboMode() {
        onView(withId(R.id.menu_turbomode))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
    }

    override fun clickMenuBlockImages() {
        onView(withId(R.id.menu_blockimg))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
    }

    override fun clickMenuClearCache() {
        onView(withId(R.id.menu_delete))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
    }

    override fun clickMenuSettings() {
        onView(withId(R.id.menu_preferences))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
    }

    override fun clickExitApp() {
        onView(withId(R.id.menu_exit))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
    }

    /** Click panel item **/
    override fun clickPanelDownload() {
        onView(withId(R.id.downloads))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
    }

    override fun clickPanelHistory() {
        onView(withId(R.id.history))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
    }
    override fun clickPanelMyShots() {
        onView(withId(R.id.screenshots))
                .inRoot(RootMatchers.isDialog())
                .perform(click())
    }

    override fun pressBack() = Espresso.pressBack()

    override fun takeScreenshotViaFastlane(fileName: String) {
        Screengrab.screenshot(fileName)
    }
}