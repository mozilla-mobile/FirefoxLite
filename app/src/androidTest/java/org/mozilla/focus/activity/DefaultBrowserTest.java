package org.mozilla.focus.activity;


import android.content.Intent;
import android.preference.Preference;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;
import android.widget.Switch;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.utils.AndroidTestUtils;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;

// Only device with API>=24 can set default browser via system settings
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 24, maxSdkVersion = 27)
public class DefaultBrowserTest {

    // This is the title of Android system setting. We hard code it here so it will only fits emulator
    // For physical devices this may vary.
    private final static String SETTING_DEFAULT_BROWSER = "Browser app";

    private UiDevice uiDevice;

    @Rule
    public final ActivityTestRule<SettingsActivity> settingsActivity = new ActivityTestRule<>(SettingsActivity.class, false, false);

    @Rule
    public ActivityTestRule<MainActivity> mainActivity = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setup() {
        AndroidTestUtils.beforeTest();
        uiDevice = UiDevice.getInstance(getInstrumentation());
    }

    /**
     * Test case no: TC0090
     * Test case name: Set FIrefox Lite as default browser when Chrome is default browser
     * Steps:
     * 1. Set Chrome as a default browser
     * 2. Launch Rocket
     * 3. Go to settings page (Tap menu -> settings)
     * 4. Tap "Make default browser"
     * 5. Go back to Rocket's settings
     * 6. Check it correctly set default browser to Firefox Lite
     */
    @Test
    public void changeDefaultBrowser_whenChromeIsDefault() {

        // Start MainActivity
        mainActivity.launchActivity(new Intent());
        final MainActivity activity = mainActivity.getActivity();
        if (activity == null) {
            throw new AssertionError("Could start activity");
        }

        final String prefName = activity.getString(R.string.pref_key_default_browser);

        // 1. Set Chrome as a default browser
        // Click on the menu item
        onView(allOf(withId(R.id.btn_menu), withParent(withId(R.id.home_screen_menu)))).perform(click());

        // Click on Settings
        onView(withId(R.id.menu_preferences)).perform(click());

        // Click on "Default Browser" setting, this will brings up the Android system setting
        onData(allOf(
                is(instanceOf(Preference.class)),
                withKey(prefName))).
                onChildView(withClassName(is(Switch.class.getName()))).perform(click());

        // Open default browser setting
        final UiObject allAppsButton = uiDevice
                .findObject(new UiSelector().text(SETTING_DEFAULT_BROWSER));

        try {
            allAppsButton.click();

        } catch (UiObjectNotFoundException e) {
            throw new AssertionError("Could find the setting", e);
        }

        // Choose Chrome browser
        final UiScrollable browserList = new UiScrollable(new UiSelector());
        final UiSelector chromeView = new UiSelector().text("Chrome");

        try {
            browserList.scrollIntoView(chromeView);
            final UiObject browserCandidate = uiDevice.findObject(chromeView);
            browserCandidate.click();

        } catch (UiObjectNotFoundException e) {
            throw new AssertionError("Could find the chrome app in default browser", e);
        }

        // 2. Set Firefox Lite as default browser
        // Open default browser setting
        try {
            allAppsButton.click();

        } catch (UiObjectNotFoundException e) {
            throw new AssertionError("Could find the setting", e);
        }

        // Choose Firefox Lite browser
        final UiSelector rocketView = new UiSelector().text(activity.getString(R.string.app_name));
        try {
            browserList.scrollIntoView(rocketView);
            final UiObject browserCandidate = uiDevice.findObject(rocketView);
            browserCandidate.click();

        } catch (UiObjectNotFoundException e) {
            throw new AssertionError("Could find the Firefox Lite app in default browser", e);
        }

        // Now launch Rocket's setting activity
        settingsActivity.launchActivity((new Intent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)));

        // Check if the "Default Browser" pref is correctly displayed (switch checked)
        onData(allOf(
                is(instanceOf(Preference.class)),
                withKey(prefName))).
                onChildView(withClassName(is(Switch.class.getName()))).check(matches(isChecked()));

    }

    /**
     * Test case no: TC0093
     * Test case name: Set Firefox Lite as default browser when no default browser
     * Steps:
     * 1. Launch Rocket
     * 2. Go to settings page (Tap menu -> settings)
     * 3. Tap "Make default browser"
     * 4. Go back to Rocket's settings
     * 5. Check it correctly set default browser to Firefox Lite
     */
    @Test
    public void changeDefaultBrowser_whenNoDefault() {

        mainActivity.launchActivity(new Intent());
        final MainActivity activity = mainActivity.getActivity();
        if (activity == null) {
            throw new AssertionError("Could start activity");
        }

        final String prefName = activity.getString(R.string.pref_key_default_browser);

        // Click on the menu item
        onView(allOf(withId(R.id.btn_menu), withParent(withId(R.id.home_screen_menu)))).perform(click());

        // Click on Settings
        onView(withId(R.id.menu_preferences)).perform(click());

        // Click on "Default Browser" setting, this will brings up the Android system setting
        onData(allOf(
                is(instanceOf(Preference.class)),
                withKey(prefName))).
                onChildView(withClassName(is(Switch.class.getName()))).perform(click());

        // 2. Set Firefox Lite as default browser
        // Open default browser setting
        final UiObject allAppsButton = uiDevice
                .findObject(new UiSelector().text(SETTING_DEFAULT_BROWSER));

        try {
            allAppsButton.click();

        } catch (UiObjectNotFoundException e) {
            throw new AssertionError("Could find the setting", e);
        }

        // Choose Firefox Lite browser
        final UiScrollable browserList = new UiScrollable(new UiSelector());

        final UiSelector rocketView = new UiSelector().text(activity.getString(R.string.app_name));
        try {
            browserList.scrollIntoView(rocketView);
            final UiObject browserCandidate = uiDevice.findObject(rocketView);
            browserCandidate.click();

        } catch (UiObjectNotFoundException e) {
            throw new AssertionError("Could find the Firefox Lite app in default browser", e);
        }

        // Now launch Rocket's setting activity
        settingsActivity.launchActivity((new Intent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)));

        // Check if the "Default Browser" pref is correctly displayed (switch checked)
        onData(allOf(
                is(instanceOf(Preference.class)),
                withKey(prefName))).
                onChildView(withClassName(is(Switch.class.getName()))).check(matches(isChecked()));

    }
}