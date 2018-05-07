/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Provide the actual impl (FirebaseMessagingService) or the dummy one (Service) so we can use the module
 * accordingly with our build types (currently only debug and beta will provide actual impl)
 *
 * If actual impl is provided, it'll handle the push and call onRemoteMessage().
 */
abstract public class FirebaseMessagingServiceWrapper extends Service {

    public static final String PUSH_OPEN_URL = "push_open_url";

    // This is never called in this flavor since it's dummy.
    abstract public void onRemoteMessage(String url, String title, String body);

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}