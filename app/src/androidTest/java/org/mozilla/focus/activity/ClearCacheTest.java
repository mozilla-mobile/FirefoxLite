package org.mozilla.focus.activity;

import android.content.Intent;
import androidx.annotation.Keep;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.utils.AndroidTestUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.withDecorView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
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
        AndroidTestUtils.tapHomeMenuButton();

        // Tap clear cache
        onView(withId(R.id.menu_delete))
                .inRoot(RootMatchers.isDialog())
                .perform(click());

        // Check toast message "cache cleared"
        String msgClearCacheWoFormatter = AndroidTestUtils.removeStrFormatter(activityRule.getActivity().getResources().getString(R.string.message_cleared_cached));
        onView(withText(containsString(msgClearCacheWoFormatter)))
                .inRoot(withDecorView(not(is(activityRule.getActivity().getWindow().getDecorView()))))
                .check(matches(isDisplayed()));
    }
}