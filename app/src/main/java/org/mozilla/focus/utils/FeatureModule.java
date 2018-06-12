package org.mozilla.focus.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.Toast;

import com.google.android.play.core.splitinstall.SplitInstallManager;
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory;
import com.google.android.play.core.splitinstall.SplitInstallRequest;
import com.google.android.play.core.splitinstall.SplitInstallSessionState;
import com.google.android.play.core.splitinstall.SplitInstallStateUpdatedListener;
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus;
import com.google.android.play.core.tasks.OnFailureListener;
import com.google.android.play.core.tasks.OnSuccessListener;

import org.mozilla.focus.tabs.TabView;
import org.mozilla.focus.tabs.TabViewProvider;
import org.mozilla.focus.web.WebViewProvider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class FeatureModule {
    private final static String TAG = "Rocket";
    private final static String DYNAMIC_FEATURE_GECKO_POWER = "geckopower";

    private static String appPkgName = "org.mozilla.rocket";

    private static FeatureModule sInstance;
    private boolean supportPrivateBrowsing = true;

    private SplitInstallManager mgr = null;

    public static String getFeatureModuleApkPath(Context ctx) {
        return ctx.getPackageResourcePath().replace("base.apk", "split_geckopower.apk");
    }

    public synchronized static FeatureModule getInstance() {
        if (sInstance == null) {
            sInstance = new FeatureModule();
        }

        return sInstance;
    }

    private FeatureModule() {
    }

    public void refresh(@NonNull final Context context) {
        if (mgr == null) {
            mgr = SplitInstallManagerFactory.create(context);
        }

        final Set<String> set = mgr.getInstalledModules();

        appPkgName = context.getPackageName();

        Log.d(TAG, "Feature-module - Installed modules size: " + set.size());
        for (final String s : set) {
            Log.d(TAG, "Feature-module - Installed module: " + s);
        }

        supportPrivateBrowsing = set.contains(DYNAMIC_FEATURE_GECKO_POWER);
    }

    public boolean isSupportPrivateBrowsing() {
        return supportPrivateBrowsing;
    }

    public static Intent intentForPrivateBrowsing(final String url) {
        // IT IS TRICK
        // if you extract apks file and see AndroidManifest.xml of base-apk, you will see
        // PrivateActivity is under packge "org.mozilla.rocket"
        final String pkgName = appPkgName;
        Intent intent = new Intent();
        intent.putExtra("extra_url", url);
        intent.setClassName(pkgName, "org.mozilla.rocket.privatebrowsing.PrivateActivity");
        return intent;
    }

    public void install(final Context context, final StatusListener callback) {
        final SplitInstallManager mgr = SplitInstallManagerFactory.create(context);
        if (isSupportPrivateBrowsing()) {
            callback.onDone();
            return;
        }

        final SplitInstallRequest req = SplitInstallRequest
                .newBuilder()
                .addModule(DYNAMIC_FEATURE_GECKO_POWER)
                .build();

        final List<String> modules = new ArrayList<>();
        modules.add(DYNAMIC_FEATURE_GECKO_POWER);
        mgr.startInstall(req)
                .addOnSuccessListener(new OnSuccessListener<Integer>() {
                    @Override
                    public void onSuccess(Integer integer) {
                        refresh(context);
                        Log.d(TAG, "installing");
                        final FeatureStateListener listener = new FeatureStateListener(
                                context,
                                mgr,
                                callback,
                                integer);
                        mgr.registerListener(listener);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(context, "Try install but failed", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                        Log.e(TAG, e.getMessage());
                        refresh(context);
                        callback.onDone();
                    }
                });
    }

    public void uninstall(final Context context, final StatusListener callback) {
        if (!isSupportPrivateBrowsing()) {
            Toast.makeText(context, "Not supporting private browsing", Toast.LENGTH_SHORT).show();
            callback.onDone();
            return;
        }

        final SplitInstallManager mgr = SplitInstallManagerFactory.create(context);
        final List<String> modules = new ArrayList<>();
        modules.add(DYNAMIC_FEATURE_GECKO_POWER);
        mgr.deferredUninstall(modules)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(context, "Uninstalling module", Toast.LENGTH_SHORT).show();
                        refresh(context);
                        callback.onDone();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception e) {
                        Toast.makeText(context, "Try uninstall but failed", Toast.LENGTH_SHORT).show();
                        refresh(context);
                        callback.onDone();
                    }
                });
    }

    public void preload(final Activity activity) {
        if (isSupportPrivateBrowsing()) {
            try {
                final Class c = Class.forName("org.mozilla.tabs.gecko.GeckoViewProvider");
                final Method method = c.getMethod("preload", Context.class);
                method.invoke(null, activity.getApplicationContext());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        WebViewProvider.preload(activity);
    }

    public TabViewProvider createTabVieProvider(final Activity activity) {
        if (isSupportPrivateBrowsing()) {
            try {
                final Class c = Class.forName("org.mozilla.tabs.gecko.GeckoViewProvider");
                final Constructor<?> cs = c.getConstructor(Activity.class);
                return (TabViewProvider) cs.newInstance(activity);
            } catch (Exception e) {
                e.printStackTrace();
                return new WebkitTabViewProvider(activity);
            }
        } else {
            return new WebkitTabViewProvider(activity);
        }
    }

    public interface StatusListener {
        void onDone();

        void onProgress(String msg);
    }

    // a TabViewProvider and it should only be used in this activity
    private static class WebkitTabViewProvider implements TabViewProvider {
        private Activity activity;

        WebkitTabViewProvider(@NonNull final Activity activity) {
            this.activity = activity;
        }

        @Override
        public TabView create() {
            return WebViewProvider.create(this.activity, null);
        }

        @Override
        public int getEngineType() {
            return TabViewProvider.ENGINE_WEBKIT;
        }
    }

    private class FeatureStateListener implements SplitInstallStateUpdatedListener {

        private Context ctx;
        private SplitInstallManager manager;
        private int sessionId = 0;
        private StatusListener callback;

        FeatureStateListener(Context ctx,
                             SplitInstallManager mgr,
                             StatusListener listener,
                             int id) {
            this.ctx = ctx;
            this.callback = listener;
            this.manager = mgr;
            this.sessionId = id;
        }

        @Override
        public void onStateUpdate(SplitInstallSessionState state) {
            if (state.sessionId() != this.sessionId) {
                Log.d(TAG, String.format("my session(%d), not %d", this.sessionId, state.sessionId()));
                return;
            }

            if (state.status() == SplitInstallSessionStatus.FAILED) {
                Toast.makeText(ctx, "fail state: " + state, Toast.LENGTH_SHORT).show();
                Log.e(TAG, "failed: " + state);
                destroySelf();
                return;
            }

            switch (state.status()) {
                case SplitInstallSessionStatus.DOWNLOADING:
                    long totalBytes = state.totalBytesToDownload();
                    long progress = state.bytesDownloaded();
                    String msg = String.format("downloading: %d/%d", progress, totalBytes);
                    callback.onProgress(msg);
                    break;
                case SplitInstallSessionStatus.REQUIRES_USER_CONFIRMATION:
                    try {
                        ctx.startIntentSender(
                                state.resolutionIntent().getIntentSender(),
                                null, 0, 0, 0
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "Exception on user_confirmation, " + e);
                        destroySelf();
                    }

                    break;
                case SplitInstallSessionStatus.DOWNLOADED:
                    Log.d(TAG, "module downloaded");
                    destroySelf();
                    break;
                case SplitInstallSessionStatus.INSTALLING:
                    callback.onProgress("Installing");
                    Log.d(TAG, "on state installing");
                    break;
                case SplitInstallSessionStatus.INSTALLED:
                    callback.onProgress("Installed");
                    Log.d(TAG, "on state installed");
                    destroySelf();
                    break;
                case SplitInstallSessionStatus.FAILED:
                case SplitInstallSessionStatus.UNKNOWN:
                case SplitInstallSessionStatus.CANCELED:
                    destroySelf();
                default:
                    Log.d(TAG, "on state update: " + state);
            }
        }

        private void destroySelf() {
            FeatureModule.this.refresh(ctx);
            if (callback != null) {
                callback.onDone();
            }
            manager.unregisterListener(this);
        }
    }
}
