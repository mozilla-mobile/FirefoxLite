/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.telemetry.TelemetryWrapper;

// Prov
public class RocketMessagingService extends FirebaseMessagingServiceWrapper {

    //
    @Override
    public void onRemoteMessage(String url, String title, String body) {
        if (!TelemetryWrapper.isTelemetryEnabled(this)) {
            return;
        }

        final Intent intent = new Intent(getApplicationContext(), MainActivity.class);

        // check if message needs to open url
        if (url != null) {
            intent.setAction(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
        }
        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        final NotificationCompat.Builder builder = NotificationUtil.generateNotificationBuilder(this, pendingIntent);

        if (title != null) {
            builder.setContentTitle(title);
        }

        if (body != null) {
            builder.setContentText(body);
        }

        NotificationUtil.sendNotification(this, NotificationId.FIREBASE_AD_HOC, builder);
    }
}