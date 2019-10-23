/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.utils.IntentUtils;

// Prov
public class RocketMessagingService extends FirebaseMessagingServiceWrapper {

    private static final int REQUEST_CODE_CLICK_NOTIFICATION = 1;
    private static final int REQUEST_CODE_DELETE_NOTIFICATION = 2;

    //
    @Override
    public void onRemoteMessage(Intent intent, String title, String body) {
        String messageId = parseMessageId(intent);
        String link = parseLink(intent);
        TelemetryWrapper.getNotification(link, messageId);
        if (!TelemetryWrapper.isTelemetryEnabled(this)) {
            return;
        }

        PendingIntent pendingIntent = getClickPendingIntent(
            getApplicationContext(),
            messageId,
            parseOpenUrl(intent),
            parseCommand(intent),
            parseDeepLink(intent),
            parseLink(intent)
        );
        final NotificationCompat.Builder builder = NotificationUtil.importantBuilder(this)
                .setContentIntent(pendingIntent);

        if (title != null) {
            builder.setContentTitle(title);
        }

        if (body != null) {
            builder.setContentText(body);
        }

        addDeleteTelemetry(getApplicationContext(), builder, messageId, link);

        NotificationUtil.sendNotification(this, NotificationId.FIREBASE_AD_HOC, builder);
        TelemetryWrapper.showNotification(link, messageId);
    }

    private String parseMessageId(Intent intent) {
        return intent.getStringExtra(MESSAGE_ID);
    }

    private String parseOpenUrl(Intent intent) {
        return intent.getStringExtra(PUSH_OPEN_URL);
    }

    private String parseCommand(Intent intent) {
        return intent.getStringExtra(PUSH_COMMAND);
    }

    private String parseDeepLink(Intent intent) {
        return intent.getStringExtra(PUSH_DEEP_LINK);
    }

    private String parseLink(Intent intent) {
        String link = intent.getStringExtra(PUSH_OPEN_URL);
        if (link == null) {
            link = intent.getStringExtra(PUSH_COMMAND);
        }
        if (link == null) {
            link = intent.getStringExtra(PUSH_DEEP_LINK);
        }

        return link;
    }

    private PendingIntent getClickPendingIntent(Context appContext, String messageId, String openUrl, String command, String deepLink, String link) {
        // RocketLauncherActivity will handle this intent
        Intent clickIntent = IntentUtils.genFirebaseNotificationClickForBroadcastReceiver(
                appContext,
                messageId,
                openUrl,
                command,
                deepLink,
                link
        );
        return PendingIntent.getBroadcast(this, REQUEST_CODE_CLICK_NOTIFICATION, clickIntent, PendingIntent.FLAG_ONE_SHOT);
    }

    private void addDeleteTelemetry(Context appContext, NotificationCompat.Builder builder, String messageId, String link) {
        Intent intent = IntentUtils.genDeleteFirebaseNotificationActionForBroadcastReceiver(appContext, messageId, link);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, REQUEST_CODE_DELETE_NOTIFICATION, intent, PendingIntent.FLAG_ONE_SHOT);
        builder.setDeleteIntent(pendingIntent);
    }
}