package org.mozilla.focus.activity;


import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.DataInteraction;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.Switch;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.ActivityRecreateLeakWatcherIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;
import org.mozilla.focus.utils.FirebaseHelper;
import org.mozilla.focus.widget.TelemetrySwitchPreference;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isRoot;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;

// Only device with API>=24 can set default browser via system settings
@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 24, maxSdkVersion = 27)
public class FirebaseSwitcherTest {

    // idling resource for ActivityRecreateLeakWatcherIdlingResource
    private ActivityRecreateLeakWatcherIdlingResource leakWatchIdlingResource;

    @Rule
    public final ActivityTestRule<SettingsActivity> settingsActivity = new ActivityTestRule<>(SettingsActivity.class, false, false);

    @Before
    public void setup() {
        // make sure the pref is on when started
        resetPref();

        AndroidTestUtils.beforeTest();
    }

    @After
    public void tearDown() {
        if (leakWatchIdlingResource != null) {
            IdlingRegistry.getInstance().unregister(leakWatchIdlingResource);
        }
    }

    @Test
    public void disableFirebase_makeSureSwitchIsOffAfterClick() {

        // prepare for the view to interact
        DataInteraction view = prepareForView();

        // make sure Send Usage Data pref' switch is checked
        view.check(matches(isChecked()));

        // after click on the pref
        view.perform(click());

        // now the pref should be unchecked
        view.check(matches(isNotChecked()));
    }

    private DataInteraction prepareForView() {
        // Now launch Rocket's setting activity
        settingsActivity.launchActivity(new Intent());

//        FirebaseHelper.init(settingsActivity.getActivity());

        FirebaseHelper.injectEnablerCallback(new Delay());

        // Click on the switch multiple times...
        return onData(
                is(instanceOf(TelemetrySwitchPreference.class))).
                onChildView(withClassName(is(Switch.class.getName())));
    }

    // call bind() multiple times to see if any runnable is created.
    // no actual click was performed. This test case here is not really a UI test.
    @Test
    public void callBindCrazily_OnlyOneRunnableIsCreated() {

        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();

        // prepare for the view to interact
        DataInteraction view = prepareForView();

        // make sure Send Usage Data pref' switch is checked ( the initial state)
        view.check(matches(isChecked()));

        // I've added some latency to the enabler, in case it runs too fast
        // This is done via BlockingEnabler's interface that IdlingResource implements.

        // The state is not changed, but we still want keep the same status and call bind again to kick off the enabler.
        // I use bind() to simulate the click. This is because calling click can't let get the return value
        // of the bind method ( I use it to determine if a new Runnable is created)
        boolean newRunnableCreated = FirebaseHelper.bind(context);
        // Only this time will be true
        // first time, should get true from bind
        assertTrue(newRunnableCreated);

        // the successors should return false( not create new runnable)
        // assume below three method calls happens very fast
        newRunnableCreated = FirebaseHelper.bind(context);
        assertFalse(newRunnableCreated);

        newRunnableCreated = FirebaseHelper.bind(context);
        // second time, should get false
        assertFalse(newRunnableCreated);

        newRunnableCreated = FirebaseHelper.bind(context);
        // third time, should get false
        assertFalse(newRunnableCreated);

        // calling onView will make sure the idlingResource will completes its operation
        view.check(matches(isChecked()));

        // this time it will be true cause the previous one is done
        newRunnableCreated = FirebaseHelper.bind(context);

        // Only this time will be true
        // first time, should get true from bind
        assertTrue(newRunnableCreated);

        // waits for bind() completes
        view.check(matches(isChecked()));
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

        // now the pref should be checked
        view.check(matches(isChecked()));

    }

    @Test
    public void flipAndLeave_ShouldHaveNoLeak() {
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

        // shouldn't leak SettingsActivity if SettingsActivity is recreated before the task completed.
        leakWatchIdlingResource = new ActivityRecreateLeakWatcherIdlingResource(settingsActivity.getActivity());

        // re create the activity to force the current one to call onDestroy.
        // two things are happening here:
        // 1. the dying one is being tracked
        // 2. when the new activity is created, idling resource will check if the old one is cleared.
        settingsActivity.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                settingsActivity.getActivity().recreate();
            }
        });

        // leakWatchIdlingResource will be idle is gc is completed.
        IdlingRegistry.getInstance().register(leakWatchIdlingResource);

        // call onView to sync and wait for idling resource
        onView(isRoot());

        // now there should be no leak
        assertFalse(leakWatchIdlingResource.hasLeak());

        IdlingRegistry.getInstance().unregister(leakWatchIdlingResource);

    }


    // in debug, this pref is off by default, We can either reset it before we run the test, or inject
    // the implementation in the original code
    private void resetPref() {
        final Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        final String prefName = context.getString(R.string.pref_key_telemetry);
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(prefName, true).apply();

    }

    // we delay BlockingEnabler to simulate slow network
    private static class Delay implements FirebaseHelper.BlockingEnablerCallback {
        @Override
        public void runDelayOnExecution() {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }


}