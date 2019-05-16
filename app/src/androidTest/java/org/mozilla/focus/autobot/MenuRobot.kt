package org.mozilla.focus.autobot

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.ViewInteraction
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.withId
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
open class MenuRobot: MenuAutomation {

    /** Open menu **/
    override fun clickHomeMenu() = AndroidTestUtils.tapHomeMenuButton()

    override fun clickBrowserMenu() = AndroidTestUtils.tapBrowserMenuButton()

    /** Click menu item **/
    override fun clickMenuBookmarks() {
        onView(withId(R.id.menu_bookmark)).perform(click())
    }

    override fun clickMenuDownloads() {
        onView(withId(R.id.menu_download)).perform(click())
    }

    override fun clickMenuHistory() {
        onView(withId(R.id.menu_history)).perform(click())
    }

    override fun clickMenuMyShots() {
        onView(withId(R.id.menu_screenshots)).perform(click())
    }

    override fun clickMenuTurboMode() {
        onView(withId(R.id.menu_turbomode)).perform(click())
    }

    override fun clickMenuBlockImages() {
        onView(withId(R.id.menu_blockimg)).perform(click())
    }

    override fun clickMenuClearCache() {
        onView(withId(R.id.menu_delete)).perform(click())
    }

    override fun clickMenuSettings() {
        onView(withId(R.id.menu_preferences)).perform(click())
    }

    override fun clickExitApp() {
        onView(withId(R.id.menu_exit)).perform(click())
    }

    /** Click panel item **/
    override fun clickPanelDownload() {
        onView(withId(R.id.downloads)).perform(click())
    }

    override fun clickPanelHistory() {
        onView(withId(R.id.history)).perform(click())
    }
    override fun clickPanelMyShots() {
        onView(withId(R.id.screenshots)).perform(click())
    }

    override fun pressBack() = Espresso.pressBack()

    override fun takeScreenshotViaFastlane(fileName: String) {
        Screengrab.screenshot(fileName)
    }
}