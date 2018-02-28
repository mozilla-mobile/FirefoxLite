package org.mozilla.focus.activity;


import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.preference.Preference;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.widget.Switch;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.activity.SettingsActivity;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.Settings;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;

// This test is for API 27 only cause I want restrict the system default browser settings(SETTING_DEFAULT_BROWSER)
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 27, maxSdkVersion = 27)
public class DefaultBrowserTest {

    private final static String SETTING_DEFAULT_BROWSER = "Browser app";

    private UiDevice uiDevice;

    private ActivityTestRule<SettingsActivity> settingsActivity = new ActivityTestRule<>(SettingsActivity.class, false, false);

    @Rule
    public ActivityTestRule<MainActivity> mainActivity = new ActivityTestRule<>(MainActivity.class);

    @Before
    public void setup() {
        uiDevice = UiDevice.getInstance(getInstrumentation());

        AndroidTestUtils.beforeTest(mainActivity.getActivity());
    }

    @Test
    public void testDefaultBrowserSetting() {

        MainActivity activity = mainActivity.getActivity();
        if (activity == null) {
            throw new AssertionError("Could start activity");
        }
        final String prefName = activity.getString(R.string.pref_key_default_browser);

        onView(withId(R.id.btn_menu)).perform(click());
        onView(withId(R.id.menu_preferences)).perform(click());
        onData(allOf(
                is(instanceOf(Preference.class)),
                withKey(prefName))).
                onChildView(withClassName(is(Switch.class.getName()))).perform(click());

        // Setting : set default browser
        final UiObject allAppsButton = uiDevice
                .findObject(new UiSelector().text(SETTING_DEFAULT_BROWSER));

        try {
            allAppsButton.click();

        } catch (UiObjectNotFoundException e) {
            throw new AssertionError("Could find the setting", e);
        }

        final UiObject browserCandidate = uiDevice
                .findObject(new UiSelector().text(activity.getString(R.string.app_name)));

        try {
            browserCandidate.click();

        } catch (UiObjectNotFoundException e) {
            throw new AssertionError("Could find the app in default browser", e);
        }

        settingsActivity.launchActivity(new Intent());

        onData(allOf(
                is(instanceOf(Preference.class)),
                withKey(prefName))).
                onChildView(withClassName(is(Switch.class.getName()))).check(matches(isChecked()));

    }

}