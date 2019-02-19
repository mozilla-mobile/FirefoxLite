package org.mozilla.focus.autobot

import android.support.test.InstrumentationRegistry
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.pressImeActionButton
import android.support.test.espresso.action.ViewActions.replaceText
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.isDisplayed
import android.support.test.espresso.matcher.ViewMatchers.withId
import android.support.test.espresso.matcher.ViewMatchers.withText
import android.support.test.uiautomator.UiDevice
import android.support.test.uiautomator.UiSelector
import android.view.View
import org.hamcrest.Matchers
import org.mozilla.focus.R
import org.mozilla.focus.helper.SessionLoadedIdlingResource
import org.mozilla.focus.utils.AndroidTestUtils
import org.mozilla.rocket.privately.PrivateModeActivity

inline fun private(func: PrivateRobot.() -> Unit) = PrivateRobot().apply(func)
class PrivateRobot {

    private lateinit var sessionLoadedIdlingResource: SessionLoadedIdlingResource
    private var uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun loadPageFromPrivateSearchField(activity: PrivateModeActivity, url: String) {
        // Click private search field
        onView(Matchers.allOf(withId(R.id.pm_home_fake_input))).perform(click())
        // create the idlingResource before the new session is created.
        loadPrivatePage(activity, url)
    }

    private fun loadPrivatePage(activity: PrivateModeActivity, url: String) {
        // TODO find a way to remove the activity reference
        sessionLoadedIdlingResource = SessionLoadedIdlingResource(activity)

        // Enter test site url
        onView(Matchers.allOf<View>(withId(R.id.url_edit), isDisplayed())).perform(replaceText(url), pressImeActionButton())

        runWithIdleRes(sessionLoadedIdlingResource) {
            onView(Matchers.allOf(withId(R.id.display_url), isDisplayed())).check(matches(withText(url)))
        }
    }

    fun tapBackToBrowserInPrivateMenu() {
        onView(withId(R.id.btn_mode)).perform(click())
    }

    fun checkUrlInPrivateMode(url: String) {
        uiDevice.findObject(UiSelector().resourceId("display_url").text(url))
    }

    fun showToastMessageInPrivateMode(activity: PrivateModeActivity, msg: Int) {
        AndroidTestUtils.toastContainsText(activity, msg)
    }
}