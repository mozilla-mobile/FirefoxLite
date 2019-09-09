package org.mozilla.focus.autobot

import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA
import androidx.test.espresso.matcher.ViewMatchers.withId
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.spy
import org.mozilla.focus.R
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

    fun clickBrowserBottomBarItem(id: Int)

    fun clickMenuBottomBarItem(id: Int)

    fun browserBottomBarItemView(id: Int): Matcher<View>

    fun menuBottomBarItemView(id: Int): Matcher<View>
}

class BottomBarRobot : BottomBarAutomation {

    override fun mockBottomBarItems(items: List<BottomBarItemAdapter.ItemData>) {
        val firebase = FirebaseHelper.getFirebase()
        val spyFirebase = spy(firebase)
        doReturn(items.toJsonString()).`when`(spyFirebase).getRcString(FirebaseHelper.STR_BOTTOM_BAR_ITEMS_V2)
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

    override fun clickBrowserBottomBarItem(id: Int) {
        onView(bottomBarItemView(R.id.browser_bottom_bar, id)).perform(click())
    }

    override fun clickMenuBottomBarItem(id: Int) {
        onView(bottomBarItemView(R.id.menu_bottom_bar, id)).perform(click())
    }

    override fun browserBottomBarItemView(id: Int): Matcher<View> =
            bottomBarItemView(R.id.browser_bottom_bar, id)

    override fun menuBottomBarItemView(id: Int): Matcher<View> =
            bottomBarItemView(R.id.menu_bottom_bar, id)

    private fun bottomBarItemView(bottomBarId: Int, viewId: Int): Matcher<View> =
            allOf(isDescendantOfA(withId(bottomBarId)), withId(viewId))
}

fun List<BottomBarItemAdapter.ItemData>.indexOfType(type: Int): Int = indexOf(BottomBarItemAdapter.ItemData(type))