package org.mozilla.focus.autobot

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.recyclerview.widget.RecyclerView
import org.mozilla.focus.R
import org.mozilla.focus.utils.RecyclerViewTestUtils.clickChildViewWithId

inline fun history(func: HistoryRobot.() -> Unit) = HistoryRobot().apply(func)

class HistoryRobot(menuAutomation: MenuAutomation = MenuRobot()) : MenuAutomation by menuAutomation {

    fun clickListItemActionMenu(position: Int) {
        onView(withId(R.id.browsing_history_recycler_view)).perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, clickChildViewWithId(R.id.history_item_btn_more)))
    }

    fun checkItemMenuDeleteIsDisplayed() {
        onView(withText(R.string.browsing_history_menu_delete))
                .inRoot(RootMatchers.isPlatformPopup())
                .check(matches(isDisplayed()))
    }

    fun clickItemMenuDelete() {
        onView(withText(R.string.browsing_history_menu_delete))
                .inRoot(RootMatchers.isPlatformPopup())
                .perform(click())
    }

    fun clickClearBrowsingHistory() {
        onView(withId(R.id.browsing_history_btn_clear)).perform(click())
    }

    fun checkConfirmClearDialogIsDisplayed() {
        onView(withText(R.string.browsing_history_dialog_confirm_clear_message)).check(matches(isDisplayed()))
    }
}