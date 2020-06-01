package org.mozilla.focus.activity;


import android.content.Intent;
import android.preference.Preference;
import androidx.annotation.NonNull;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiScrollable;
import androidx.test.uiautomator.UiSelector;
import android.widget.Switch;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.utils.AndroidTestUtils;

import static androidx.test.InstrumentationRegistry.getInstrumentation;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.PreferenceMatchers.withKey;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withClassName;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;

// Only device with API>=24 can set default browser via system settings.
// However, Browser select dialog behavior is different between API 24,25 (7.X) and API 26 (8.0) so this test caters to API 24, 25
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 24, maxSdkVersion = 25)
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
     * Test case name: Set Firefox Lite as default browser when Chrome is default browser
     * Steps:
     * 1. Set Chrome as a default browser
     * 2. Launch app
     * 3. Go to settings page (Tap menu -> settings)
     * 4. Tap "Make default browser"
     * 5. Go back to Firefox Lite's settings
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
        AndroidTestUtils.tapHomeMenuButton();

        // Click on Settings
        onView(withId(R.id.menu_preferences))
                .inRoot(RootMatchers.isDialog())
                .perform(click());

        // Click on "Default Browser" setting, this will brings up the Android system setting
        clickDefaultBrowserSetting(prefName);

        // Open default browser setting
        final UiObject allAppsButton = uiDevice
                .findObject(new UiSelector().text(SETTING_DEFAULT_BROWSER));
        openDefaultBrowserAndroidSetting(allAppsButton);

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
        // Since it is not on root activity so we use ui-automator instead of Espresso Press back
        UiDevice mDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        mDevice.pressBack();

        // Check if the "Default Browser" pref switch not checked
        onData(allOf(
                is(instanceOf(Preference.class)),
                withKey(prefName))).
                onChildView(withClassName(is(Switch.class.getName()))).check(matches(not(isChecked())));

        // 2. Set Firefox Lite as default browser
        // Click on "Default Browser" setting, this will brings up the Android system setting
        clickDefaultBrowserSetting(prefName);

        // Click on the positive button of the tutorial dialog to continue
        onView(withId(android.R.id.button1))
                .inRoot(RootMatchers.isDialog())
                .perform(click());

        // Open default browser setting
        openDefaultBrowserAndroidSetting(allAppsButton);

        // Choose Firefox Lite browser
        chooseLiteAsDefaultBrowser(activity);

        // Now launch Firefox Lite's setting activity
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
     * 1. Launch app
     * 2. Go to settings page (Tap menu -> settings)
     * 3. Tap "Make default browser"
     * 4. Go back to app's settings
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
        AndroidTestUtils.tapHomeMenuButton();

        // Click on Settings
        onView(withId(R.id.menu_preferences))
                .inRoot(RootMatchers.isDialog())
                .perform(click());

        // Click on "Default Browser" setting, this will brings up the Android system setting
        clickDefaultBrowserSetting(prefName);

        // Click on the positive button of the tutorial dialog to continue
        onView(withId(android.R.id.button1))
                .inRoot(RootMatchers.isDialog())
                .perform(click());

        // Set Firefox Lite as default browser in 'Open with' panel
        chooseAsDefaultBrowserViaOpenLink(activity.getString(R.string.app_name));

        // Now launch Firefox Lite's setting activity
        settingsActivity.launchActivity((new Intent().setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)));

        // Check if the "Default Browser" pref is correctly displayed (switch checked)
        onData(allOf(
                is(instanceOf(Preference.class)),
                withKey(prefName))).
                onChildView(withClassName(is(Switch.class.getName()))).check(matches(isChecked()));
    }

    private void chooseAsDefaultBrowserViaOpenLink(String appName) {
        final UiScrollable browserList = new UiScrollable(new UiSelector());

        final UiSelector browserView = new UiSelector().text(appName);
        try {
            browserList.scrollIntoView(browserView);
            final UiObject browserCandidate = uiDevice.findObject(browserView);
            browserCandidate.click();

            final UiObject alwaysButton = uiDevice
                    .findObject(new UiSelector().text("ALWAYS"));
            alwaysButton.click();

        } catch (UiObjectNotFoundException e) {
            throw new AssertionError("Could find the " + appName + " in 'Open with' browser panel", e);
        }
    }

    private void chooseLiteAsDefaultBrowser(MainActivity activity) {
        final UiScrollable browserList = new UiScrollable(new UiSelector());

        final UiSelector rocketView = new UiSelector().text(activity.getString(R.string.app_name));
        try {
            browserList.scrollIntoView(rocketView);
            final UiObject browserCandidate = uiDevice.findObject(rocketView);
            browserCandidate.click();

        } catch (UiObjectNotFoundException e) {
            throw new AssertionError("Could find the Firefox Lite app in default browser", e);
        }
    }

    @NonNull
    private UiObject openDefaultBrowserAndroidSetting(UiObject allAppsButton) {

        try {
            allAppsButton.click();

        } catch (UiObjectNotFoundException e) {
            throw new AssertionError("Could find the setting", e);
        }
        return allAppsButton;
    }

    private void clickDefaultBrowserSetting(String prefName) {
        onData(allOf(
                is(instanceOf(Preference.class)),
                withKey(prefName))).
                onChildView(withClassName(is(Switch.class.getName()))).perform(click());
    }

}
