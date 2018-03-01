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
import android.support.v4.os.BuildCompat;

import org.mozilla.focus.R;

public class NotificationUtil {

    public static final String PUSH_OPEN_URL = "push_open_url";

    private static final String DEFAULT_CHANNEL_ID = "rocket_news";


    public static NotificationCompat.Builder generateNotificationBuilder(Context context, PendingIntent pendingIntent) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, DEFAULT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setColor(ContextCompat.getColor(context, R.color.surveyNotificationAccent))
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(new long[0]);


        if (BuildCompat.isAtLeastN()) {
            builder.setShowWhen(false);
        }
        return builder;

    }

    public static void sendNotification(Context context, int id, NotificationCompat.Builder builder) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        configNotificationChannel(context, notificationManager);

        if (notificationManager != null) {
            notificationManager.notify(id, builder.build());
        }
    }

    // Configure the notification channel if needed
    private static void configNotificationChannel(Context context, NotificationManager notificationManager) {
        // Channel is required for O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            final String channelName = context.getString(R.string.notification_channel_name);
            final NotificationChannel notificationChannel = new NotificationChannel(DEFAULT_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_DEFAULT);
            final String description = context.getString(R.string.notification_channel_description);
            notificationChannel.setDescription(description);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[] { 0, 1000, 500, 1000 });
            notificationChannel.enableVibration(true);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }
}
