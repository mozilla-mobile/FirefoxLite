/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import org.mozilla.focus.R;

public class NotificationUtil {

    private static final String DEFAULT_CHANNEL_ID = "default_channel_id";

    /**
     * To ensure we can generate a Notification Builder with same style
     *
     * @param context
     * @return
     */
    public static NotificationCompat.Builder baseBuilder(Context context, PendingIntent pendingIntent) {
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setColor(ContextCompat.getColor(context, R.color.surveyNotificationAccent))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setShowWhen(false);
        }

        return builder;
    }

    // DEFAULT_VIBRATE makes notifications can show heads-up for Android 7 and below
    public static NotificationCompat.Builder importantBuilder(Context context, PendingIntent pendingIntent) {

        final NotificationCompat.Builder builder = baseBuilder(context, pendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_VIBRATE)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        return builder;
    }


    public static void sendNotification(Context context, int id, NotificationCompat.Builder builder) {
        final NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            notificationManager.notify(id, builder.build());
        }

    }

    // Configure the default notification channel if needed
    // See: https://developer.android.com/training/notify-user/channels#CreateChannel
    public static void init(Context context) {

        final NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            // NotificationChannel API is only available for Android O and above, so we need to add the check here so IDE won't complain
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                final String channelName = context.getString(R.string.app_name);
                // IMPORTANCE_HIGH makes notifications can show heads-up for Android 8 and above
                final NotificationChannel notificationChannel = new NotificationChannel(DEFAULT_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_HIGH);
                notificationChannel.enableLights(true);
                notificationChannel.setLightColor(Color.RED);
                notificationChannel.setImportance(NotificationManager.IMPORTANCE_HIGH);

                notificationManager.createNotificationChannel(notificationChannel);
            }
        }
    }
}