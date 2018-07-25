package org.mozilla.focus.components;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.SettingsActivity;
import org.mozilla.focus.notification.NotificationUtil;
import org.mozilla.focus.utils.Browsers;
import org.mozilla.rocket.component.ConfigActivity;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A service to toggle ConfigActivity on-off to clear Default browser config.
 * <p>
 * If the browser related packages list changed, it will clear default browser config. Hence all this
 * service doing is to enable then disable ConfigActivity.
 */
public class ComponentToggleService extends Service {

    public static final int NOTIFICATION_ID = 0xDEFB;

    public static final IntentFilter SERVICE_STOP_INTENT_FILTER = new IntentFilter();
    public static final String SERVICE_STOP_ACTION = "_component_service_stopped_";

    static {
        SERVICE_STOP_INTENT_FILTER.addAction(SERVICE_STOP_ACTION);
    }

    private static final int FG_NOTIFICATION_ID = 0xDEFA;
    private static final int INTENT_REQ_CODE = 0x9527;
    private static final IntentFilter sIntentFilter = new IntentFilter();

    static {
        sIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        sIntentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        sIntentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        sIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        sIntentFilter.addDataScheme("package");
    }

    private BroadcastReceiver mPackageStatusReceiver;
    private Timer timer = new Timer();

    @Override
    public void onCreate() {
        super.onCreate();
        mPackageStatusReceiver = new PackageStatusReceiver(new PackageStatusReceiver.Listener() {
            @Override
            public void onPackageChanged(@NonNull Intent intent) {
                if (!sIntentFilter.hasAction(intent.getAction())) {
                    return;
                }

                final String pkgName = intent.getData().getEncodedSchemeSpecificPart();
                //  Only handle our package
                if (getPackageName().equals(pkgName)) {
                    // package changed, to notify service the job is finished
                    startService(new Intent(getApplicationContext(), ComponentToggleService.class));
                }
            }
        });

        timer.schedule(new BombTask(this), BombTask.TIMEOUT);
        registerReceiver(mPackageStatusReceiver, sIntentFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mPackageStatusReceiver);

        // this should not happen, just in case
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        LocalBroadcastManager.getInstance(getApplicationContext())
                .sendBroadcast(new Intent(SERVICE_STOP_ACTION));

        stopForeground(true);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        final boolean hasDefaultBrowser = Browsers.hasDefaultBrowser(getApplicationContext());
        final boolean isDefaultBrowser = Browsers.isDefaultBrowser(getApplicationContext());
        final boolean configCleared = (!isDefaultBrowser && !hasDefaultBrowser);

        final PackageManager pkgMgr = getPackageManager();
        final ComponentName componentName = new ComponentName(getApplicationContext(), ConfigActivity.class);
        final boolean componentEnabled = (pkgMgr.getComponentEnabledSetting(componentName)
                == PackageManager.COMPONENT_ENABLED_STATE_ENABLED);

        // if there is default-browser-setting and it is not me, let's enable component to clear settings
        final boolean toStartJob = (!componentEnabled) && hasDefaultBrowser && !isDefaultBrowser;

        // job of this service is completed, let's stop service
        final boolean jobFinished = configCleared || componentEnabled;

        if (toStartJob) {
            pkgMgr.setComponentEnabledSetting(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP);
            startToForeground();
        } else if (jobFinished) {
            pkgMgr.setComponentEnabledSetting(componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP);
            removeFromForeground();
        }

        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    /**
     * A helper function to report whether this service is alive
     *
     * @param context
     * @return true if this service is alive
     */
    public static boolean isAlive(Context context) {
        final ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ComponentToggleService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }

        return false;
    }

    private void startToForeground() {
        final NotificationCompat.Builder builder =
                NotificationUtil.importantBuilder(getApplicationContext());

        final Notification notification = builder
                .setContentTitle(getString(R.string.setting_default_browser_notification_title))
                .setContentText(getString(R.string.setting_default_browser_notification_text))
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(buildIntent())
                .build();

        startForeground(FG_NOTIFICATION_ID, notification);
    }

    private void removeFromForeground() {
        // to post a new notification so people can go to SettingsActivity easily
        // this notification will be removed by SettingsActivity if it is in foreground
        final NotificationCompat.Builder builder =
                NotificationUtil.importantBuilder(getApplicationContext());

        final Notification notification = builder
                .setContentTitle(getString(R.string.setting_default_browser_notification_clickable_text))
                .setAutoCancel(true)
                .setContentIntent(buildIntent())
                .build();

        NotificationManagerCompat.from(getApplicationContext())
                .notify(NOTIFICATION_ID, notification);

        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        stopSelf();
    }

    private PendingIntent buildIntent() {
        return PendingIntent.getActivity(getApplicationContext(),
                INTENT_REQ_CODE,
                new Intent(getApplicationContext(), SettingsActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    // A TimerTask to stop service if it runs too long
    private final static class BombTask extends TimerTask {
        // How long this Service will stop itself
        private static final long TIMEOUT = 30000; // 30 seconds

        final WeakReference<ComponentToggleService> service;

        BombTask(ComponentToggleService srv) {
            this.service = new WeakReference<>(srv);
        }

        @Override
        public void run() {
            final ComponentToggleService srv = this.service.get();
            if (srv != null) {
                srv.removeFromForeground();
            }
        }
    }

    private final static class PackageStatusReceiver extends BroadcastReceiver {

        interface Listener {
            void onPackageChanged(@NonNull Intent intent);
        }

        private final Listener mListener;

        PackageStatusReceiver(Listener listener) {
            mListener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null && intent.getData() != null) {
                mListener.onPackageChanged(intent);
            }
        }
    }
}
