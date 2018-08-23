/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.annotation.SuppressLint;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class UrlUtilsTest {
    @Test
    public void urlsMatchExceptForTrailingSlash() throws Exception {
        assertTrue(UrlUtils.urlsMatchExceptForTrailingSlash("http://www.mozilla.org", "http://www.mozilla.org"));
        assertTrue(UrlUtils.urlsMatchExceptForTrailingSlash("http://www.mozilla.org/", "http://www.mozilla.org"));
        assertTrue(UrlUtils.urlsMatchExceptForTrailingSlash("http://www.mozilla.org", "http://www.mozilla.org/"));

        assertFalse(UrlUtils.urlsMatchExceptForTrailingSlash("http://mozilla.org", "http://www.mozilla.org"));
        assertFalse(UrlUtils.urlsMatchExceptForTrailingSlash("http://www.mozilla.org/", "http://mozilla.org"));

        assertFalse(UrlUtils.urlsMatchExceptForTrailingSlash("http://www.mozilla.org", "https://www.mozilla.org"));
        assertFalse(UrlUtils.urlsMatchExceptForTrailingSlash("https://www.mozilla.org", "http://www.mozilla.org"));

        // Same length of domain, but otherwise different:
        assertFalse(UrlUtils.urlsMatchExceptForTrailingSlash("http://www.allizom.org", "http://www.mozilla.org"));
        assertFalse(UrlUtils.urlsMatchExceptForTrailingSlash("http://www.allizom.org/", "http://www.mozilla.org"));
        assertFalse(UrlUtils.urlsMatchExceptForTrailingSlash("http://www.allizom.org", "http://www.mozilla.org/"));

        // Check upper/lower case is OK:
        assertTrue(UrlUtils.urlsMatchExceptForTrailingSlash("http://www.MOZILLA.org", "http://www.mozilla.org"));
        assertTrue(UrlUtils.urlsMatchExceptForTrailingSlash("http://www.MOZILLA.org/", "http://www.mozilla.org"));
        assertTrue(UrlUtils.urlsMatchExceptForTrailingSlash("http://www.MOZILLA.org", "http://www.mozilla.org/"));
    }

    @Test
    public void isPermittedResourceProtocol() {
        assertTrue(UrlUtils.isPermittedResourceProtocol("http"));
        assertTrue(UrlUtils.isPermittedResourceProtocol("https"));

        assertTrue(UrlUtils.isPermittedResourceProtocol("data"));
        assertTrue(UrlUtils.isPermittedResourceProtocol("file"));

        assertFalse(UrlUtils.isPermittedResourceProtocol("nielsenwebid"));
    }

    @Test
    public void isPermittedProtocol() {
        assertTrue(UrlUtils.isSupportedProtocol("http"));
        assertTrue(UrlUtils.isSupportedProtocol("https"));
        assertTrue(UrlUtils.isSupportedProtocol("error"));
        assertTrue(UrlUtils.isSupportedProtocol("data"));

        assertFalse(UrlUtils.isSupportedProtocol("market"));
    }

    @Test
    public void testIsSearchQuery() {
        assertTrue(UrlUtils.isSearchQuery("hello world"));

        assertFalse(UrlUtils.isSearchQuery("mozilla.org"));
        assertFalse(UrlUtils.isSearchQuery("mozilla"));
    }

    @Test
    @SuppressLint("AuthLeak")
    public void testStripUserInfo() {
        assertEquals("", UrlUtils.stripUserInfo(null));
        assertEquals("", UrlUtils.stripUserInfo(""));

        assertEquals("https://www.mozilla.org", UrlUtils.stripUserInfo("https://user:password@www.mozilla.org"));
        assertEquals("https://www.mozilla.org", UrlUtils.stripUserInfo("https://user@www.mozilla.org"));

        assertEquals("user@mozilla.org", UrlUtils.stripUserInfo("user@mozilla.org"));

        assertEquals("ftp://mozilla.org", UrlUtils.stripUserInfo("ftp://user:password@mozilla.org"));

        assertEquals("öäü102ß", UrlUtils.stripUserInfo("öäü102ß"));
    }

    @Test
    public void isInternalErrorURL() {
        assertTrue(UrlUtils.isInternalErrorURL("data:text/html;charset=utf-8;base64,"));

        assertFalse(UrlUtils.isInternalErrorURL("http://www.mozilla.org"));
        assertFalse(UrlUtils.isInternalErrorURL("https://www.mozilla.org/en-us/about"));
        assertFalse(UrlUtils.isInternalErrorURL("www.mozilla.org"));
        assertFalse(UrlUtils.isInternalErrorURL("error:-8"));
        assertFalse(UrlUtils.isInternalErrorURL("hello world"));
    }

    @Test
    public void isHttpOrHttpsUrl() {
        assertFalse(UrlUtils.isHttpOrHttps(null));
        assertFalse(UrlUtils.isHttpOrHttps(""));
        assertFalse(UrlUtils.isHttpOrHttps("     "));
        assertFalse(UrlUtils.isHttpOrHttps("mozilla.org"));
        assertFalse(UrlUtils.isHttpOrHttps("httpstrf://example.org"));

        assertTrue(UrlUtils.isHttpOrHttps("https://www.mozilla.org"));
        assertTrue(UrlUtils.isHttpOrHttps("http://example.org"));
        assertTrue(UrlUtils.isHttpOrHttps("http://192.168.0.1"));
    }

    @Test
    public void stripCommonSubdomains() {
        assertEquals(UrlUtils.stripCommonSubdomains("m.mobile.com"), "mobile.com");
        assertEquals(UrlUtils.stripCommonSubdomains("mobile.mozilla.org"), "mozilla.org");
        assertEquals(UrlUtils.stripCommonSubdomains("www.synology.com"), "synology.com");
        assertEquals(UrlUtils.stripCommonSubdomains("i.j.k"), "i.j.k");
    }


    @Test
    public void stripHttp() {
        assertEquals(UrlUtils.stripHttp("http://我的.首頁"), "我的.首頁");
        assertEquals(UrlUtils.stripHttp("https://mobile.mozilla.org"), "mobile.mozilla.org");
        assertEquals(UrlUtils.stripHttp("ftp://quickconnect.to"), "ftp://quickconnect.to");
        assertEquals(UrlUtils.stripHttp("synology.com"), "synology.com");
    }
}
