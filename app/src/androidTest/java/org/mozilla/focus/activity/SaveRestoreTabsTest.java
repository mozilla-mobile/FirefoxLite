package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;

import androidx.test.espresso.contrib.RecyclerViewActions;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.autobot.BottomBarRobot;
import org.mozilla.focus.persistence.TabEntity;
import org.mozilla.focus.persistence.TabsDatabase;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.RecyclerViewTestUtils;
import org.mozilla.rocket.content.ExtentionKt;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.pressBack;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.assertThat;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.AllOf.allOf;
import static org.mozilla.focus.utils.AndroidTestExtensionKt.visibleWithId;

@RunWith(AndroidJUnit4.class)
public class SaveRestoreTabsTest {

    private static final TabEntity TAB = new TabEntity("TEST_ID", "ID_HOME", "Yahoo TW", "https://tw.yahoo.com");
    private static final TabEntity TAB_2 = new TabEntity("TEST_ID_2", TAB.getId(), "Google", "https://www.google.com");

    private TabsDatabase tabsDatabase;

    @Rule
    public final ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        // Set the share preferences and start the activity
        AndroidTestUtils.beforeTest();

        tabsDatabase = ExtentionKt.appComponent((Context) getApplicationContext()).tabsDatabase();
    }

    /**
     * Test case no: TC_0072
     * Test case name: Empty tab
     * Steps:
     * 1. Launch Rocket with no tab
     * 2. tab number is zero
     */
    @Test
    public void restoreEmptyTab() {
        activityRule.launchActivity(new Intent());
        checkHomeTabCounterText("0");
    }

    /**
     * Test case no: TC_0070
     * Test case name: Tap tray overview - Normal State
     * Steps:
     * 1. Launch app with no tab
     * 2. open first top website on home page
     * 4. relaunch app
     * 5. check tab number is 1
     * 6. open tab tray to check url, icon, website title, website subtitle, close button displayed
     */
    @Test
    public void restoreEmptyTab_addNewTabThenRelaunch() {
        activityRule.launchActivity(new Intent());
        checkBrowserTabCounterText("0");

        // Some intermittent issues happens when performing a single click event, we add a rollback action in case of a long click action
        // is triggered unexpectedly here. i.e. pressBack() can dismiss the popup menu.

        onView(visibleWithId(R.id.page_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click(pressBack())));

        relaunchActivity();

        checkHomeTabCounterText("1");
        onView(withId(R.id.home_fragment_tab_counter)).perform(click());

        onView(withId(R.id.tab_tray_recycler_view)).check(matches(isDisplayed()));

        onView(withId(R.id.close_button)).check(matches(isDisplayed()));

        onView(withId(R.id.website_icon)).check(matches(isDisplayed()));

        onView(withId(R.id.website_title)).check(matches(isDisplayed()));

        onView(withId(R.id.website_subtitle)).check(matches(isDisplayed()));
    }

    /**
     * Test case no: TC0075
     * Test case name: restorePreviousTabs -> add new tab
     * Steps:
     * 1. given there are 2 tabs
     * 2. open a new tab from tab tray
     * 3. open first top site
     * 4. relaunch activity
     * 5. check tab number is 3
     */
    @Test
    public void restorePreviousTabs_addNewTabThenRelaunch() throws InterruptedException {
        tabsDatabase.tabDao().insertTabs(TAB, TAB_2);
        AndroidTestUtils.setFocusTabId(TAB.getId());

        activityRule.launchActivity(new Intent());

        //open a new tab from tab tray
        checkHomeTabCounterText("2");
        onView(withId(R.id.home_fragment_tab_counter)).perform(click());

        // wait for tab tray to show up
        Thread.sleep(500);
        onView(withId(R.id.new_tab_button)).perform(click());

        //dismiss url bar and go back to home
        onView(Matchers.allOf(withId(R.id.dismiss), isDisplayed())).perform(click());

        //open first top site
        onView(visibleWithId(R.id.page_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        relaunchActivity();

        checkBrowserTabCounterText("3");
    }

    /**
     * Test case no: TC0107
     * Test case name: Tab tray - close all tab
     * Steps:
     * 1. Given there are 2 tabs
     * 2. Open tab tray
     * 3. Tap Close All -> Cancel
     * 4. Check tab tray count is 2
     * 5. Tap Close All -> Ok
     * 6. Check tab tray number is 0
     */
    @Test
    public void tabTray_closeAllTab() throws InterruptedException {

        // Given there are 2 tabs
        tabsDatabase.tabDao().insertTabs(TAB, TAB_2);
        AndroidTestUtils.setFocusTabId(TAB.getId());

        activityRule.launchActivity(new Intent());

        // Open tab tray
        onView(withId(R.id.home_fragment_tab_counter)).perform(click());

        // Tap Close All -> Tap Cancel
        // wait for tab tray to show up
        Thread.sleep(500);
        onView(withId(R.id.close_all_tabs_btn)).perform(click());
        onView(withText(R.string.action_cancel)).perform(click());

        // Check tab tray count is 2
        assertThat(RecyclerViewTestUtils.getCountFromRecyclerView(R.id.tab_tray_recycler_view), is(2));

        // Tap Close All -> Tap Ok
        onView(allOf(withId(R.id.close_all_tabs_btn), isDisplayed())).perform(click());
        onView(withText(R.string.action_ok)).perform(click());

        // Check tab number is 0
        checkBrowserTabCounterText("0");
    }


    private void relaunchActivity() {
        activityRule.finishActivity();
        activityRule.launchActivity(new Intent());
    }

    private void checkHomeTabCounterText(String text) {
        onView(allOf(withId(R.id.counter_text), isDescendantOfA(withId(R.id.home_fragment_tab_counter))))
                .check(matches(withText(text)));
    }

    private void checkBrowserTabCounterText(String text) {
        onView(allOf(withId(R.id.counter_text), isDescendantOfA(new BottomBarRobot().browserBottomBarItemView(R.id.bottom_bar_tab_counter))))
                .check(matches(withText(text)));
    }
}
