package org.mozilla.focus.activity;

import android.content.Intent;
import android.support.test.espresso.contrib.RecyclerViewActions;
import android.support.test.espresso.matcher.ViewMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.autobot.BottomBarRobot;
import org.mozilla.focus.autobot.BottomBarRobotKt;
import org.mozilla.focus.persistence.TabEntity;
import org.mozilla.focus.persistence.TabsDatabase;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.RecyclerViewTestUtils;
import org.mozilla.rocket.chrome.BottomBarItemAdapter;
import org.mozilla.rocket.chrome.BottomBarViewModel;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressBack;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.assertThat;
import static android.support.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.AllOf.allOf;

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

        tabsDatabase = Inject.getTabsDatabase(null);
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
        int bottomBarTabCounterPos = BottomBarRobotKt.indexOfType(BottomBarViewModel.getDEFAULT_BOTTOM_BAR_ITEMS(), BottomBarItemAdapter.TYPE_TAB_COUNTER);
        activityRule.launchActivity(new Intent());
        checkHomeTabCounterText(bottomBarTabCounterPos, "0");
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
        int bottomBarTabCounterPos = BottomBarRobotKt.indexOfType(BottomBarViewModel.getDEFAULT_BOTTOM_BAR_ITEMS(), BottomBarItemAdapter.TYPE_TAB_COUNTER);
        activityRule.launchActivity(new Intent());
        checkBrowserTabCounterText(bottomBarTabCounterPos, "0");

        // Some intermittent issues happens when performing a single click event, we add a rollback action in case of a long click action
        // is triggered unexpectedly here. i.e. pressBack() can dismiss the popup menu.

        onView(withId(R.id.main_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click(pressBack())));

        relaunchActivity();

        checkHomeTabCounterText(bottomBarTabCounterPos, "1");
        onView(new BottomBarRobot().homeBottomBarItemView(bottomBarTabCounterPos)).perform(click());

        onView(withId(R.id.tab_tray)).check(matches(isDisplayed()));

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

        int bottomBarTabCounterPos = BottomBarRobotKt.indexOfType(BottomBarViewModel.getDEFAULT_BOTTOM_BAR_ITEMS(), BottomBarItemAdapter.TYPE_TAB_COUNTER);
        activityRule.launchActivity(new Intent());

        //open a new tab from tab tray
        checkBrowserTabCounterText(bottomBarTabCounterPos, "2");
        onView(new BottomBarRobot().browserBottomBarItemView(bottomBarTabCounterPos)).perform(click());

        // wait for tab tray to show up
        Thread.sleep(500);
        onView(withId(R.id.new_tab_button)).perform(click());

        //open first top site
        onView(ViewMatchers.withId(R.id.main_list))
                .perform(RecyclerViewActions.actionOnItemAtPosition(0, click()));
        relaunchActivity();

        checkBrowserTabCounterText(bottomBarTabCounterPos, "3");
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

        int bottomBarTabCounterPos = BottomBarRobotKt.indexOfType(BottomBarViewModel.getDEFAULT_BOTTOM_BAR_ITEMS(), BottomBarItemAdapter.TYPE_TAB_COUNTER);
        activityRule.launchActivity(new Intent());

        // Open tab tray
        checkBrowserTabCounterText(bottomBarTabCounterPos, "2");
        onView(new BottomBarRobot().browserBottomBarItemView(bottomBarTabCounterPos)).perform(click());

        // Tap Close All -> Tap Cancel
        // wait for tab tray to show up
        Thread.sleep(500);
        onView(withId(R.id.close_all_tabs_btn)).perform(click());
        onView(withText(R.string.action_cancel)).perform(click());

        // Check tab tray count is 2
        assertThat(RecyclerViewTestUtils.getCountFromRecyclerView(R.id.tab_tray), is(2));

        // Tap Close All -> Tap Ok
        onView(allOf(withId(R.id.close_all_tabs_btn), isDisplayed())).perform(click());
        onView(withText(R.string.action_ok)).perform(click());

        // Check tab number is 0
        checkBrowserTabCounterText(bottomBarTabCounterPos, "0");
    }


    private void relaunchActivity() {
        activityRule.finishActivity();
        activityRule.launchActivity(new Intent());
    }

    private void checkHomeTabCounterText(int tabCounterPos, String text) {
        onView(allOf(withId(R.id.counter_text), isDescendantOfA(new BottomBarRobot().homeBottomBarItemView(tabCounterPos))))
                .check(matches(withText(text)));
    }

    private void checkBrowserTabCounterText(int tabCounterPos, String text) {
        onView(allOf(withId(R.id.counter_text), isDescendantOfA(new BottomBarRobot().browserBottomBarItemView(tabCounterPos))))
                .check(matches(withText(text)));
    }
}
