/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class MimeUtilsTest {

    @Test
    public void testEmpty() {
        assertFalse(MimeUtils.isText(null));
        assertFalse(MimeUtils.isImage(null));
        assertFalse(MimeUtils.isAudio(null));
        assertFalse(MimeUtils.isVideo(null));
    }

    @Test
    public void testText() {
        assertTrue(MimeUtils.isText("text/*"));
        assertTrue(MimeUtils.isText("text/plain"));
        assertTrue(MimeUtils.isText("text/html"));

        assertFalse(MimeUtils.isText("text/"));
        assertFalse(MimeUtils.isText("text/*/"));
        assertFalse(MimeUtils.isText("text/plain/"));
        assertFalse(MimeUtils.isText("text/plain/*"));
        assertFalse(MimeUtils.isText("text/*/*"));
    }

    @Test
    public void testImage() {
        assertTrue(MimeUtils.isImage("image/*"));
        assertTrue(MimeUtils.isImage("image/png"));
        assertTrue(MimeUtils.isImage("image/jpg"));

        assertFalse(MimeUtils.isImage("image/"));
        assertFalse(MimeUtils.isImage("image/*/"));
        assertFalse(MimeUtils.isImage("image/png/"));
        assertFalse(MimeUtils.isImage("image/png/*"));
        assertFalse(MimeUtils.isImage("image/*/*"));
    }

    @Test
    public void testAudio() {
        assertTrue(MimeUtils.isAudio("audio/*"));
        assertTrue(MimeUtils.isAudio("audio/wav"));
        assertTrue(MimeUtils.isAudio("audio/mp3"));

        assertFalse(MimeUtils.isAudio("audio/"));
        assertFalse(MimeUtils.isAudio("audio/*/"));
        assertFalse(MimeUtils.isAudio("audio/mp3/"));
        assertFalse(MimeUtils.isAudio("audio/mp3/*"));
        assertFalse(MimeUtils.isAudio("audio/*/*"));
    }

    @Test
    public void testVideo() {
        assertTrue(MimeUtils.isVideo("video/*"));
        assertTrue(MimeUtils.isVideo("video/mp4"));
        assertTrue(MimeUtils.isVideo("video/avi"));

        assertFalse(MimeUtils.isVideo("video/"));
        assertFalse(MimeUtils.isVideo("video/*/"));
        assertFalse(MimeUtils.isVideo("video/mp4/"));
        assertFalse(MimeUtils.isVideo("video/mp4/*"));
        assertFalse(MimeUtils.isVideo("video/*/*"));
    }
}
