/* -*- Mode: Java; c-basic-offset: 4; tab-width: 20; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.utils;

import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;

import androidx.annotation.CheckResult;
import androidx.annotation.StringRes;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import org.mozilla.focus.BuildConfig;
import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.notification.NotificationActionBroadcastReceiver;
import org.mozilla.focus.notification.RocketMessagingService;
import org.mozilla.rocket.deeplink.DeepLinkConstants;
import org.mozilla.rocket.tabs.TabView;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

public class IntentUtils {

    @VisibleForTesting
    public static final String MARKET_INTENT_URI_PACKAGE_PREFIX = "market://details?id=";
    private static final String EXTRA_BROWSER_FALLBACK_URL = "browser_fallback_url";
    public static final String EXTRA_IS_INTERNAL_REQUEST = "is_internal_request";
    public static final String EXTRA_OPEN_NEW_TAB = "open_new_tab";
    public static final String EXTRA_SHOW_RATE_DIALOG = "show_rate_dialog";

    public static final String EXTRA_NOTIFICATION_MESSAGE_ID = "ex_no_message_id";
    public static final String EXTRA_NOTIFICATION_MESSAGE = "ex_no_message";
    public static final String EXTRA_NOTIFICATION_LINK = "ex_no_link";
    public static final String EXTRA_NOTIFICATION_OPEN_URL = "ex_no_open_url";
    public static final String EXTRA_NOTIFICATION_COMMAND = "ex_no_command";
    public static final String EXTRA_NOTIFICATION_DEEP_LINK = "ex_no_deep_link";

    public static final String EXTRA_NOTIFICATION_ACTION_RATE_STAR = "ex_no_action_rate_star";
    public static final String EXTRA_NOTIFICATION_ACTION_FEEDBACK = "ex_no_action_feedback";
    public static final String EXTRA_NOTIFICATION_CLICK_DEFAULT_BROWSER = "ex_no_click_default_browser";
    public static final String EXTRA_NOTIFICATION_CLICK_LOVE_FIREFOX = "ex_no_click_love_firefox";
    public static final String EXTRA_NOTIFICATION_CLICK_PRIVACY_POLICY_UPDATE = "ex_no_click_privacy_policy_update";
    public static final String EXTRA_NOTIFICATION_NOTIFICATION_SOURCE = "ex_no_notification_source";
    public static final String EXTRA_NOTIFICATION_DELETE_NOTIFICATION = "ex_no_delete_notification";
    public static final String EXTRA_NOTIFICATION_CLICK_NOTIFICATION = "ex_no_click_notification";

    public static final String ACTION_NOTIFICATION = "action_notification";

    public static final String NOTIFICATION_SOURCE_FIREBASE = "notification_source_firebase";
    public static final String NOTIFICATION_SOURCE_FIRSTRUN = "notification_source_firstrun";


    /**
     * Find and open the appropriate app for a given Uri. If appropriate, let the user select between
     * multiple supported apps. Returns a boolean indicating whether the URL was handled. A fallback
     * URL will be opened in the supplied WebView if appropriate (in which case the URL was handled,
     * and true will also be returned). If not handled, we should  fall back to webviews error handling
     * (which ends up calling our error handling code as needed).
     * <p>
     * Note: this method "leaks" the target Uri to Android before asking the user whether they
     * want to use an external app to open the uri. Ultimately the OS can spy on anything we're
     * doing in the app, so this isn't an actual "bug".
     */
    public static boolean handleExternalUri(final Context context, final String uri) {
        // This code is largely based on Fennec's ExternalIntentDuringPrivateBrowsingPromptFragment.java
        final Intent intent;
        try {
            intent = Intent.parseUri(uri, 0);
        } catch (URISyntaxException e) {
            // Let the browser handle the url (i.e. let the browser show it's unsupported protocol /
            // invalid URL page).
            return false;
        }

        // Since we're a browser:
        intent.addCategory(Intent.CATEGORY_BROWSABLE);

        final PackageManager packageManager = context.getPackageManager();

        // This is where we "leak" the uri to the OS. If we're using the system webview, then the OS
        // already knows that we're opening this uri. Even if we're using GeckoView, the OS can still
        // see what domains we're visiting, so there's no loss of privacy here:
        final List<ResolveInfo> matchingActivities = packageManager.queryIntentActivities(intent, 0);

        if (matchingActivities.size() > 0) {
            context.startActivity(intent);
        }
        return true;
    }

    private static boolean handleUnsupportedLink(final Context context, final TabView webView, final Intent intent) {
        final String fallbackUrl = intent.getStringExtra(EXTRA_BROWSER_FALLBACK_URL);
        if (fallbackUrl != null) {
            webView.loadUrl(fallbackUrl);
            return true;
        }

        if (intent.getPackage() != null) {
            // The url included the target package:
            final String marketUri = MARKET_INTENT_URI_PACKAGE_PREFIX + intent.getPackage();
            final Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(marketUri));
            marketIntent.addCategory(Intent.CATEGORY_BROWSABLE);

            final PackageManager packageManager = context.getPackageManager();
            final ResolveInfo info = packageManager.resolveActivity(marketIntent, 0);
            final CharSequence marketTitle = info.loadLabel(packageManager);
            showConfirmationDialog(context, marketIntent,
                    context.getString(R.string.external_app_prompt_no_app_title),
                    R.string.external_app_prompt_no_app, marketTitle);

            // Stop loading, we essentially have a result.
            return true;
        }

        // If there's really no way to handle this, we just let the browser handle this URL
        // (which then shows the unsupported protocol page).
        return false;
    }

    // We only need one param for both scenarios, hence we use just one "param" argument. If we ever
    // end up needing more or a variable number we can change this, but java varargs are a bit messy
    // so let's try to avoid that seeing as it's not needed right now.
    private static void showConfirmationDialog(final Context context, final Intent targetIntent, final String title, final @StringRes int messageResource, final CharSequence param) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.DialogStyle);

        final CharSequence ourAppName = context.getString(R.string.app_name);

        builder.setTitle(title);

        builder.setMessage(context.getResources().getString(messageResource, ourAppName, param));

        builder.setPositiveButton(R.string.action_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                context.startActivity(targetIntent);
            }
        });

        builder.setNegativeButton(R.string.action_cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                dialog.dismiss();
            }
        });

        // TODO: remove this, or enable it - depending on how we decide to handle the multiple-app/>1
        // case in future.
//            if (matchingActivities.size() > 1) {
//                builder.setNeutralButton(R.string.external_app_prompt_other, new DialogInterface.OnClickListener() {
//                    @Override
//                    public void onClick(DialogInterface dialog, int which) {
//                        final String chooserTitle = activity.getResources().getString(R.string.external_multiple_apps_matched_exit);
//                        final Intent chooserIntent = Intent.createChooser(intent, chooserTitle);
//                        activity.startActivity(chooserIntent);
//                    }
//                });
//            }

        builder.show();
    }

    public static void intentOpenFile(Context context, String fileUriStr, String mimeType) throws URISyntaxException {
        if (fileUriStr != null) {
            String authorities = BuildConfig.APPLICATION_ID + ".provider.fileprovider";
            Uri fileUri = FileProvider.getUriForFile(context, authorities, new File(new URI(fileUriStr).getPath()));

            Intent launchIntent = new Intent(Intent.ACTION_VIEW);
            launchIntent.setDataAndType(fileUri, mimeType);
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_GRANT_READ_URI_PERMISSION);

            try {
                context.startActivity(launchIntent);
            } catch (Exception e) {
                openDownloadPage(context);
            }
        } else {
            openDownloadPage(context);
        }
    }

    private static void openDownloadPage(Context context) {
        Intent pageView = new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS);
        pageView.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(pageView);
    }

    @SuppressWarnings({"SameParameterValue", "WeakerAccess"})
    public static void openUrl(Context context, String url, boolean openInNewTab) {
        context.startActivity(createInternalOpenUrlIntent(context, url, openInNewTab));
    }

    public static Intent createInternalOpenUrlIntent(Context context, String url, boolean openInNewTab) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse(url),
                context,
                MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra(IntentUtils.EXTRA_IS_INTERNAL_REQUEST, true);
        intent.putExtra(IntentUtils.EXTRA_OPEN_NEW_TAB, openInNewTab);
        return intent;
    }

    public static Intent createSetDefaultBrowserIntent(Context context) {
        Intent intent = new Intent();
        intent.setClassName(context, AppConstants.LAUNCHER_ACTIVITY_ALIAS);
        intent.putExtra(RocketMessagingService.STR_PUSH_DEEP_LINK, "rocket://command?command=" + DeepLinkConstants.COMMAND_SET_DEFAULT_BROWSER);
        return intent;
    }

    // FLAG_ACTIVITY_NEW_TASK is needed if the context is not an activity
    public static void goToPlayStore(Context context) {
        goToPlayStore(context, context.getPackageName());
    }

    public static void goToPlayStore(Context context, String appPackageName) {
        goToPlayStore(context, appPackageName, () -> {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        });
    }

    public static void goToPlayStore(Context context, String appPackageName, OpenGooglePlayFallback fallback) {
        try {
            final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_INTENT_URI_PACKAGE_PREFIX + appPackageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
            //No google play install
            fallback.OnOpenFailed();
        }
    }

    public static Intent genDefaultBrowserSettingIntentForBroadcastReceiver(Context context) {

        final Intent setAsDefault = new Intent(context, NotificationActionBroadcastReceiver.class);
        setAsDefault.setAction(IntentUtils.ACTION_NOTIFICATION);
        setAsDefault.putExtra(IntentUtils.EXTRA_NOTIFICATION_CLICK_DEFAULT_BROWSER, true);
        return setAsDefault;
    }

    static Intent genRateStarNotificationActionForBroadcastReceiver(Context context) {
        final Intent rateStar = new Intent(context, NotificationActionBroadcastReceiver.class);
        rateStar.setAction(IntentUtils.ACTION_NOTIFICATION);
        rateStar.putExtra(IntentUtils.EXTRA_NOTIFICATION_ACTION_RATE_STAR, true);
        return rateStar;
    }

    static Intent genFeedbackNotificationActionForBroadcastReceiver(Context context) {
        final Intent feedback = new Intent(context, NotificationActionBroadcastReceiver.class);
        feedback.setAction(IntentUtils.ACTION_NOTIFICATION);
        feedback.putExtra(IntentUtils.EXTRA_NOTIFICATION_ACTION_FEEDBACK, true);
        return feedback;
    }

    static Intent genFeedbackNotificationClickForBroadcastReceiver(Context context) {
        final Intent openRocket = new Intent(context, NotificationActionBroadcastReceiver.class);
        openRocket.setAction(IntentUtils.ACTION_NOTIFICATION);
        openRocket.putExtra(IntentUtils.EXTRA_NOTIFICATION_CLICK_LOVE_FIREFOX, true);
        return openRocket;
    }

    static Intent genPrivacyPolicyUpdateNotificationActionForBroadcastReceiver(Context context) {
        final Intent privacyPolicyUpdate = new Intent(context, NotificationActionBroadcastReceiver.class);
        privacyPolicyUpdate.setAction(IntentUtils.ACTION_NOTIFICATION);
        privacyPolicyUpdate.putExtra(IntentUtils.EXTRA_NOTIFICATION_CLICK_PRIVACY_POLICY_UPDATE, true);
        return privacyPolicyUpdate;
    }

    public static Intent genDeleteFirebaseNotificationActionForBroadcastReceiver(Context context, String messageId, String link) {
        final Intent deleteNotification = genDeleteNotificationActionForBroadcastReceiver(context, NOTIFICATION_SOURCE_FIREBASE);
        deleteNotification.putExtra(EXTRA_NOTIFICATION_MESSAGE_ID, messageId);
        deleteNotification.putExtra(EXTRA_NOTIFICATION_LINK, link);
        return deleteNotification;
    }

    public static Intent genDeleteFirstrunNotificationActionForBroadcastReceiver(Context context, String messageId, String link) {
        final Intent deleteNotification = genDeleteNotificationActionForBroadcastReceiver(context, NOTIFICATION_SOURCE_FIRSTRUN);
        deleteNotification.putExtra(EXTRA_NOTIFICATION_MESSAGE_ID, messageId);
        deleteNotification.putExtra(EXTRA_NOTIFICATION_LINK, link);
        return deleteNotification;
    }

    public static Intent genDeleteNotificationActionForBroadcastReceiver(Context context, String source) {
        final Intent deleteNotification = new Intent(context, NotificationActionBroadcastReceiver.class);
        deleteNotification.setAction(ACTION_NOTIFICATION);
        deleteNotification.putExtra(EXTRA_NOTIFICATION_DELETE_NOTIFICATION, true);
        deleteNotification.putExtra(EXTRA_NOTIFICATION_NOTIFICATION_SOURCE, source);
        return deleteNotification;
    }

    public static Intent genFirebaseNotificationClickForBroadcastReceiver(
        Context context,
        String messageId,
        String openUrl,
        String command,
        String deepLink
    ) {
        final Intent intent = genNotificationActionIntent(context, NOTIFICATION_SOURCE_FIREBASE, openUrl, command, deepLink);
        intent.putExtra(EXTRA_NOTIFICATION_MESSAGE_ID, messageId);
        return intent;
    }

    public static Intent genFirstrunNotificationClickForBroadcastReceiver(
        Context context,
        String messageId,
        String openUrl,
        String command,
        String deepLink
    ) {
        final Intent intent = genNotificationActionIntent(context, NOTIFICATION_SOURCE_FIRSTRUN, openUrl, command, deepLink);
        intent.putExtra(EXTRA_NOTIFICATION_MESSAGE_ID, messageId);
        return intent;
    }

    private static Intent genNotificationActionIntent(
        Context context,
        String notificationSource,
        String openUrl,
        String command,
        String deepLink
    ) {
        final Intent intent = new Intent(context, NotificationActionBroadcastReceiver.class);
        intent.setAction(ACTION_NOTIFICATION);
        intent.putExtra(EXTRA_NOTIFICATION_CLICK_NOTIFICATION, true);
        intent.putExtra(EXTRA_NOTIFICATION_NOTIFICATION_SOURCE, notificationSource);
        intent.putExtra(EXTRA_NOTIFICATION_OPEN_URL, openUrl);
        intent.putExtra(EXTRA_NOTIFICATION_COMMAND, command);
        intent.putExtra(EXTRA_NOTIFICATION_DEEP_LINK, deepLink);
        intent.putExtra(EXTRA_NOTIFICATION_LINK, openUrl != null ? openUrl : (command != null ? command : deepLink));
        return intent;
    }

    @CheckResult
    public static boolean openDefaultAppsSettings(Context context) {
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException e) {
            // In some cases, a matching Activity may not exist (according to the Android docs).
            return false;
        }
    }

    public static Intent getLauncherHomeIntent() {
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return homeIntent;
    }

    public static PendingIntent getLauncherHomePendingIntent(Context context) {
        return PendingIntent.getActivity(context, 0, getLauncherHomeIntent(),
                PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public interface OpenGooglePlayFallback {
        void OnOpenFailed();
    }
}
