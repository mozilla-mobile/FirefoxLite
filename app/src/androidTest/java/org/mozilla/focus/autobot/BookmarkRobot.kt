package org.mozilla.focus.autobot

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.contrib.RecyclerViewActions
import android.support.test.espresso.matcher.RootMatchers
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.v7.widget.RecyclerView
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.utils.AndroidTestUtils
import org.mozilla.focus.utils.RecyclerViewTestUtils.clickChildViewWithId

inline fun bookmark(func: BookmarkRobot.() -> Unit) = BookmarkRobot().apply(func)

class BookmarkRobot : MenuRobot() {

    fun checkEmptyViewIsDisplayed() {
        onView(withText(R.string.bookmarks_empty_view_msg)).check(matches(isDisplayed()))
    }

    fun updateBookmarkName(name: String) {
        onView(withId(R.id.bookmark_name)).perform(replaceText(name))
    }

    fun clickSave() {
        onView(withText(R.string.bookmark_edit_save)).perform(click())
    }

    fun clickListItemActionMenu(position: Int) {
        onView(withId(R.id.recyclerview)).perform(RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(position, clickChildViewWithId(R.id.history_item_btn_more)))
    }

    fun checkItemMenuEditIsDisplayed() {
        onView(withText(R.string.edit_bookmark)).inRoot(RootMatchers.isPlatformPopup()).check(matches(isDisplayed()))
    }

    fun clickItemMenuEdit() {
        onView(withText(R.string.edit_bookmark)).inRoot(RootMatchers.isPlatformPopup()).perform(click())
    }

    fun checkBookmarkUpdatedToastIsDisplayed(activity: MainActivity) = AndroidTestUtils.toastContainsText(activity, R.string.bookmark_edit_success)
}