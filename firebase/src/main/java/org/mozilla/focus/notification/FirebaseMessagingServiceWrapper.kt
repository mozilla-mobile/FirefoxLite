/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.notification

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/**
 * Provide the actual impl (FirebaseMessagingService) or the dummy one (Service) so we can use the module
 * accordingly with our build types (currently only debug and beta will provide actual impl)
 *
 *
 * If actual impl is provided, it'll handle the push and call onRemoteMessage().
 */
abstract class FirebaseMessagingServiceWrapper : FirebaseMessagingService() {
    abstract fun onNotificationMessage(data: Map<String, String>, title: String?, body: String?, imageUrl: String?)

    abstract fun onDataMessage(data: MutableMap<String, String>)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // This happens when the app is running in foreground, and the user clicks on the push
        // notification with payload "PUSH_OPEN_URL"
        if (remoteMessage.notification != null) {

            val title = remoteMessage.notification?.title
            val body = remoteMessage.notification?.body
            val imageUrl = remoteMessage.notification?.imageUrl?.toString()
            // We have a remote message from gcm, let the child decides what to do with it.
            onNotificationMessage(remoteMessage.data, title, body, imageUrl)
        } else if (remoteMessage.data.isNotEmpty()) {

            onDataMessage(remoteMessage.data)
        }
    }
}