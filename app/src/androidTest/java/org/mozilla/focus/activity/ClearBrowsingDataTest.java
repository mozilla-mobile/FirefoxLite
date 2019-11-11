package org.mozilla.focus.activity;

import android.content.Intent;
import androidx.annotation.Keep;
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
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

@Keep
@RunWith(AndroidJUnit4.class)
public class ClearBrowsingDataTest {

    // Defer the startup of the activity cause we want to avoid First Run / Share App / Rate App dialogs
    @Rule
    public final ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setUp() {
        // Set the share preferences and start the activity
        new BeforeTestTask.Builder()
                .setSkipFirstRun(true)
                .build()
                .execute();
        activityRule.launchActivity(new Intent());
    }

    /**
     * Test case no: TC0027
     * Test case name: Clear browsing data
     * Steps:
     * 1. Launch app
     * 2. Tap menu -> settings
     * 3. Tap clear browsing data
     * 4. A list of browsing history, from history, cookies, cache is checked
     * 5. Check cancel and clear data button displayed
     * 6. Tap clear data button
     * 7. Check toast message "browsing data cleared" displayed
     */
    @Test
    public void clearBrowsingData() {

        //Tap menu -> settings
        AndroidTestUtils.tapHomeMenuButton();
        AndroidTestUtils.tapSettingButton();

        // Tap clear browsing data
        onView(withText(R.string.preference_privacy_storage_clear_browsing_data)).perform(click());

        // check browsing history, from history, cookies, cache in the list
        onView(withText(R.string.setting_dialog_browsing_history)).check(matches(isDisplayed())).check(matches(isChecked()));
        onView(withText(R.string.setting_dialog_form_history)).check(matches(isDisplayed())).check(matches(isChecked()));
        onView(withText(R.string.setting_dialog_cookies)).check(matches(isDisplayed())).check(matches(isChecked()));
        onView(withText(R.string.setting_dialog_cache)).check(matches(isDisplayed())).check(matches(isChecked()));

        // Check cancel and clear data button
        onView(withText(R.string.setting_dialog_cancel)).check(matches(isDisplayed()));
        onView(withText(R.string.setting_dialog_clear_data)).check(matches(isDisplayed()));

        // Tap clear browsing data
        onView(withText(R.string.setting_dialog_clear_data)).perform(click());

        // Check toast message "Browsing data cleared"
        AndroidTestUtils.toastContainsText(activityRule.getActivity(), R.string.message_cleared_browsing_data);
    }
}