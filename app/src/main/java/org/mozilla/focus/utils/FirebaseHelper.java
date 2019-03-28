/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.home.FeatureSurveyViewHelper;
import org.mozilla.focus.home.HomeFragment;
import org.mozilla.focus.screenshot.ScreenshotManager;
import org.mozilla.rocket.content.NewsSourceManager;
import org.mozilla.rocket.periodic.FirstLaunchWorker;
import org.mozilla.threadutils.ThreadUtils;

import java.util.HashMap;

import static org.mozilla.rocket.widget.NewsSourcePreference.PREF_INT_NEWS_PRIORITY;

/**
 * A wrapper around FirebaseContract. It's job:
 * 1. Interact with FirebaseContract and initialize Firebase
 * 2. Provide helper methods for Firebase related features
 */
final public class FirebaseHelper {

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
    static final String FIRST_LAUNCH_TIMER_MINUTES = "first_launch_timer_minutes";
    static final String FIRST_LAUNCH_NOTIFICATION_MESSAGE = "first_launch_notification_message";
    static final String ENABLE_LIFE_FEED = "enable_life_feed";
    static final String LIFE_FEED_PROVIDERS = "life_feed_providers";
    static final String STR_E_COMMERCE_SHOPPINGLINKS = "str_e_commerce_shoppinglinks";

    private static final String FIREBASE_WEB_ID = "default_web_client_id";
    private static final String FIREBASE_DB_URL = "firebase_database_url";
    private static final String FIREBASE_CRASH_REPORT = "google_crash_reporting_api_key";
    private static final String FIREBASE_APP_ID = "google_app_id";
    private static final String FIREBASE_API_KEY = "google_api_key";
    private static final String FIREBASE_PROJECT_ID = "project_id";

    static final String STR_SHARE_APP_DIALOG_TITLE = "str_share_app_dialog_title";
    static final String STR_SHARE_APP_DIALOG_CONTENT = "str_share_app_dialog_content";
    static final String STR_SHARE_APP_DIALOG_MSG = "str_share_app_dialog_msg";

    private static final String NEWLINE_PLACE_HOLDER = "<BR>";

    private static FirebaseContract firebaseContract;

    private FirebaseHelper() {
    }

    /**
    *  The entry point to inject FirebaseContract and start the library.
     * @param context The entry point to inject FirebaseContract and start the library.
     * @param enabled false if start Firebase without Analytics, true otherwise.
     * @param contract The Firebase implementation to init. If NoOp, remote config will return the passed-in default value
     */
    public static void init(final Context context, boolean enabled, FirebaseContract contract) {

        if (firebaseContract == null) {
            firebaseContract = contract;
        }
        firebaseContract.setRemoteConfigDefault(contract.getRemoteConfigDefault());

        initInternal(context.getApplicationContext());

        enableAnalytics(context.getApplicationContext(), enabled);

    }

    /**
     * A helper method to display new line from remote config
     *
     * @param string The remote config string on server.
     * @return Replace "</BR>" with a new line character
     */
    static String prettify(String string) {
        return string.replace(NEWLINE_PLACE_HOLDER, "\n");
    }

    /**
     * provider dummy Firebase implementation
     *
     * @param context Context used to start Firebase
     * @return FirebaseContract that defines Firebase behavior
     */
    public static FirebaseContract provideFirebaseNoOpImpl(Context context) {
        return new FirebaseNoOpImp(provideDefaultValues(context));
    }

    /**
     * Provider actual Firebase implementation
     * @param context Context used to start Firebase
     * @return FirebaseContract that defines Firebase behavior
     */
    public static FirebaseContract provideFirebaseImpl(Context context) {
        final String webId = getStringResourceByName(context, FIREBASE_WEB_ID);
        final String dbUrl = getStringResourceByName(context, FIREBASE_DB_URL);
        final String crashReport = getStringResourceByName(context, FIREBASE_CRASH_REPORT);
        final String appId = getStringResourceByName(context, FIREBASE_APP_ID);
        final String apiKey = getStringResourceByName(context, FIREBASE_API_KEY);
        final String projectId = getStringResourceByName(context, FIREBASE_PROJECT_ID);
        // Firebase will use those resources before initialized. Check them first.
        if (webId.isEmpty() || dbUrl.isEmpty() || crashReport.isEmpty() ||
                appId.isEmpty() || apiKey.isEmpty() || projectId.isEmpty()) {
            throw new IllegalStateException("Firebase related keys are not set");
        }

        return new FirebaseImpl(provideDefaultValues(context));
    }


    /**
     * Get the implementation of FirebaseContract.
     * This will throw IllegalStateException if FirebaseHelper is not initiallzed.
     * @return Implementation of FirebaseContract
     */
    @NonNull
    public static FirebaseContract getFirebase() {

        checkFirebaseInitState();

        return firebaseContract;
    }

    /**
     * @param applicationContext Used to disable Firebase Analytics. Allow null for Unit Tests.
     * @param enable If true, enable Firebase Analytics.
     */
    public static void enableAnalytics(@Nullable final Context applicationContext, final boolean enable) {

        checkFirebaseInitState();

        // applicationContext is nullable for unit tests
        if (applicationContext == null) {
            return;
        }

        firebaseContract.enableAnalytics(applicationContext, enable);
    }

    // all components required for app to function.
    private static void initInternal(final Context applicationContext) {

        checkFirebaseInitState();

        // applicationContext is nullable for unit tests
        if (applicationContext == null) {
            return;
        }
        firebaseContract.setDeveloperModeEnabled(AppConstants.isFirebaseBuild());
        firebaseContract.init(applicationContext);
        firebaseContract.enableRemoteConfig(applicationContext, () -> {
            ThreadUtils.postToBackgroundThread(() -> {
                final String pref = applicationContext.getString(R.string.pref_s_news);
                final String source = firebaseContract.getRcString(pref);
                final Settings settings = Settings.getInstance(applicationContext);
                final boolean canOverride = settings.canOverride(PREF_INT_NEWS_PRIORITY, Settings.PRIORITY_FIREBASE);
                Log.d(NewsSourceManager.TAG, "Remote Config fetched");
                if (!TextUtils.isEmpty(source) && (canOverride || TextUtils.isEmpty(settings.getNewsSource()))) {
                    Log.d(NewsSourceManager.TAG, "Remote Config is used:" + source);
                    settings.setPriority(PREF_INT_NEWS_PRIORITY, Settings.PRIORITY_FIREBASE);
                    settings.setNewsSource(source);
                    NewsSourceManager.getInstance().setNewsSource(source);
                }
                LocalBroadcastManager.getInstance(applicationContext).sendBroadcast(new Intent(FirebaseHelper.FIREBASE_READY));
            });
        });
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

    /**
     * @param context Some default values are from resource string, if context is null and they'll be null.
     * @return A Hashmap contains key-value pair for remote config default value
     */
    private static HashMap<String, Object> provideDefaultValues(@Nullable Context context) {
        final HashMap<String, Object> map = new HashMap<>();
        if (context != null) {
            map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_TITLE, context.getString(R.string.rate_app_dialog_text_title, context.getString(R.string.app_name)));
            map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_CONTENT, context.getString(R.string.rate_app_dialog_text_content));
            map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_POSITIVE, context.getString(R.string.rate_app_dialog_btn_go_rate));
            map.put(FirebaseHelper.RATE_APP_DIALOG_TEXT_NEGATIVE, context.getString(R.string.rate_app_dialog_btn_feedback));
            map.put(FirebaseHelper.FIRST_LAUNCH_NOTIFICATION_MESSAGE, context.getString(R.string.preference_default_browser) + "?\uD83D\uDE0A");
            // Share App
            map.put(FirebaseHelper.STR_SHARE_APP_DIALOG_TITLE, context.getString(R.string.share_app_dialog_text_title,
                    context.getString(R.string.app_name)));
            map.put(FirebaseHelper.STR_SHARE_APP_DIALOG_CONTENT, context.getString(R.string.share_app_dialog_text_content));
            final String shareAppDialogMsg = context.getString(R.string.share_app_promotion_text,
                    context.getString(R.string.app_name), context.getString(R.string.share_app_google_play_url),
                    context.getString(R.string.mozilla));
            map.put(FirebaseHelper.STR_SHARE_APP_DIALOG_MSG, shareAppDialogMsg);

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
        map.put(FirebaseHelper.FIRST_LAUNCH_TIMER_MINUTES, FirstLaunchWorker.TIMER_DISABLED);
        map.put(FirebaseHelper.ENABLE_LIFE_FEED, AppConfigWrapper.LIFE_FEED_ENABLED_DEFAULT);
        map.put(FirebaseHelper.LIFE_FEED_PROVIDERS, AppConfigWrapper.LIFE_FEED_PROVIDERS_DEFAULT);
        map.put(FirebaseHelper.STR_E_COMMERCE_SHOPPINGLINKS, AppConfigWrapper.STR_E_COMMERCE_SHOPPINGLINKS_DEFAULT);


        return map;
    }

    private static void checkFirebaseInitState() {
        if (firebaseContract == null) {
            throw new IllegalStateException("Firebase Helper not initialized");
        }
    }
}
