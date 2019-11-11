package org.mozilla.focus.activity;


import android.content.Intent;

import androidx.test.espresso.intent.rule.IntentsTestRule;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.utils.AndroidTestUtils;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasAction;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.matcher.PreferenceMatchers.withKey;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.Is.is;

public class ShareWithFriendTest {

    @Rule
    public IntentsTestRule<MainActivity> mainActivity = new IntentsTestRule<>(MainActivity.class, true, false);

    @Before
    public void setup() {
        new BeforeTestTask.Builder()
                .build()
                .execute();
        mainActivity.launchActivity(new Intent());
    }

    /**
     * Test case no: TC0026
     * Test case name: Share with friends
     * Steps:
     * 1. Launch app
     * 2. Tap menu -> settings
     * 3. Tap share with friends -> close
     * 4. Tap share with friends -> share
     * 5. Check intent sent
     */
    @Test
    public void shareWithFriends() {

        // Start MainActivity
        final MainActivity activity = mainActivity.getActivity();


        if (activity == null) {
            throw new AssertionError("Could start activity");
        }

        // Tap menu -> settings
        AndroidTestUtils.tapHomeMenuButton();
        AndroidTestUtils.tapSettingButton();

        // Tap share with friends -> close
        onData(withKey(activity.getResources().getString(R.string.pref_key_share_with_friends))).perform(click());
        onView(withId(R.id.close_button)).perform(click());

        // Tap share with friends -> share
        onData(withKey(activity.getResources().getString(R.string.pref_key_share_with_friends))).perform(click());
        onView(withId(R.id.positive_button)).perform(click());

        // Check intent sent
        intended(allOf(hasAction(Intent.ACTION_CHOOSER), hasExtra(is(Intent.EXTRA_INTENT), allOf(hasAction(Intent.ACTION_SEND), hasExtra(Intent.EXTRA_TEXT, activity.getResources().getString(R.string.share_app_promotion_text, activity.getResources().getString(R.string.app_name), activity.getResources().getString(R.string.share_app_google_play_url), activity.getResources().getString(R.string.mozilla)))))));
    }
}
