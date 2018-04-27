package org.mozilla.focus.activity;


import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;
import android.widget.Switch;

import com.squareup.leakcanary.LeakCanary;

import org.bouncycastle.crypto.util.Pack;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.FirebaseEnablerIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.FirebaseHelper;

import static android.support.test.InstrumentationRegistry.getContext;
import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withParent;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;

// Only device with API>=24 can set default browser via system settings
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 24, maxSdkVersion = 27)
public class FirebaseSwitcherTest {

    // idling resource for firebase enabler
    FirebaseEnablerIdlingResource idlingResource;

    // pref key for send usage data
    String prefName;

    @Rule
    public final ActivityTestRule<SettingsActivity> settingsActivity = new ActivityTestRule<>(SettingsActivity.class, false, false);

    @Rule
    public ActivityTestRule<MainActivity> mainActivity = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setup() {
        AndroidTestUtils.beforeTest();

        // set the pref name for later use
        final Context context = InstrumentationRegistry.getContext();
        prefName = context.getString(R.string.pref_key_telemetry);

        // set idlingResource for Firebase enabler
        idlingResource = new FirebaseEnablerIdlingResource();

        // make sure the pref is on when the app starts
        resetPref(true);
    }

    @After
    public void tearDown() {

        // make sure the pref is off when the app starts
        resetPref(false);

        if (idlingResource != null) {
            // unregister again if any surprise happens during the test
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    // this is the happy case:
    @Test
    public void disableFirebase_makeSureSwitchIsOffAfterClick() {

        // prepare for the view to interact
        DataInteraction view = prepareForView();

        // make sure Send Usage Data pref' switch is checked
        view.check(matches(isChecked()));

        // after click on the pref
        view.perform(click());

        // wait for completion
        IdlingRegistry.getInstance().register(idlingResource);

        // now the pref should be unchecked
        view.check(matches(isNotChecked()));

    }

    private DataInteraction prepareForView() {
        // Now launch Rocket's setting activity
        settingsActivity.launchActivity(new Intent());

        // This make FirebaseHelper aware of idlingResource
        FirebaseHelper.injectEnablerCallback(idlingResource);

        // Click on the switch multiple times...
        return onData(allOf(
                is(instanceOf(Preference.class)),
                withKey(prefName))).
                onChildView(withClassName(is(Switch.class.getName())));
    }

    @Test
    public void flipPrefCrazily_OnlyOneRunnableIsCreated() {

        // prepare for the view to interact
        DataInteraction view = prepareForView();

        // make sure Send Usage Data pref' switch is checked ( the initial state)
        view.check(matches(isChecked()));

        // I've added some latency to the enabler, in case it runs too fast
        // This is done via BlockingEnabler's interface that IdlingResource implements.

        // The state is not changed, but we still want keep the same status and call bind again to kick off the enabler.
        // I use bind() to simulate the click. This is because calling click can't let get the return value
        // of the bind method ( I use it to deteremine if a new Runnable is created)
        boolean newRunnableCreated = FirebaseHelper.bind(getContext());
        // Only this time will be true
        // first time, should get true from bind
        assertTrue(newRunnableCreated);

        // the successors should return false( not create new runnable)
        // assume below three method calls happens very fast
        newRunnableCreated = FirebaseHelper.bind(getContext());
        assertFalse(newRunnableCreated);

        newRunnableCreated = FirebaseHelper.bind(getContext());
        // second time, should get false
        assertFalse(newRunnableCreated);

        newRunnableCreated = FirebaseHelper.bind(getContext());
        // third time, should get false
        assertFalse(newRunnableCreated);

        // Now we wait for the enabler to completes
        IdlingRegistry.getInstance().register(idlingResource);

        // now waits for the idling resource to complete, then check again

        // now the pref should be unchecked
        view.check(matches(isNotChecked()));

        // check the state, should be synced.
    }

    @Test
    public void flipPrefCrazily_TheStateIsSynced() {

        // prepare for the view to interact
        DataInteraction view = prepareForView();

        // make sure Send Usage Data pref' switch is checked ( the initial state)
        view.check(matches(isChecked()));

        // after this, the state is off
        view.perform(click());

        // after this, the state is on
        view.perform(click());

        // after this, the state is off
        view.perform(click());

        // after this, the state is on
        view.perform(click());

        // Now we wait for the enabler to completes
        IdlingRegistry.getInstance().register(idlingResource);

        // now the pref should be checked
        view.check(matches(isChecked()));

    }


    private void resetPref(boolean enable) {
        final Context context = InstrumentationRegistry.getContext();
        final String prefName = context.getString(R.string.pref_key_telemetry);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(prefName, enable).apply();

    }


}