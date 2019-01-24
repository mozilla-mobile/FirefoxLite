package org.mozilla.focus.activity;

import android.content.Intent;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.BeforeTestTask;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class VPNIconTest {

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
     * Test case no: TC0168
     * Test case name: Show VPN icon
     * Steps:
     * 1. Launch app
     * 2. VPN icon displayed
     * 3. Relaunch app
     * 4. VPN icon displayed
     */
    @Test
    public void restoreEmptyTab() {

        // VPN icon displayed
        onView(withId(R.id.home_wifi_vpn_survey)).check(matches(isDisplayed()));

        // Relaunch app
        relaunchActivity();

        // VPN icon displayed
        onView(withId(R.id.home_wifi_vpn_survey)).check(matches(isDisplayed()));
    }

    private void relaunchActivity() {
        activityRule.finishActivity();
        activityRule.launchActivity(new Intent());
    }
}
