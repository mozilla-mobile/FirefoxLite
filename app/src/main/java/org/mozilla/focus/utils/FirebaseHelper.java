package org.mozilla.focus.utils;

import android.content.Context;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;

import org.mozilla.focus.notification.RocketMessagingService;
import org.mozilla.focus.telemetry.TelemetryWrapper;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.logging.Handler;

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

    private FirebaseHelper() {
    }

    public static void init(final Context context) {


        if (instance == null) {
            instance = new FirebaseHelper();
        }

        initCrashlytics();

        bind(context);
    }


    public static void bind(@NonNull final Context context) {

        final boolean enable = TelemetryWrapper.isTelemetryEnabled(context);

        enableFirebase(context, enable);


    }

    private static void enableFirebase(final Context context, final boolean enable) {

        // if the task is already running, we cache the value and skip creating new runnable
        if (changing) {
            pending = enable;
            return;
        }
        // Now it's time to change the state of firebase helper.
        changing = true;
        // starting from now, there's no pending state. (pending state will only be used in the runnable)
        pending = null;

        ThreadUtils.postToBackgroundThread(new EnableHandler(context, enable));
    }

    private static class EnableHandler implements Runnable {

        boolean enable;
        WeakReference<Context> weakContext;

        EnableHandler(Context c, boolean state) {
            enable = state;
            weakContext = new WeakReference<>(c);
        }

        @Override
        public void run() {
            if (weakContext == null || weakContext.get() == null) {
                return;
            }

            final Context context = weakContext.get();

            // make sure we are in the changing state
            changing = true;

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
            }
            // after now, there'll be now pending state.
            pending = null;
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
