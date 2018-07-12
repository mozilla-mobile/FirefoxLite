/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.tabs.utils;

import junit.framework.Assert;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class TabUtilTest {

    @Test
    public void testCreation() {
        Assert.assertNotNull(TabUtil.argument("parent_id", false, false));
        Assert.assertNotNull(TabUtil.argument(null, false, false));
        Assert.assertNotNull(TabUtil.argument(null, false, true));
        Assert.assertNotNull(TabUtil.argument(null, true, false));
    }

    @Test
    public void testReturnValue() {
        Assert.assertNull(TabUtil.getParentId(TabUtil.argument(null, true, false)));
        Assert.assertEquals("foobar", TabUtil.getParentId(TabUtil.argument("foobar", true, false)));
        Assert.assertTrue(TabUtil.isFromExternal(TabUtil.argument(null, true, false)));
        Assert.assertFalse(TabUtil.isFromExternal(TabUtil.argument(null, false, false)));
        Assert.assertTrue(TabUtil.toFocus(TabUtil.argument(null, false, true)));
        Assert.assertFalse(TabUtil.toFocus(TabUtil.argument(null, false, false)));
    }
}
