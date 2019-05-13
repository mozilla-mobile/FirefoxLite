package org.mozilla.focus.activity;

import android.content.Context;
import android.content.Intent;
import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.utils.Settings;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.doesNotExist;
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

    @After
    public void after() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final Settings settings = Settings.getInstance(context);
        settings.getEventHistory().clear();
    }

    /**
     * Test case no: TC0171
     * Test case name: Deny to install VPN
     * Steps:
     * 1. Launch app
     * 2. Tap VPN icon
     * 3. Tap No
     * 4. Check VPN icon not exist
     */
    @Test
    public void denyToInstallVpn() {

        // VPN icon displayed
        onView(withId(R.id.home_wifi_vpn_survey)).perform(click());

        // Deny to install
        onView(withId(R.id.wifi_vpn_btn_no)).perform(click());

        // Check VPN icon not exist
        onView(withId(R.id.wifi_vpn_btn_no)).check(doesNotExist());
    }
}
