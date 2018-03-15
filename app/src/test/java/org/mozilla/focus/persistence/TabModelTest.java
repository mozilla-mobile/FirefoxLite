/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.persistence;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class TabModelTest {

    @Test
    public void testSanity() {
        Assert.assertFalse(TabModel.isSane(new TabModel(null, null)));
        Assert.assertFalse(TabModel.isSane(new TabModel(null, null, "title", "")));
        Assert.assertFalse(TabModel.isSane(new TabModel(null, null, null, "url")));
        Assert.assertFalse(TabModel.isSane(new TabModel("id", null, null, null)));

        Assert.assertTrue(TabModel.isSane(new TabModel("id", null, null, "url")));
        Assert.assertTrue(TabModel.isSane(new TabModel("id", null, "title", "url")));
    }
}
