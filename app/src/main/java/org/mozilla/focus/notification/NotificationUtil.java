/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.annotation.CheckResult;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;

import org.mozilla.focus.R;

public class NotificationUtil {

    public enum Channel {
        IMPORTANT,
        PRIVATE
    }

    private static final String DEFAULT_CHANNEL_ID = "default_channel_id";
    private static final String PRIVATE_MODE_CHANNEL_ID = "private_mode_channel_id";

    /**
     * To ensure we can generate a Notification Builder with same style
     *
     * @param context
     * @return
     */
    public static NotificationCompat.Builder baseBuilder(Context context, Channel channel) {

        final String channelId = getChannelD(channel);
        if (TextUtils.isEmpty(channelId)) {
            throw new IllegalStateException("No such channel");
        }

        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher))
                .setColor(ContextCompat.getColor(context, R.color.surveyNotificationAccent))
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            builder.setShowWhen(false);
        }

        return builder;
    }

    // DEFAULT_VIBRATE makes notifications can show heads-up for Android 7 and below
    public static NotificationCompat.Builder importantBuilder(Context context) {

        final NotificationCompat.Builder builder = baseBuilder(context, Channel.IMPORTANT)
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
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }

        createNotificationChannel(context, DEFAULT_CHANNEL_ID,
                R.string.app_name,
                NotificationManager.IMPORTANCE_HIGH);

        createNotificationChannel(context, PRIVATE_MODE_CHANNEL_ID,
                R.string.private_browsing_title,
                NotificationManager.IMPORTANCE_LOW);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private static void createNotificationChannel(Context context, String channelId, int channelNameRes, int importance) {
        final NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager == null) {
            return;
        }

        final NotificationChannel channel = new NotificationChannel(channelId, context.getString(channelNameRes), importance);
        notificationManager.createNotificationChannel(channel);
    }

    @CheckResult
    private static String getChannelD(Channel channel) {
        switch (channel) {
            case IMPORTANT:
                return DEFAULT_CHANNEL_ID;
            case PRIVATE:
                return PRIVATE_MODE_CHANNEL_ID;
        }
        return "";
    }
}