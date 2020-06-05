/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.VisibleForTesting
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import org.mozilla.focus.R
import org.mozilla.focus.notification.RocketMessagingService
import org.mozilla.focus.screenshot.ScreenshotManager
import org.mozilla.rocket.periodic.FirstLaunchWorker
import org.mozilla.rocket.shopping.search.data.ShoppingSearchRemoteDataSource.Companion.RC_KEY_ENABLE_SHOPPING_SEARCH
import java.util.HashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * A wrapper around FirebaseContract. It's job:
 * 1. Interact with FirebaseContract and initialize Firebase
 * 2. Provide helper methods for Firebase related features
 */
object FirebaseHelper {

    private val TAG = "FirebaseHelper"

    // keys for user properties
    const val USER_PROPERTY_TRACKER = "tracker"
    const val USER_PROPERTY_EXPERIMENT_BUCKET = "experiment_bucket"
    const val USER_PROPERTY_DEFAULT_BROWSER_NAME = "default_browser_name"

    // keys for remote config default value
    const val RATE_APP_DIALOG_TEXT_TITLE = "rate_app_dialog_text_title"
    const val RATE_APP_DIALOG_TEXT_CONTENT = "rate_app_dialog_text_content"
    const val RATE_APP_DIALOG_TEXT_POSITIVE = "rate_app_dialog_text_positive"
    const val RATE_APP_DIALOG_TEXT_NEGATIVE = "rate_app_dialog_text_negative"

    // Key for local broadcast
    const val FIREBASE_READY = "Firebase_ready"

    internal const val RATE_APP_DIALOG_THRESHOLD = "rate_app_dialog_threshold"
    internal const val RATE_APP_NOTIFICATION_THRESHOLD = "rate_app_notification_threshold"
    internal const val SHARE_APP_DIALOG_THRESHOLD = "share_app_dialog_threshold"
    internal const val SCREENSHOT_CATEGORY_MANIFEST = "screenshot_category_manifest"
    internal const val FIRST_LAUNCH_TIMER_MINUTES = "first_launch_timer_minutes"
    internal const val FIRST_LAUNCH_NOTIFICATION = "first_launch_notification"

    private const val FIREBASE_WEB_ID = "default_web_client_id"
    private const val FIREBASE_DB_URL = "firebase_database_url"
    private const val FIREBASE_CRASH_REPORT = "google_crash_reporting_api_key"
    private const val FIREBASE_APP_ID = "google_app_id"
    private const val FIREBASE_API_KEY = "google_api_key"
    private const val FIREBASE_PROJECT_ID = "project_id"

    internal const val STR_SHARE_APP_DIALOG_TITLE = "str_share_app_dialog_title"
    internal const val STR_SHARE_APP_DIALOG_CONTENT = "str_share_app_dialog_content"
    internal const val STR_SHARE_APP_DIALOG_MSG = "str_share_app_dialog_msg"

    const val STR_BOTTOM_BAR_ITEMS_V2 = "str_bottom_bar_items_v2"
    const val STR_MENU_ITEMS = "str_menu_items"
    const val STR_MENU_BOTTOM_BAR_ITEMS = "str_menu_bottom_bar_items"
    const val STR_PRIVATE_BOTTOM_BAR_ITEMS = "str_private_bottom_bar_items"

    const val BOOL_IN_APP_UPDATE_SHOW_INTRO = "bool_in_app_update_show_intro"
    const val STR_IN_APP_UPDATE_CONFIG = "str_in_app_update_config"

    const val STR_CONTENT_HUB_ITEMS = "str_content_hub_items"
    const val BOOL_CONTENT_HUB_ITEM_TEXT_ENABLED = "bool_content_hub_item_text_enabled"

    const val STR_TOP_SITES_FIXED_ITEMS = "str_top_sites_fixed_items"
    const val STR_TOP_SITES_DEFAULT_ITEMS = "str_top_sites_default_items"

    const val STR_EXPERIMENT_NAME = "str_experiment_name"

    private const val NEWLINE_PLACE_HOLDER = "<BR>"

    private lateinit var firebaseContract: FirebaseContract

    /**
     * Get the implementation of FirebaseContract.
     * This will throw IllegalStateException if FirebaseHelper is not initiallzed.
     *
     * @return Implementation of FirebaseContract
     */
    @JvmStatic
    fun getFirebase() = firebaseContract

    @JvmStatic
    fun getUid() = firebaseContract.uid

    /**
     * The entry point to inject FirebaseContract and start the library. This method can only
     * be called once or a IllegalStateException will be thrown.
     *
     * @param context The entry point to inject FirebaseContract and start the library.
     * @param enabled false if start Firebase without Analytics, true otherwise.
     */
    @JvmStatic
    fun init(context: Context, enabled: Boolean) {

        if (AppConstants.isBuiltWithFirebase()) {
            firebaseContract = provideFirebaseImpl(context)
            Log.d(TAG, "We are using FirebaseImp")
        } else {
            firebaseContract = provideFirebaseNoOpImpl(context)
            Log.d(TAG, "We are using FirebaseNoOpImp")
        }

        // if firebaseContract is only used to initialized Firebase Helper once. It
        // doesn't make sense to use the new contract to initialize again.
        initInternal(context.applicationContext)

        enableAnalytics(context.applicationContext, enabled)

        enableCrashlytics(context.applicationContext, enabled)

        Log.d(TAG, "Firebase Helper initialized")
    }

    private fun enableCrashlytics(applicationContext: Context, enabled: Boolean) {
        firebaseContract.enableCrashlytics(applicationContext, enabled)
    }

    /**
     * This method should only be used for tests. We replace FirebaseContract. to change the default value
     *
     * @param contract FirebaseContract used to provide default value.
     */
    @VisibleForTesting
    fun replaceContract(contract: FirebaseContract) {
        firebaseContract = contract
    }

    /**
     * A helper method to display new line from remote config
     *
     * @param string The remote config string on server.
     * @return Replace "" with a new line character
     */
    @JvmStatic
    fun prettify(string: String): String {
        return string.replace(NEWLINE_PLACE_HOLDER, "\n")
    }

    /**
     * provider dummy Firebase implementation
     *
     * @param context Context used to start Firebase
     * @return FirebaseContract that defines Firebase behavior
     */
    private fun provideFirebaseNoOpImpl(context: Context): FirebaseContract {
        return FirebaseNoOpImp(provideDefaultValues(context))
    }

    /**
     * Provider actual Firebase implementation
     *
     * @param context Context used to start Firebase
     * @return FirebaseContract that defines Firebase behavior
     */
    private fun provideFirebaseImpl(context: Context): FirebaseContract {
        val webId = getStringResourceByName(context, FIREBASE_WEB_ID)
        val dbUrl = getStringResourceByName(context, FIREBASE_DB_URL)
        val crashReport = getStringResourceByName(context, FIREBASE_CRASH_REPORT)
        val appId = getStringResourceByName(context, FIREBASE_APP_ID)
        val apiKey = getStringResourceByName(context, FIREBASE_API_KEY)
        val projectId = getStringResourceByName(context, FIREBASE_PROJECT_ID)
        // Firebase will use those resources before initialized. Check them first.
        if (webId.isEmpty() || dbUrl.isEmpty() || crashReport.isEmpty() ||
            appId.isEmpty() || apiKey.isEmpty() || projectId.isEmpty()
        ) {
            throw IllegalStateException("Firebase related keys are not set")
        }

        return FirebaseImp(provideDefaultValues(context))
    }

    /**
     * @param applicationContext Used to disable Firebase Analytics. Allow null for Unit Tests.
     * @param enable If true, enable Firebase Analytics.
     */
    @JvmStatic
    fun enableAnalytics(applicationContext: Context?, enable: Boolean) {

        // applicationContext is nullable for unit tests
        if (applicationContext == null) {
            return
        }

        firebaseContract.enableAnalytics(applicationContext, enable)
    }

    // all components required for app to function.
    private fun initInternal(applicationContext: Context?) {

        // applicationContext is nullable for unit tests
        if (applicationContext == null) {
            return
        }
        firebaseContract.setDeveloperModeEnabled(AppConstants.isFirebaseBuild())
        firebaseContract.init(applicationContext)
        firebaseContract.enableRemoteConfig(applicationContext, object : FirebaseContract.Callback {
            override fun onRemoteConfigFetched() {
                LocalBroadcastManager.getInstance(applicationContext)
                    .sendBroadcast(Intent(FIREBASE_READY))
            }
        })
    }

    private fun getStringResourceByName(context: Context, resourceName: String): String {
        val packageName = context.packageName
        val resId = context.resources
            .getIdentifier(resourceName, "string", packageName)
        return if (resId == 0) {
            ""
        } else {
            context.getString(resId)
        }
    }

    /**
     * @param context Some default values are from resource string, if context is null and they'll be null.
     * @return A Hashmap contains key-value pair for remote config default value
     */
    private fun provideDefaultValues(context: Context?): HashMap<String, Any> {
        val map = HashMap<String, Any>()
        if (context != null) {
            map[RATE_APP_DIALOG_TEXT_TITLE] =
                context.getString(R.string.rate_app_dialog_text_title, context.getString(R.string.app_name))
            map[RATE_APP_DIALOG_TEXT_CONTENT] = context.getString(R.string.rate_app_dialog_text_content)
            map[RATE_APP_DIALOG_TEXT_POSITIVE] = context.getString(R.string.rate_app_dialog_btn_go_rate)
            map[RATE_APP_DIALOG_TEXT_NEGATIVE] = context.getString(R.string.rate_app_dialog_btn_feedback)
            map[FIRST_LAUNCH_NOTIFICATION] =
                context.getString(R.string.preference_default_browser) + "?\uD83D\uDE0A"
            // Share App
            map[STR_SHARE_APP_DIALOG_TITLE] = context.getString(
                R.string.share_app_dialog_text_title,
                context.getString(R.string.app_name)
            )
            map[STR_SHARE_APP_DIALOG_CONTENT] = context.getString(R.string.share_app_dialog_text_content)
            val shareAppDialogMsg = context.getString(
                R.string.share_app_promotion_text,
                context.getString(R.string.app_name), context.getString(R.string.share_app_google_play_url),
                context.getString(R.string.mozilla)
            )
            map[STR_SHARE_APP_DIALOG_MSG] = shareAppDialogMsg
        }
        map[RATE_APP_DIALOG_THRESHOLD] = DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_DIALOG
        map[RATE_APP_NOTIFICATION_THRESHOLD] = DialogUtils.APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION
        map[SHARE_APP_DIALOG_THRESHOLD] = DialogUtils.APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG
        map[SCREENSHOT_CATEGORY_MANIFEST] = ScreenshotManager.SCREENSHOT_CATEGORY_MANIFEST_DEFAULT
        map[FIRST_LAUNCH_TIMER_MINUTES] = FirstLaunchWorker.TIMER_DISABLED
        map[RC_KEY_ENABLE_SHOPPING_SEARCH] = AppConfigWrapper.RC_KEY_ENABLE_SHOPPING_SEARCH_DEFAULT

        return map
    }

    @JvmStatic
    fun setUserProperty(context: Context, tag: String, value: String) {
        firebaseContract.setFirebaseUserProperty(context, tag, value)
    }

    @JvmStatic
    fun initUserState(activity: Activity) {
        firebaseContract.initUserState(activity)
        RocketMessagingService.checkFcmTokenUploaded(activity.applicationContext)
    }

    @JvmStatic
    fun signInWithCustomToken(
        jwt: String,
        onSuccess: Function2<String?, String?, Unit>,
        onFail: Function1<String, Unit>
    ) {
        firebaseContract.signInWithCustomToken(jwt, onSuccess, onFail)
    }

    /**
     *
     * public interface OnSuccessListener<TResult> {
    void onSuccess(TResult var1);
    }
     */

    @JvmStatic
    suspend fun getUserToken() = suspendCoroutine<String?> { continuation ->
        firebaseContract.getUserToken(object : (String?) -> Unit {
            override fun invoke(p1: String?) {
                continuation.resume(p1)
            }
        })
    }

    @JvmStatic
    fun isAnonymous(): Boolean? = firebaseContract.isAnonymous()

    @JvmStatic
    fun refreshRemoteConfig(callback: (Boolean, e: Exception?) -> Unit) {
        firebaseContract.refreshRemoteConfig(callback)
    }
}
