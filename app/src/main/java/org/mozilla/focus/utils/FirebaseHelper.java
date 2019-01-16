/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Looper;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import org.mozilla.fileutils.FileUtils;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.home.FeatureSurveyViewHelper;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.notification.RocketMessagingService;
import org.mozilla.focus.screenshot.ScreenshotManager;
import org.mozilla.focus.telemetry.TelemetryWrapper;

import java.lang.ref.WeakReference;
import java.util.HashMap;

/**
 * Implementation for FirebaseWrapper. It's job:
 * 1. Call init() to start the wrapper in a background thread
 * 2. Implement getRemoteConfigDefault to provide Remote Config default value
 */
final public class FirebaseHelper extends FirebaseWrapper {

    private static final String TAG = "FirebaseHelper";

    // keys for remote config default value
    public static final String RATE_APP_DIALOG_TEXT_TITLE = "rate_app_dialog_text_title";
    public static final String RATE_APP_DIALOG_TEXT_CONTENT = "rate_app_dialog_text_content";
    public static final String RATE_APP_DIALOG_TEXT_POSITIVE = "rate_app_dialog_text_positive";
    public static final String RATE_APP_DIALOG_TEXT_NEGATIVE = "rate_app_dialog_text_negative";

    // Key for local broadcast
    public static final String FIREBASE_READY = "Firebase_ready";

    static final String RATE_APP_DIALOG_THRESHOLD = "rate_app_dialog_threshold";
    static final String RATE_APP_NOTIFICATION_THRESHOLD = "rate_app_notification_threshold";
    static final String SHARE_APP_DIALOG_THRESHOLD = "share_app_dialog_threshold";
    static final String ENABLE_MY_SHOT_UNREAD = "enable_my_shot_unread";
    static final String ENABLE_PRIVATE_MODE = "enable_private_mode";
    static final String BANNER_MANIFEST = "banner_manifest";
    static final String SCREENSHOT_CATEGORY_MANIFEST = "screenshot_category_manifest";
    static final String FEATURE_SURVEY = "feature_survey";
    static final String VPN_RECOMMENDER_URL = "vpn_recommender_url";
    static final String VPN_RECOMMENDER_PACKAGE = "vpn_recommender_package";

    private static final String FIREBASE_WEB_ID = "default_web_client_id";
    private static final String FIREBASE_DB_URL = "firebase_database_url";
    private static final String FIREBASE_CRASH_REPORT = "google_crash_reporting_api_key";
    private static final String FIREBASE_APP_ID = "google_app_id";
    private static final String FIREBASE_API_KEY = "google_api_key";
    private static final String FIREBASE_PROJECT_ID = "project_id";


    private HashMap<String, Object> remoteConfigDefault;
    private static boolean changing = false;
    private static Boolean pending = null;

    @Nullable
    private static BlockingEnablerCallback enablerCallback;

    // the file name to used when you want to set the default value of RemoteConfig
    private static final String REMOTE_CONFIG_JSON = "remote_config.json";

    private FirebaseHelper() {
    }

    // inject delay to BlockingEnabler
    @VisibleForTesting
    public static void injectEnablerCallback(BlockingEnablerCallback callback) {
        enablerCallback = callback;
    }

    public static void init(final Context context, boolean enabled) {

        if (getInstance() == null) {
            initInternal(new FirebaseHelper());
        }
        bind(context, enabled);
    }


    public static boolean bind(@NonNull final Context context, boolean enabled) {
        checkIfApiReady(context);
        return enableFirebase(context.getApplicationContext(), enabled);
    }

    private static void checkIfApiReady(Context context) {
        if (AppConstants.isBuiltWithFirebase()) {
            final String webId = getStringResourceByName(context, FIREBASE_WEB_ID);
            final String dbUrl = getStringResourceByName(context, FIREBASE_DB_URL);
            final String crashReport = getStringResourceByName(context, FIREBASE_CRASH_REPORT);
            final String appId = getStringResourceByName(context, FIREBASE_APP_ID);
            final String apiKey = getStringResourceByName(context, FIREBASE_API_KEY);
            final String projectId = getStringResourceByName(context, FIREBASE_PROJECT_ID);
            if (webId.isEmpty() || dbUrl.isEmpty() || crashReport.isEmpty() ||
                    appId.isEmpty() || apiKey.isEmpty() || projectId.isEmpty()) {
                throw new IllegalStateException("Firebase related keys are not set");
            }
        }
    }

    /**
     * @param context Should be application context. It's used for component enable/disable, and
     * @param enable  A boolean to determine if we should enable Firebase or not
     * @return Return true if a new runnable is created. otherwise return false.I need this return value for testing (as the return value of bind() method)
     */
    private static boolean enableFirebase(final Context context, final boolean enable) {

        // if the task is already running, we cache the value in pending, and avoid creating a new AsyncTask.
        // I use a variable here, instead of keeping a reference to the AsyncTask below and use
        // its running state as this flag. Cause I'm hesitated to keep a reference to an AsyncTask.
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
        // We only reference application context here. But to make lint happy, I'll use an extra WeakReference for it.
        WeakReference<Context> weakApplicationContext;

        // We only need application context here.
        BlockingEnabler(Context c, boolean state) {
            enable = state;
            weakApplicationContext = new WeakReference<>(c.getApplicationContext());

        }

        @Override
        protected Void doInBackground(Void... voids) {


            // make StrictMode quiet here, cause Crashlytics has StrictMode.onUntaggedSocket violation
            // and some I/O access below will also conduct StrictModeDiskReadViolation. I'll set it back after all works are done
            final StrictMode.ThreadPolicy cachedThreadPolicy = StrictMode.getThreadPolicy();
            final StrictMode.VmPolicy cacheVmPolicy = StrictMode.getVmPolicy();
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder().build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder().build());

            if (weakApplicationContext == null || weakApplicationContext.get() == null) {
                // set back the policy if this happened.
                StrictMode.setThreadPolicy(cachedThreadPolicy);
                StrictMode.setVmPolicy(cacheVmPolicy);
                return null;
            }
            // although we should check for weakApplicationContext.get() every time before we use it,
            // but since it's an application context so we should be fine here.
            final Context applicationContext = weakApplicationContext.get();

            // this is only for testing. So we can simulate slow network..etc
            final BlockingEnablerCallback callback = FirebaseHelper.enablerCallback;
            if (callback != null) {
                callback.runDelayOnExecution();
            }

            setDeveloperModeEnabled(AppConstants.isFirebaseBuild());

            // make sure we are in the changing state
            changing = true;

            // this methods is blocking.
            updateInstanceId(applicationContext, enable);

            enableCrashlytics(applicationContext, enable);
            enableAnalytics(applicationContext, enable);
            enableCloudMessaging(applicationContext, RocketMessagingService.class.getName(), enable);
            // we only record the firebaseRemoteConfigFetched event when the app is launched.
            enableRemoteConfig(applicationContext, enable, TelemetryWrapper::firebaseRemoteConfigFetched);

            // now firebase has completed state changing,
            changing = false;
            // we'll check if the cached state is the same as our current one. If not, issue
            // a state change again.
            if (pending != null && pending != enable) {
                enableFirebase(applicationContext, pending);
            } else {
                // after now, there'll be now pending state.
                pending = null;
            }

            StrictMode.setThreadPolicy(cachedThreadPolicy);
            StrictMode.setVmPolicy(cacheVmPolicy);

            return null;
        }

        protected void onPostExecute(Void result) {
            if (pending == null) {
                LocalBroadcastManager.getInstance(weakApplicationContext.get()).sendBroadcast(new Intent(FIREBASE_READY));
            }
        }

    }

    // this is called in FirebaseWrapper's internalInit()
    @Override
    HashMap<String, Object> getRemoteConfigDefault(Context context) {

        if (remoteConfigDefault == null) {
            final boolean mayUseLocalFile = AppConstants.isDevBuild() || AppConstants.isBetaBuild();
            if (mayUseLocalFile && Looper.myLooper() != Looper.getMainLooper()) {
                // this only happens during init with
                remoteConfigDefault = fromFile(context);
            } else {
                remoteConfigDefault = fromResourceString(context);
            }
        }

        return remoteConfigDefault;
    }

    @Override
    void refreshRemoteConfigDefault(Context context) {
        // Clear remoteConfigDefault
        remoteConfigDefault = null;
        getRemoteConfigDefault(context);
        // Now also need to reset the default config in Firebase if "Send Usage Data" is turned on.
        // We don't need callback for firebaseRemoteConfigFetched cause we only want to record it when
        // the app is launched.
        enableRemoteConfig(context, TelemetryWrapper.isTelemetryEnabled(context), null);
    }


    private HashMap<String, Object> fromFile(Context context) {

        // If we don't have read external storage permission, just don't bother reading the config file.
        if (FileUtils.canReadExternalStorage(context)) {
            try {
                return FileUtils.fromJsonOnDisk(REMOTE_CONFIG_JSON);
            } catch (Exception e) {
                Log.w(TAG, "Some problem when reading RemoteConfig file from local disk: ", e);
                // For any exception, we read the default resource file.
                return fromResourceString(context);
            }
        }

        return fromResourceString(context);
    }

    // This is the default value from resource string ( so we can leverage l10n)
    private HashMap<String, Object> fromResourceString(Context context) {
        final HashMap<String, Object> map = new HashMap<>();
        if (context != null) {
            map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_TITLE, context.getString(R.string.rate_app_dialog_text_title, context.getString(R.string.app_name)));
            map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_CONTENT, context.getString(R.string.rate_app_dialog_text_content));
            map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_POSITIVE, context.getString(R.string.rate_app_dialog_btn_go_rate));
            map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_NEGATIVE, context.getString(R.string.rate_app_dialog_btn_feedback));
        }
        map.put(FirebaseHelper.RATE_APP_DIALOG_THRESHOLD, DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_DIALOG);
        map.put(FirebaseHelper.RATE_APP_NOTIFICATION_THRESHOLD, DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION);
        map.put(FirebaseHelper.SHARE_APP_DIALOG_THRESHOLD, DialogUtils.APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG);
        map.put(FirebaseHelper.ENABLE_MY_SHOT_UNREAD, MainActivity.ENABLE_MY_SHOT_UNREAD_DEFAULT);
        map.put(FirebaseHelper.BANNER_MANIFEST, HomeFragment.BANNER_MANIFEST_DEFAULT);
        map.put(FirebaseHelper.ENABLE_PRIVATE_MODE, AppConfigWrapper.PRIVATE_MODE_ENABLED_DEFAULT);
        map.put(FirebaseHelper.FEATURE_SURVEY, RemoteConfigConstants.INSTANCE.getFEATURE_SURVEY_DEFAULT());
        map.put(FirebaseHelper.SCREENSHOT_CATEGORY_MANIFEST, ScreenshotManager.SCREENSHOT_CATEGORY_MANIFEST_DEFAULT);
        map.put(FirebaseHelper.VPN_RECOMMENDER_PACKAGE, FeatureSurveyViewHelper.Constants.PACKAGE_RECOMMEND_VPN);
        map.put(FirebaseHelper.VPN_RECOMMENDER_URL, FeatureSurveyViewHelper.Constants.LINK_RECOMMEND_VPN);

        return map;
    }

    @NonNull
    private static String getStringResourceByName(Context context, String aString) {
        String packageName = context.getPackageName();
        int resId = context.getResources()
                .getIdentifier(aString, "string", packageName);
        if (resId == 0) {
            return "";
        } else {
            return context.getString(resId);
        }
    }

    public static void onLocaleUpdate(Context context) {
        getInstance().refreshRemoteConfigDefault(context);
    }
}
