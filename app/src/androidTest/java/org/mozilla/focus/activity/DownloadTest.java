/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.content.Context;
import android.preference.PreferenceManager;
import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.support.test.uiautomator.UiObject;
import android.support.test.uiautomator.UiObjectNotFoundException;
import android.support.test.uiautomator.UiSelector;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.R;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.helper.TestHelper;
import org.mozilla.focus.utils.AndroidTestUtils;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.pressImeActionButton;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.mozilla.focus.fragment.FirstrunFragment.FIRSTRUN_PREF;

@RunWith(AndroidJUnit4.class)
public class DownloadTest {
    private static final String TEST_PATH = "/";

    private Context appContext;
    private MockWebServer webServer;
    private SessionLoadedIdlingResource loadingIdlingResource;

    @Before
    public void setUp() {
        AndroidTestUtils.beforeTest(activityRule.getActivity());

        loadingIdlingResource = new SessionLoadedIdlingResource(activityRule.getActivity());

    }

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<MainActivity>(MainActivity.class) {
        @Override
        protected void beforeActivityLaunched() {
            super.beforeActivityLaunched();

            appContext = InstrumentationRegistry.getInstrumentation()
                    .getTargetContext()
                    .getApplicationContext();

            PreferenceManager.getDefaultSharedPreferences(appContext)
                    .edit()
                    .putBoolean(FIRSTRUN_PREF, true)
                    .apply();

            webServer = new MockWebServer();

            try {
                webServer.enqueue(new MockResponse()
                        .setBody(TestHelper.readTestAsset("image_test.html"))
                        .addHeader("Set-Cookie", "sphere=battery; Expires=Wed, 21 Oct 2035 07:28:00 GMT;"));
                webServer.enqueue(new MockResponse()
                        .setBody(TestHelper.readTestAsset("rabbit.jpg")));
                webServer.enqueue(new MockResponse()
                        .setBody(TestHelper.readTestAsset("download.jpg")));
                webServer.enqueue(new MockResponse()
                        .setBody(TestHelper.readTestAsset("download.jpg")));

                webServer.start();
            } catch (IOException e) {
                throw new AssertionError("Could not start web server", e);
            }
        }

        @Override
        protected void afterActivityFinished() {
            super.afterActivityFinished();

            try {
                webServer.close();
                webServer.shutdown();
            } catch (IOException e) {
                throw new AssertionError("Could not stop web server", e);
            }
        }
    };


    @Test
    public void DownloadFileTest() throws InterruptedException, UiObjectNotFoundException, IOException {

        onView(withId(R.id.home_fragment_fake_input)).perform(click());
        onView(withId(R.id.url_edit)).perform(replaceText(webServer.url(TEST_PATH).toString()), pressImeActionButton());
        // IdlingRegistry.getInstance().register(loadingIdlingResource);
        final UiObject downloadIcon = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation()).findObject(new UiSelector()
                .resourceId("rabbitImage"));
        downloadIcon.click();

    }
}
