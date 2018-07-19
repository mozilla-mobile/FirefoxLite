/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

public class NotificationId {
    public static final int SURVEY_ON_3RD_LAUNCH = 1000;
    public static final int LOVE_FIREFOX = 1001;            // in app promotion: Love Firefox
    public static final int DEFAULT_BROWSER = 1002;         // in app promotion: Set Default Browser
    public static final int PRIVACY_POLICY_UPDATE = 1003;   // in app notification
    public static final int RELOCATE_SERVICE = 2000;        // For file download
    public static final int FIREBASE_AD_HOC = 3000;         // For push notification
    public static final int PRIVATE_MODE = 4000;            // Keeps PrivateModeActivity alive
}
