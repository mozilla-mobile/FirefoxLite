/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.pm.PackageManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.util.Locale;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class SupportUtilsTest {

    @Test
    public void cleanup() {
        // Other tests might get confused by our locale fiddling, so lets go back to the default:
        Locale.setDefault(Locale.ENGLISH);
    }


    @Test
    public void testNormalize() {
        assertEquals("http://www.mozilla.org", SupportUtils.normalize("http://www.mozilla.org"));
        assertEquals("https://www.mozilla.org", SupportUtils.normalize("https://www.mozilla.org"));
        assertEquals("https://www.mozilla.org/en-US/internet-health/", SupportUtils.normalize("https://www.mozilla.org/en-US/internet-health/"));
        assertEquals("file:///mnt/sdcard/", SupportUtils.normalize("file:///mnt/sdcard/"));

        assertEquals("http://mozilla.org", SupportUtils.normalize("mozilla.org"));
        assertEquals("http://mozilla.org", SupportUtils.normalize("http://mozilla.org "));
        assertEquals("http://mozilla.org", SupportUtils.normalize(" http://mozilla.org "));
        assertEquals("http://mozilla.org", SupportUtils.normalize(" http://mozilla.org"));
        assertEquals("http://localhost", SupportUtils.normalize("localhost"));

        // for supported/predefined url, we should not change them at all.
        for (final String supported : SupportUtils.SUPPORTED_URLS) {
            assertEquals(supported, SupportUtils.normalize(supported));
        }
    }

    @Test
    public void testIsUrl() {
        assertTrue(SupportUtils.isUrl("http://www.mozilla.org"));
        assertTrue(SupportUtils.isUrl("https://www.mozilla.org"));
        assertTrue(SupportUtils.isUrl("https://www.mozilla.org "));
        assertTrue(SupportUtils.isUrl(" https://www.mozilla.org"));
        assertTrue(SupportUtils.isUrl(" https://www.mozilla.org "));
        assertTrue(SupportUtils.isUrl("https://www.mozilla.org/en-US/internet-health/"));
        assertTrue(SupportUtils.isUrl("file:///mnt/sdcard/"));
        assertTrue(SupportUtils.isUrl("file:///"));
        assertTrue(SupportUtils.isUrl("file:///mnt"));
        assertTrue(SupportUtils.isUrl("mozilla.org"));
        assertTrue(SupportUtils.isUrl("192.168.1.1"));
        assertTrue(SupportUtils.isUrl("192.168.1.1:4000"));
        assertTrue(SupportUtils.isUrl("192.168.1.1/a/../b"));
        assertTrue(SupportUtils.isUrl("http://192.168.1.1"));
        assertTrue(SupportUtils.isUrl("http://192.168.1.1:4000"));
        assertTrue(SupportUtils.isUrl("http://192.168.1.1/a/../b"));
        assertTrue(SupportUtils.isUrl("localhost"));
        assertTrue(SupportUtils.isUrl("http://localhost"));
        assertTrue(SupportUtils.isUrl("about:blank"));
        assertTrue(SupportUtils.isUrl("focusabout:"));

        assertFalse(SupportUtils.isUrl("Hello World"));
        assertFalse(SupportUtils.isUrl("Mozilla"));
        assertFalse(SupportUtils.isUrl(""));
        assertFalse(SupportUtils.isUrl(" "));
        assertFalse(SupportUtils.isUrl(":"));
        assertFalse(SupportUtils.isUrl("."));
        assertFalse(SupportUtils.isUrl("::"));
        assertFalse(SupportUtils.isUrl("..."));
        assertFalse(SupportUtils.isUrl("file://:"));
    }

    /*
     * Super simple sumo URL test - it exists primarily to verify that we're setting the language
     * and page tags correctly. appVersion is null in tests, so we just test that there's a null there,
     * which doesn't seem too useful...
     */
    @Test
    public void getSumoURLForTopic() throws Exception {
        final String appVersion;
        try {
            appVersion = RuntimeEnvironment.application.getPackageManager().getPackageInfo(RuntimeEnvironment.application.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // This should be impossible - we should always be able to get information about ourselves:
            throw new IllegalStateException("Unable find package details for Rocket", e);
        }

        Locale.setDefault(Locale.GERMANY);
        assertEquals("https://support.mozilla.org/1/mobile/" + appVersion + "/Android/de-DE/foobar",
                SupportUtils.getSumoURLForTopic(RuntimeEnvironment.application, "foobar"));

        Locale.setDefault(Locale.CANADA_FRENCH);
        assertEquals("https://support.mozilla.org/1/mobile/" + appVersion + "/Android/fr-CA/foobar",
                SupportUtils.getSumoURLForTopic(RuntimeEnvironment.application, "foobar"));
    }

    /**
     * This is a pretty boring tests - it exists primarily to verify that we're actually setting
     * a langtag in the manfiesto URL.
     */
    @Test
    public void getManifestoURL() throws Exception {
        Locale.setDefault(Locale.UK);
        assertEquals("https://www.mozilla.org/en-GB/about/manifesto/",
                SupportUtils.getManifestoURL());

        Locale.setDefault(Locale.KOREA);
        assertEquals("https://www.mozilla.org/ko-KR/about/manifesto/",
                SupportUtils.getManifestoURL());
    }

}