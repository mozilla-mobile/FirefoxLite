package org.mozilla.focus.components;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import org.mozilla.focus.R;
import org.mozilla.focus.utils.Browsers;
import org.mozilla.rocket.component.ConfigActivity;

import java.util.concurrent.atomic.AtomicInteger;

public class ComponentStatusMonitor extends Service {


    private static final IntentFilter sIntentFilter = new IntentFilter();

    static {
        sIntentFilter.addAction(Intent.ACTION_PACKAGE_ADDED);
        sIntentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        sIntentFilter.addAction(Intent.ACTION_PACKAGE_REPLACED);
        sIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        sIntentFilter.addDataScheme("package");
    }

    private final static AtomicInteger sThreadSerial = new AtomicInteger(1);
    private BroadcastReceiver mPackageStatusReceiver;

    private final static class PackageStatusReceiver extends BroadcastReceiver {

        interface Listener {
            void onPackageChanged(Intent intent);
        }

        private final Listener mListener;

        PackageStatusReceiver(Listener listener) {
            mListener = listener;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            mListener.onPackageChanged(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mPackageStatusReceiver = new PackageStatusReceiver(new PackageStatusReceiver.Listener() {
            @Override
            public void onPackageChanged(Intent intent) {
                if (intent == null) {
                    return;
                }
                if (!sIntentFilter.hasAction(intent.getAction())) {
                    return;
                }
                Uri data = intent.getData();
                if (data == null) {
                    return;
                }
                String pkgName = data.getEncodedSchemeSpecificPart();
                //  Only handle our package
                if (getPackageName().equals(pkgName)) {
                    startService(new Intent(getApplicationContext(), ComponentStatusMonitor.class));
                }
            }
        });
        registerReceiver(mPackageStatusReceiver, sIntentFilter);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(mPackageStatusReceiver);

        //  TODO: Do not remove the Notification, replace it with contentTitle("Tap to set default browser")
        //  TODO: replaced Notification should launch Setting activity and popup system activity resolver
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
        boolean hasDefaultBrowser = Browsers.hasDefaultBrowser(getApplicationContext());
        boolean isDefaultBrowser = Browsers.isDefaultBrowser(getApplicationContext());


        PackageManager packageManager = getPackageManager();
        ComponentName componentName = new ComponentName(getApplicationContext(), ConfigActivity.class);
        int componentState = packageManager.getComponentEnabledSetting(componentName);
        switch (componentState) {
            case (PackageManager.COMPONENT_ENABLED_STATE_ENABLED):
                break;
            default:
                if (isDefaultBrowser) {
                    //  Do nothing,
                } else if (hasDefaultBrowser) {
                    //  enable the component if we has default browser and it's not me.
                    packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
                    startForeground();
                } else {
                    //  Job done, not default and no default browser
                }
                break;
        }

        if ((!isDefaultBrowser && !hasDefaultBrowser) || componentState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            stopSelf();
        }

        //  TODO: To avoid a service keep up and idle because these conditions above are not able to complete
        //  We probably need to set a timeout runnable to postDelay to stopSelf if it takes too long.
        //  Or maybe do this in onDestroy

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

    private void startForeground() {
        Log.d("mmmmmmmm", "startForeground");
        //  The notification channel id should not be necessary since this service should only be enabled for API 21~23
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "channel_id");
        Notification notification = builder
                .setContentTitle(getString(R.string.setting_default_browser_notification_title))
                .setBadgeIconType(R.drawable.ic_notification)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        startForeground(1, notification);
    }

    public static boolean isAlive(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ComponentStatusMonitor.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
