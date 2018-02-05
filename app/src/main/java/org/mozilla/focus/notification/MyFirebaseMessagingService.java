/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

import android.app.PendingIntent;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "MyFirebaseMsgService";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
            Log.d(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
            // TODO : handle incoming message parsing to display url and notification title/body
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                    PendingIntent.FLAG_ONE_SHOT);

            final String message = remoteMessage.getNotification().getBody();
            final NotificationCompat.Builder builder = NotificationUtil.generateNotificationBuilder(this, pendingIntent)
                    .setContentTitle(getString(R.string.survey_notification_title, "\uD83D\uDE4C"))
                    .setContentText(message)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(message));

            NotificationUtil.sendNotification(this, NotificationId.FIREBASE_AD_HOC, builder);
        }

    }
}