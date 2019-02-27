package org.mozilla.focus.activity;


import android.content.Intent;
import android.content.res.Resources;
import android.preference.Preference;
import android.support.annotation.NonNull;
import android.support.test.rule.ActivityTestRule;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiScrollable;
import android.support.test.uiautomator.UiSelector;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.BeforeTestTask;
import org.mozilla.focus.utils.AndroidTestUtils;

import static android.support.test.InstrumentationRegistry.getInstrumentation;
import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.PreferenceMatchers.withKey;
import static android.support.test.espresso.matcher.RootMatchers.isDialog;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;

@Ignore
public class ChangeLanguageTest {

    // This is the lang name of Android system. We hard code it here so it will only fits emulator
    // For physical devices this may vary.
    private final String INDOLAN = "Indonesia";
    private final String ENGUSLAN = "English (United States)";

    private UiDevice uiDevice;

    @Rule
    public final ActivityTestRule<SettingsActivity> settingsActivity = new ActivityTestRule<>(SettingsActivity.class, false, false);

    @Rule
    public ActivityTestRule<MainActivity> mainActivity = new ActivityTestRule<>(MainActivity.class, true, false);

    @Before
    public void setup() {
        new BeforeTestTask.Builder()
                .build()
                .execute();
        uiDevice = UiDevice.getInstance(getInstrumentation());
        mainActivity.launchActivity(new Intent());
    }

    /**
     * Test case no: TC0026
     * Test case name: Change display language
     * Steps:
     * 1. Launch app
     * 2. Tap on menu -> settings
     * 3. Tap on language
     * 4. Check language dialog displayed
     * 5. Choose English(US)
     * 6. Check English(US) displayed
     * 7. Tap language and choose Indonesian
     * 8. Check Indonesian displayed
     */
    @Test
    public void changeDisplayLang() {
        // To do : we hard code the language may apply getPackagedLocaleTags(getContext()) later
        // Start MainActivity
        final MainActivity activity = mainActivity.getActivity();

        if (activity == null) {
            throw new AssertionError("Could start activity");
        }

        // Tap menu -> settings
        AndroidTestUtils.tapBrowserMenuButton();
        onView(withId(R.id.menu_preferences)).perform(click());

        // Tap language
        Resources resources = activity.getResources();
        onData(withKey(resources.getString(R.string.pref_key_locale))).perform(click());

        // Check dialog displayed
        onView(withText(R.string.preference_language)).inRoot(isDialog()).check(matches(isDisplayed()));

        // Scroll to choose Indonesia and check it displayed
        scrollToTapLang(activity, ENGUSLAN);

        // Tap language
        onData(withKey(resources.getString(R.string.pref_key_locale))).perform(click());

        // Scroll to choose English(US) and check it displayed
        scrollToTapLang(activity, INDOLAN);

        // Set it back to English (United States)
        onData(withKey(resources.getString(R.string.pref_key_locale))).perform(click());
        scrollToTapLang(activity, ENGUSLAN);
    }

    @NonNull
    private UiScrollable scrollToTapLang(MainActivity activity, String lang) {

        // Scroll to choose lang
        final UiScrollable langList = new UiScrollable(new UiSelector());
        final UiSelector langView = new UiSelector().text(lang);

        try {
            langList.scrollIntoView(langView);
            final UiObject langCandidate = uiDevice.findObject(langView);
            langCandidate.click();

        } catch (UiObjectNotFoundException e) {
            throw new AssertionError(String.format("Could not find %s in system language dialog", lang), e);
        }

        // Check lang displayed
        onData(allOf(
                is(instanceOf(Preference.class)),
                withKey(activity.getResources().getString(R.string.pref_key_locale)))).
                onChildView(withText(lang)).check(matches(isDisplayed()));
        return langList;
    }
}
