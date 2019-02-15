package org.mozilla.focus.autobot

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.withId
import org.mozilla.focus.R
import org.mozilla.focus.utils.AndroidTestUtils
import tools.fastlane.screengrab.Screengrab

/** Some common menu relevant actions **/
open class MenuRobot {

    /** Open menu **/
    fun clickHomeMenu() = AndroidTestUtils.tapHomeMenuButton()

    fun clickBrowserMenu() = AndroidTestUtils.tapBrowserMenuButton()

    /** Click menu item **/
    fun clickMenuBookmarks() = onView(withId(R.id.menu_bookmark)).perform(click())

    fun clickMenuDownloads() = onView(withId(R.id.menu_download)).perform(click())
    fun clickMenuHistory() = onView(withId(R.id.menu_history)).perform(click())
    fun clickMenuMyShots() = onView(withId(R.id.menu_screenshots)).perform(click())
    fun clickMenuTurboMode() = onView(withId(R.id.menu_turbomode)).perform(click())
    fun clickMenuFindInPage() = onView(withId(R.id.menu_find_in_page)).perform(click())
    fun clickMenuBlockImages() = onView(withId(R.id.menu_blockimg)).perform(click())
    fun clickMenuClearCache() = onView(withId(R.id.menu_delete)).perform(click())
    fun clickMenuSettings() = onView(withId(R.id.menu_preferences)).perform(click())
    fun clickExitApp() = onView(withId(R.id.menu_exit)).perform(click())

    /** Click panel item **/
    fun clickPanelDownload() = onView(withId(R.id.downloads)).perform(click())

    fun clickPanelHistory() = onView(withId(R.id.history)).perform(click())
    fun clickPanelMyShots() = onView(withId(R.id.screenshots)).perform(click())

    fun pressBack() = Espresso.pressBack()

    fun takeScreenshotViaFastlane(fileName: String) {
        Screengrab.screenshot(fileName)
    }
}