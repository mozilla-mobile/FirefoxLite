/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.activity;

import android.support.annotation.Keep;
import android.support.test.espresso.Espresso;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mozilla.focus.autobot.ScreenshotRobot;
import org.mozilla.focus.autobot.SessionRobot;
import org.mozilla.focus.utils.AndroidTestUtils;

@Keep
@RunWith(AndroidJUnit4.class)
public class TakeScreenshotTest {


    private static final String TARGET_URL_SITE = "file:///android_asset/gpl.html";

    @Before
    public void setUp() {
        AndroidTestUtils.beforeTest();
    }


    @Test
    public void takeScreenshot_screenshotIsCaptured () {

        SessionRobot sessionRobot = new SessionRobot();

        sessionRobot.loadPage(TARGET_URL_SITE);

        final ScreenshotRobot screenshotRobot = sessionRobot.takeScreenshot();

        screenshotRobot.clickMenuMyShots();

        screenshotRobot.clickFirstItemInMyShotsAndOpen();

        screenshotRobot.longClickAndDeleteTheFirstItemInMyShots();

        // Back to home
        Espresso.pressBack();
    }


}