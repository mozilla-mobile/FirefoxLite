package org.mozilla.focus.activity;

import android.content.Intent;
import android.support.annotation.Keep;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.utils.AndroidTestUtils;

import static android.support.test.espresso.Espresso.onIdle;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.RootMatchers.withDecorView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

@Keep
@RunWith(AndroidJUnit4.class)
public class ClearCacheTest {

    // Defer the startup of the activity cause we want to avoid First Run / Share App / Rate App dialogs
    @Rule
    public final ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        new BeforeTestTask.Builder()
                .setSkipFirstRun(true)
                .build()
                .execute();
        activityRule.launchActivity(new Intent());
    }

    @After
    public void tearDown() {
        onIdle();
    }

    /**
     * Test case no: TC0044
     * Test case name: Clear cache
     * Steps:
     * 1. Launch app
     * 2. Tap menu
     * 3. Tap clear cache
     * 4. Check toast message "cache cleared"
     */
    @Test
    public void clearCache() {

        // Tap menu
        AndroidTestUtils.tapBrowserMenuButton();

        // Tap clear cache
        onView(withId(R.id.menu_delete)).perform(click());

        // Check toast message "cache cleared"
        String msgClearCacheWoFormatter = AndroidTestUtils.removeStrFormatter(activityRule.getActivity().getResources().getString(R.string.message_cleared_cached));
        onView(withText(containsString(msgClearCacheWoFormatter)))
                .inRoot(withDecorView(not(is(activityRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }
}