package org.mozilla.focus.utils;

import android.content.Context;

import org.mozilla.focus.BuildConfig;

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

    private FirebaseHelper() {

    }

    public static void init(final Context context) {
        if (instance == null) {
            instance = new FirebaseHelper();
        }
        setDeveloperModeEnabled(BuildConfig.DEBUG);
        // internalInit() require I/O so I put it in background thread.
        ThreadUtils.postToBackgroundThread(new Runnable() {
            public void run() {
                internalInit(context, instance);
            }
        });
    }

    // this is called in FirebaseWrapper's internalInit()
    @Override
    HashMap<String, Object> getRemoteConfigDefault(Context context) {
        return FirebaseHelperInject.getRemoteConfigDefault(context);
    }
}
