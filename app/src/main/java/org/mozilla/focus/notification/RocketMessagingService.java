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
import org.mozilla.focus.utils.IntentUtils;
import org.mozilla.rocket.deeplink.IntentScheme;

import static org.mozilla.rocket.deeplink.IntentScheme.SCHEME;

// Prov
public class RocketMessagingService extends FirebaseMessagingServiceWrapper {

    //
    @Override
    public void onRemoteMessage(String url, String title, String body) {
        if (!TelemetryWrapper.isTelemetryEnabled(this)) {
            return;
        }

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);

        // check if message needs to open url or it's a serialized intent
        if (url != null) {
            final Uri uri = Uri.parse(url);
            if (SCHEME.equals(uri.getScheme())) {
                final Intent intentScheme = IntentScheme.parse(getBaseContext(), uri);
                if (intentScheme == null) {
                    // intent scheme but can't be resolved nor a has a fallback page. We'll
                    // exit early and not even display the notification
                    return;
                } else {
                    intent = intentScheme;
                }
            } else {
                intent.setAction(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                intent.putExtra(IntentUtils.EXTRA_OPEN_NEW_TAB, true);
            }


        }
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