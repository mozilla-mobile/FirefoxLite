/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web;

import android.content.pm.PackageManager;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class WebViewProviderTest {

    @Test
    public void testGetUABrowserString() {
        // Typical situation with a webview UA string from Android 5:
        String focusToken = "Focus/1.0";
        final String existing = "Mozilla/5.0 (Linux; Android 5.0.2; Android SDK built for x86_64 Build/LSY66K) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/37.0.0.0 Mobile Safari/537.36";
        assertEquals("AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 " + focusToken + " Chrome/37.0.0.0 Mobile Safari/537.36",
                WebViewProvider.getUABrowserString(existing, focusToken));

        // Make sure we can use any token, e.g Klar:
        focusToken = "Klar/2.0";
        assertEquals("AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 " + focusToken + " Chrome/37.0.0.0 Mobile Safari/537.36",
                WebViewProvider.getUABrowserString(existing, focusToken));

        // And a non-standard UA String, which doesn't contain AppleWebKit
        focusToken = "Focus/1.0";
        final String imaginaryKit = "Mozilla/5.0 (Linux) ImaginaryKit/-10 (KHTML, like Gecko) Version/4.0 Chrome/37.0.0.0 Mobile Safari/537.36";
        assertEquals("ImaginaryKit/-10 (KHTML, like Gecko) Version/4.0 " + focusToken + " Chrome/37.0.0.0 Mobile Safari/537.36",
                WebViewProvider.getUABrowserString(imaginaryKit, focusToken));

        // Another non-standard UA String, this time with no Chrome (in which case we should be appending focus)
        final String chromeless = "Mozilla/5.0 (Linux; Android 5.0.2; Android SDK built for x86_64 Build/LSY66K) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Imaginary/37.0.0.0 Mobile Safari/537.36";
        assertEquals("AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Imaginary/37.0.0.0 Mobile Safari/537.36 " + focusToken,
                WebViewProvider.getUABrowserString(chromeless, focusToken));

        // No AppleWebkit, no Chrome
        final String chromelessImaginaryKit = "Mozilla/5.0 (Linux) ImaginaryKit/-10 (KHTML, like Gecko) Version/4.0 Imaginary/37.0.0.0 Mobile Safari/537.36";
        assertEquals("ImaginaryKit/-10 (KHTML, like Gecko) Version/4.0 Imaginary/37.0.0.0 Mobile Safari/537.36 " + focusToken,
                WebViewProvider.getUABrowserString(chromelessImaginaryKit, focusToken));

    }

}