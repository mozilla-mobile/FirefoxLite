/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.Manifest;
import android.content.Intent;
import android.support.test.espresso.IdlingRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mozilla.focus.generated.LocaleList;
import org.mozilla.focus.helper.SessionLoadedIdlingResource;
import org.mozilla.focus.utils.AndroidTestUtils;

import java.util.Locale;


public class DefaultSearchEngineTest {

    @Rule
    public final GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);

    @Rule
    public final ActivityTestRule<MainActivity> activityTestRule = new ActivityTestRule<>(MainActivity.class, true, false);

    private static final String SEARCH_KEYWORD = "zerda";

    private static final String DEFAULT_ENGINE = "google";

    private SessionLoadedIdlingResource loadingIdlingResource;

    private Locale defaultLocale;

    @Before
    public void setUp() {
        AndroidTestUtils.beforeTest();
        defaultLocale = Locale.getDefault();
    }

    @After
    public void tearDown() {
        if (defaultLocale != null) {
            Locale.setDefault(defaultLocale);
        }
    }

    @Test
    public void defaultSearchEngineShouldBeGoogle() {

        activityTestRule.launchActivity(new Intent());

        for (String locale : LocaleList.BUNDLED_LOCALES) {
            Locale.setDefault(new Locale(locale));

            loadingIdlingResource = new SessionLoadedIdlingResource(activityTestRule.getActivity());

            // Tap search field
            AndroidTestUtils.tapHomeSearchField();

            // Type search keyword and browse
            AndroidTestUtils.typeTextInSearchFieldAndGo(SEARCH_KEYWORD);

            IdlingRegistry.getInstance().register(loadingIdlingResource);

            // Check is url contains search keyword
            AndroidTestUtils.urlBarContainsText(SEARCH_KEYWORD);

            // Check is url contains search engine name
            AndroidTestUtils.urlBarContainsText(DEFAULT_ENGINE);

            IdlingRegistry.getInstance().unregister(loadingIdlingResource);

            // Remove tab and back to home
            AndroidTestUtils.removeNewAddedTab();
        }
    }
}