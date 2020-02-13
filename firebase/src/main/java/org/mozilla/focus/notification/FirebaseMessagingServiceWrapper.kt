/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.notification

import android.content.Intent
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
    abstract fun onRemoteMessage(intent: Intent, title: String?, body: String?)

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        // This happens when the app is running in foreground, and the user clicks on the push
        // notification with payload "PUSH_OPEN_URL"
        if (remoteMessage.notification != null) {
            val intent = Intent()
            // check if message contains data payload
            intent.putExtra(MESSAGE_ID, remoteMessage.data[MESSAGE_ID])
            intent.putExtra(PUSH_OPEN_URL, remoteMessage.data[PUSH_OPEN_URL])
            intent.putExtra(PUSH_COMMAND, remoteMessage.data[PUSH_COMMAND])
            intent.putExtra(PUSH_DEEP_LINK, remoteMessage.data[PUSH_DEEP_LINK])
            val title = remoteMessage.notification?.title
            val body = remoteMessage.notification?.body
            // We have a remote message from gcm, let the child decides what to do with it.
            onRemoteMessage(intent, title, body)
        }
    }

    companion object {
        const val MESSAGE_ID = "message_id"
        const val PUSH_OPEN_URL = "push_open_url"
        const val PUSH_COMMAND = "push_command"
        const val PUSH_DEEP_LINK = "push_deep_link"
    }
}