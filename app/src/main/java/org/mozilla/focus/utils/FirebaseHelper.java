package org.mozilla.focus.utils;

import android.content.Context;
import android.os.Looper;
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

    private static FirebaseHelper instance;
    private HashMap<String, Object> remoteConfigDefault;
    private static boolean changing = false;
    private static Boolean pending = null;

    @Nullable
    private static BlockingEnabler.BlockingEnablerCallback enablerCallback;


    private FirebaseHelper() {
    }

    // this is only for testing, so testing code can observe the completion of enabler completes.
    // the passin callback is static, so don't pass something could be leaked
    @VisibleForTesting
    public static void injectEnablerCallback(BlockingEnabler.BlockingEnablerCallback callback) {
        enablerCallback = callback;
    }

    public static void init(final Context context) {


        if (instance == null) {
            instance = new FirebaseHelper();
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

        ThreadUtils.postToBackgroundThread(new BlockingEnabler(context, enable, enablerCallback));
        return true;
    }

    
    // this is a static class cause I want avoid leaking to context.
    public static class BlockingEnabler implements Runnable {

        public interface BlockingEnablerCallback {

            void runDelayOnExecution();

            void onComplete();
        }

        BlockingEnablerCallback blockingEnablerCallback;

        boolean enable;
        WeakReference<Context> weakContext;

        // We only need application context here.
        BlockingEnabler(Context c, boolean state, BlockingEnablerCallback callback) {
            enable = state;
            weakContext = new WeakReference<>(c.getApplicationContext());
            blockingEnablerCallback = callback;
        }

        @Override
        public void run() {
            if (weakContext == null || weakContext.get() == null) {
                return;
            }

            blockingEnablerCallback.runDelayOnExecution();

            final Context context = weakContext.get();

            // make sure we are in the changing state
            changing = true;

            // this methods is blocking.
            updateInstanceId(enable);

            enableCrashlytics(enable);
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
                if (blockingEnablerCallback != null) {
                    blockingEnablerCallback.onComplete();
                }
                // after now, there'll be now pending state.
                pending = null;
            }
        }
    }

    // this is called in FirebaseWrapper's internalInit()
    @Override
    HashMap<String, Object> getRemoteConfigDefault(Context context) {

        if (remoteConfigDefault == null) {
            // This should only happen during internalInit
            if (Looper.myLooper() != Looper.getMainLooper()) {
                // getRemoteConfigDefault can have I/O access, so must in background thread
                remoteConfigDefault = FirebaseHelperInject.getRemoteConfigDefault(context);
            }
        }
        return remoteConfigDefault;
    }
}
