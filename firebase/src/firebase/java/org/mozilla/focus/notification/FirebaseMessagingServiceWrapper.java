/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

import android.content.Intent;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;


/**
 * Provide the actual impl (FirebaseMessagingService) or the dummy one (Service) so we can use the module
 * accordingly with our build types (currently only debug and beta will provide actual impl)
 * <p>
 * If actual impl is provided, it'll handle the push and call onRemoteMessage().
 */
abstract public class FirebaseMessagingServiceWrapper extends FirebaseMessagingService {

    public static final String PUSH_OPEN_URL = "push_open_url";
    public static final String PUSH_COMMAND = "push_command";

    abstract public void onRemoteMessage(Intent intent, String title, String body);

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        // This happens when the app is running in foreground, and the user clicks on the push
        // notification with payload "PUSH_OPEN_URL"
        if (remoteMessage.getNotification() != null) {

            final Intent intent = new Intent();
            // check if message contains data payload
            if (remoteMessage.getData() != null) {
                intent.putExtra(PUSH_OPEN_URL, remoteMessage.getData().get(PUSH_OPEN_URL));
                intent.putExtra(PUSH_COMMAND, remoteMessage.getData().get(PUSH_COMMAND));
            }
            final String title = remoteMessage.getNotification().getTitle();
            final String body = remoteMessage.getNotification().getBody();

            // We have a remote message from gcm, let the child decides what to do with it.
            onRemoteMessage(intent, title, body);
        }

    }


}