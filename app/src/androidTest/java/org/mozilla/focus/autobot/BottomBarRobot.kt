package org.mozilla.focus.autobot

import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.view.View
import org.hamcrest.Matcher
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mozilla.focus.R
import org.mozilla.focus.helper.GetNthChildViewMatcher.nthChildOf
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.chrome.BottomBarItemAdapter

inline fun bottomBar(func: BottomBarRobot.() -> Unit) = BottomBarRobot().apply(func)

// Need to call before the ViewModel created
inline fun bottomBar(
    bottomBarItems: List<BottomBarItemAdapter.ItemData>,
    MenuBottomBarItems: List<BottomBarItemAdapter.ItemData>,
    func: BottomBarRobot.() -> Unit
) {
    BottomBarRobot().apply {
        mockBottomBarItems(bottomBarItems)
        mockMenuBottomBarItems(MenuBottomBarItems)
        func()
    }
}

interface BottomBarAutomation {
    fun mockBottomBarItems(items: List<BottomBarItemAdapter.ItemData>)

    fun mockMenuBottomBarItems(items: List<BottomBarItemAdapter.ItemData>)

    fun clickHomeBottomBarItem(position: Int)

    fun clickBrowserBottomBarItem(position: Int)

    fun clickMenuBottomBarItem(position: Int)

    fun homeBottomBarItemView(position: Int): Matcher<View>

    fun browserBottomBarItemView(position: Int): Matcher<View>

    fun menuBottomBarItemView(position: Int): Matcher<View>
}

class BottomBarRobot : BottomBarAutomation {

    override fun mockBottomBarItems(items: List<BottomBarItemAdapter.ItemData>) {
        val firebase = FirebaseHelper.getFirebase()
        val spyFirebase = spy(firebase)
        doReturn(items.toJsonString()).`when`(spyFirebase).getRcString(FirebaseHelper.STR_BOTTOM_BAR_ITEMS)
        FirebaseHelper.replaceContract(spyFirebase)
    }

    override fun mockMenuBottomBarItems(items: List<BottomBarItemAdapter.ItemData>) {
        val firebase = FirebaseHelper.getFirebase()
        val spyFirebase = spy(firebase)
        doReturn(items.toJsonString()).`when`(spyFirebase).getRcString(FirebaseHelper.STR_MENU_BOTTOM_BAR_ITEMS)
        FirebaseHelper.replaceContract(spyFirebase)
    }

    private fun List<BottomBarItemAdapter.ItemData>.toJsonString(): String =
            joinToString(separator = ",", prefix = "[", postfix = "]") { "{\"type\":\"${it.type}\"}" }

    override fun clickHomeBottomBarItem(position: Int) {
        onView(bottomBarItemView(R.id.bottom_bar, position)).perform(click())
    }

    override fun clickBrowserBottomBarItem(position: Int) {
        onView(bottomBarItemView(R.id.browser_bottom_bar, position)).perform(click())
    }

    override fun clickMenuBottomBarItem(position: Int) {
        onView(bottomBarItemView(R.id.menu_bottom_bar, position)).perform(click())
    }

    override fun homeBottomBarItemView(position: Int): Matcher<View> =
            bottomBarItemView(R.id.bottom_bar, position)

    override fun browserBottomBarItemView(position: Int): Matcher<View> =
            bottomBarItemView(R.id.browser_bottom_bar, position)

    override fun menuBottomBarItemView(position: Int): Matcher<View> =
            bottomBarItemView(R.id.menu_bottom_bar, position)

    private fun bottomBarItemView(id: Int, position: Int): Matcher<View> =
            nthChildOf(nthChildOf(withId(id), 0), position)
}

fun List<BottomBarItemAdapter.ItemData>.indexOfType(type: Int): Int = indexOf(BottomBarItemAdapter.ItemData(type))