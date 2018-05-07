package org.mozilla.focus.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;

import org.mozilla.focus.notification.RocketMessagingService;
import org.mozilla.focus.telemetry.TelemetryWrapper;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Implementation for FirebaseWrapper. It's job:
 * 1. Call init() to start the wrapper in a background thread
 * 2. Implement getRemoteConfigDefault to provide Remote Config default value
 */
final public class FirebaseHelper extends FirebaseWrapper {

    // keys for remote config default value
    static final String RATE_APP_DIALOG_TEXT_TITLE = "rate_app_dialog_text_title";
    static final String RATE_APP_DIALOG_TEXT_CONTENT = "rate_app_dialog_text_content";

    private HashMap<String, Object> remoteConfigDefault;
    private static boolean changing = false;
    private static Boolean pending = null;

    @Nullable
    private static BlockingEnablerCallback enablerCallback;

    private FirebaseHelper() {
    }

    // inject delay to BlockingEnabler
    @VisibleForTesting
    public static void injectEnablerCallback(BlockingEnablerCallback callback) {
        enablerCallback = callback;
    }

    public static void init(final Context context) {

        if (initNeeded()) {
            initInternal(new FirebaseHelper());
        }

        initCrashlytics();

        bind(context);
    }


    public static boolean bind(@NonNull final Context context) {

        final Context safeContext = context.getApplicationContext();
        final boolean enable = TelemetryWrapper.isTelemetryEnabled(safeContext);

        return enableFirebase(safeContext, enable);


    }

    // return true if a new runnable is created. otherwise return false.
    // I need this return value for testing (as the return value of bind() method)
    private static boolean enableFirebase(final Context context, final boolean enable) {

        // if the task is already running, we cache the value and skip creating new runnable
        if (changing) {
            pending = enable;
            return false;
        }
        // Now it's time to change the state of firebase helper.
        changing = true;
        // starting from now, there's no pending state. (pending state will only be used in the runnable)
        pending = null;

        final BlockingEnabler blockingEnabler = new BlockingEnabler(context, enable);
        blockingEnabler.execute();
        return true;
    }

    // an interface for testing code to inject delay to BlockingEnabler
    public interface BlockingEnablerCallback {
        void runDelayOnExecution();
    }

    // AsyncTask is useful cause we don't need to write a specific idling resource for it.
    public static class BlockingEnabler extends AsyncTask<Void, Void, Void> {
        boolean enable;
        WeakReference<Context> weakContext;

        // We only need application context here.
        BlockingEnabler(Context c, boolean state) {
            enable = state;
            weakContext = new WeakReference<>(c.getApplicationContext());

        }


        @Override
        protected Void doInBackground(Void... voids) {
            // make StrictMode quiet here, cause Crashlytics has StrictMode.onUntaggedSocket violation
            // and some I/O access below will also conduct StrictModeDiskReadViolation. I'll set it back after all works are done
            final StrictMode.ThreadPolicy cachedThreadPolicy = StrictMode.getThreadPolicy();
            final StrictMode.VmPolicy cacheVmPolicy = StrictMode.getVmPolicy();
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());

            if (weakContext == null || weakContext.get() == null) {
                return null;
            }

            // this is only for testing. So we can simulate slow network..etc
            final BlockingEnablerCallback callback = FirebaseHelper.enablerCallback;
            if (callback != null) {
                callback.runDelayOnExecution();
            }

            final Context context = weakContext.get();

            // make sure we are in the changing state
            changing = true;

            // this methods is blocking.
            updateInstanceId(enable);

            enableCrashlytics(context, enable);
            enableAnalytics(context, enable);
            enableCloudMessaging(context, RocketMessagingService.class.getName(), enable);
            enableRemoteConfig(context, enable);

            // now firebase has completed state changing,
            changing = false;
            // we'll check if the cached state is the same as our current one. If not, issue
            // a state change again.
            if (pending != null && pending != enable) {
                enableFirebase(context, pending);
            } else {
                // after now, there'll be now pending state.
                pending = null;
            }

            StrictMode.setThreadPolicy(cachedThreadPolicy);
            StrictMode.setVmPolicy(cacheVmPolicy);
            return null;
        }

    }

    // this is called in FirebaseWrapper's internalInit()
    @Override
    HashMap<String, Object> getRemoteConfigDefault(Context context) {

        if (remoteConfigDefault == null) {
            // getRemoteConfigDefault can have I/O access, so must in background thread
            remoteConfigDefault = FirebaseHelperInject.getRemoteConfigDefault(context);
        }
        return remoteConfigDefault;
    }
}
