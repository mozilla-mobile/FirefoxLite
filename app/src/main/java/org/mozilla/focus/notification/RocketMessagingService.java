/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;

import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.rocket.component.RocketLauncherActivity;

// Prov
public class RocketMessagingService extends FirebaseMessagingServiceWrapper {

    //
    @Override
    public void onRemoteMessage(Intent intent, String title, String body) {
        if (!TelemetryWrapper.isTelemetryEnabled(this)) {
            return;
        }
        /**
         * Target RocketLauncherActivity with ACTION_VIEW and CATEGORY_BROWSABLE
         * This is because we want to handle App link in {@link  org.mozilla.rocket.component.LaunchIntentDispatcher}
         */
        intent.setAction(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setClass(getApplicationContext(), RocketLauncherActivity.class);

        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT);

        final NotificationCompat.Builder builder = NotificationUtil.importantBuilder(this)
                .setContentIntent(pendingIntent);

        if (title != null) {
            builder.setContentTitle(title);
        }

        if (body != null) {
            builder.setContentText(body);
        }

        NotificationUtil.sendNotification(this, NotificationId.FIREBASE_AD_HOC, builder);
    }
}