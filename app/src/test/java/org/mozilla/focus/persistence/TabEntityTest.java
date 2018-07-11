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
public class TabEntityTest {

    @Test
    public void testSanity() {
        Assert.assertFalse(new TabEntity(null, null).isValid());
        Assert.assertFalse(new TabEntity(null, null, "title", "").isValid());
        Assert.assertFalse(new TabEntity(null, null, null, "url").isValid());
        Assert.assertFalse(new TabEntity("id", null, null, null).isValid());

        Assert.assertTrue(new TabEntity("id", null, null, "url").isValid());
        Assert.assertTrue(new TabEntity("id", null, "title", "url").isValid());
    }
}
