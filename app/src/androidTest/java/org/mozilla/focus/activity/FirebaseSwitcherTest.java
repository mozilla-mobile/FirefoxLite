package org.mozilla.focus.activity;


import android.content.Intent;
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
public class FirebaseSwitcherTest {

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

    @Test
    public void changeDefaultBrowser_prefSwitched() {

        // Start MainActivity
        mainActivity.launchActivity(new Intent());
        final MainActivity activity = mainActivity.getActivity();
        if (activity == null) {
            throw new AssertionError("Could start activity");
        }

        final String prefName = activity.getString(R.string.pref_key_telemetry);


        // Now launch Rocket's setting activity
        settingsActivity.launchActivity(new Intent());

        // Click on the switch multiple times...
        onData(allOf(
                is(instanceOf(Preference.class)),
                withKey(prefName))).
                onChildView(withClassName(is(Switch.class.getName()))).check(matches(isChecked()));

        // first time, should get true from bind
        // inject the waiting blocking function
        // second time, should get false
        // third time, should get false
        // wait until the task is done
        // check the state, should be synced.

    }

}