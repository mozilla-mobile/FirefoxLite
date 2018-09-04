package org.mozilla.rocket.util;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.ServiceCompat;

import org.mozilla.focus.R;
import org.mozilla.focus.notification.NotificationId;

public abstract class ForeGroundIntentService extends IntentService {

    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public ForeGroundIntentService(String name) {
        super(name);
    }

    protected void startForeground() {
        final String notificationChannelId;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            configForegroundChannel(this);
            notificationChannelId = getNotificationId();
        } else {
            notificationChannelId = "not_used_notification_id";
        }
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), notificationChannelId);

        Notification notification = builder
                .build();
        startForeground(NotificationId.RELOCATE_SERVICE, notification);
    }

    // Configure the notification channel if needed
    private void configForegroundChannel(Context context) {
        final NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        // NotificationChannel API is only available for Android O and above, so we need to add the check here so IDE won't complain
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            final String channelName = context.getString(R.string.app_name);
            final NotificationChannel notificationChannel = new NotificationChannel(getNotificationId(), channelName, NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(notificationChannel);
        }
    }

    protected void stopForeground() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
    }

    protected abstract String getNotificationId();

}
