/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.support.annotation.Keep;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.Inject;
import org.mozilla.focus.R;
import org.mozilla.focus.history.model.Site;
import org.mozilla.focus.utils.TopSitesUtils;

import java.util.List;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.hasDescendant;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.mozilla.focus.test.TestUtil.atPosition;

@Keep
@RunWith(AndroidJUnit4.class)
public class HomeTest {

    @Rule
    public ActivityTestRule<MainActivity> activityRule = new ActivityTestRule<>(MainActivity.class);

    @Test
    public void testTopSite() {
        final MainActivity context = activityRule.getActivity();

        final JSONArray jsonDefault;
        try {
            jsonDefault = new JSONArray(Inject.getDefaultTopSites(context));
            List<Site> defaultSites = TopSitesUtils.paresJsonToList(context, jsonDefault);
            onView(withId(R.id.main_list))
                    .check(matches(atPosition(0, hasDescendant(withText(defaultSites.get(0).getTitle())))));
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}