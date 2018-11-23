package org.mozilla.focus.autobot

import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withParent
import android.view.View
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.mozilla.focus.R
import tools.fastlane.screengrab.Screengrab

/** Some common menu relevant actions **/
open class MenuRobot {

    /** Open menu **/
    fun clickHomeMenu() = onView(allOf<View>(withId(R.id.btn_menu), withParent(withId(R.id.home_screen_menu)))).perform(click())

    fun clickBrowserMenu() = onView(allOf<View>(withId(R.id.btn_menu), not<View>(withParent(withId(R.id.home_screen_menu))))).perform(click())

    /** Click menu item **/
    fun clickMenuBookmarks() = onView(withId(R.id.menu_bookmark)).perform(click())

    fun clickMenuDownloads() = onView(withId(R.id.menu_download)).perform(click())
    fun clickMenuHistory() = onView(withId(R.id.menu_history)).perform(click())
    fun clickMenuMyShots() = onView(withId(R.id.menu_screenshots)).perform(click())
    fun clickMenuTurboMode() = onView(withId(R.id.menu_turbomode)).perform(click())
    fun clickMenuBlockImages() = onView(withId(R.id.menu_blockimg)).perform(click())
    fun clickMenuClearCache() = onView(withId(R.id.menu_delete)).perform(click())
    fun clickMenuSettings() = onView(withId(R.id.menu_preferences)).perform(click())

    /** Click panel item **/
    fun clickPanelDownload() = onView(withId(R.id.downloads)).perform(click())

    fun clickPanelHistory() = onView(withId(R.id.history)).perform(click())
    fun clickPanelMyShots() = onView(withId(R.id.screenshots)).perform(click())

    fun pressBack() = Espresso.pressBack()

    fun takeScreenshotViaFastlane(fileName: String) {
        Screengrab.screenshot(fileName)
    }
}