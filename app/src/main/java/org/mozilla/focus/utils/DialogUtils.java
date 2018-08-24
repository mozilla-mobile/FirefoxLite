/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.activity.SettingsActivity;
import org.mozilla.focus.notification.NotificationId;
import org.mozilla.focus.notification.NotificationUtil;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.widget.FocusView;

public class DialogUtils {

    // default values for RemoteConfig
    public static final int APP_CREATE_THRESHOLD_FOR_RATE_DIALOG = 6;
    public static final int APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION = APP_CREATE_THRESHOLD_FOR_RATE_DIALOG + 6;
    public static final int APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG = APP_CREATE_THRESHOLD_FOR_RATE_DIALOG + 4;

    private static final int REQUEST_RATE_CLICK = 1;
    private static final int REQUEST_RATE_RATE = 2;
    private static final int REQUEST_RATE_FEEDBACK = 3;
    private static final int REQUEST_DEFAULT_CLICK = 4;
    private static final int REQUEST_PRIVACY_POLICY_CLICK = 5;

    public static void showRateAppDialog(final Context context) {
        if (context == null) {
            return;
        }

        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.setOnCancelListener(dialogInterface -> {
            Settings.getInstance(context).setRateAppDialogDidDismiss();
            telemetryFeedback(context, TelemetryWrapper.Value.DISMISS);
        });

        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_rate_app_dialog, null);

        final TextView textView = dialogView.findViewById(R.id.rate_app_dialog_textview_title);
        textView.setText(context.getString(R.string.rate_app_dialog_text_title, context.getString(R.string.app_name)));

        dialogView.findViewById(R.id.dialog_rate_app_btn_close).setOnClickListener(v -> {
            Settings.getInstance(context).setRateAppDialogDidDismiss();
            dialog.dismiss();
            telemetryFeedback(context, TelemetryWrapper.Value.DISMISS);
        });
        dialogView.findViewById(R.id.dialog_rate_app_btn_go_rate).setOnClickListener(v -> {

            IntentUtils.goToPlayStore(context);

            dialog.dismiss();
            telemetryFeedback(context, TelemetryWrapper.Value.POSITIVE);
        });
        final String title = AppConfigWrapper.getRateAppDialogTitle(context);
        if (title != null) {
            ((TextView) dialogView.findViewById(R.id.rate_app_dialog_textview_title)).setText(title);
        }

        final String content = AppConfigWrapper.getRateAppDialogContent(context);
        if (content != null) {
            ((TextView) dialogView.findViewById(R.id.rate_app_dialog_text_content)).setText(content);
        }

        dialogView.findViewById(R.id.dialog_rate_app_btn_feedback).setOnClickListener(v -> {
            // Users set negative feedback, don't ask them to share in the future
            Settings.getInstance(context).setShareAppDialogDidShow();
            IntentUtils.openUrl(context, context.getString(R.string.rate_app_feedback_url), true);
            dialog.dismiss();
            telemetryFeedback(context, TelemetryWrapper.Value.NEGATIVE);
        });
        dialog.setView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Settings.getInstance(context).setRateAppDialogDidShow();
    }

    private static void telemetryFeedback(final Context context, String value) {
        if (context instanceof MainActivity) {
            TelemetryWrapper.feedbackClickEvent(value, TelemetryWrapper.Extra_Value.CONTEXTUAL_HINTS);
        } else if (context instanceof SettingsActivity) {
            TelemetryWrapper.feedbackClickEvent(value, TelemetryWrapper.Extra_Value.SETTING);
        }
    }

    public static void showShareAppDialog(final Context context) {
        if (context == null) {
            return;
        }

        final AlertDialog dialog = new AlertDialog.Builder(context).create();
        dialog.setOnCancelListener(dialogInterface -> telemetryShareApp(context, TelemetryWrapper.Value.DISMISS));

        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_share_app_dialog, null);

        final TextView textView = dialogView.findViewById(R.id.share_app_dialog_textview_title);
        textView.setText(context.getString(R.string.share_app_dialog_text_title, context.getString(R.string.app_name)));

        dialogView.findViewById(R.id.dialog_share_app_btn_close).setOnClickListener(v -> {
            dialog.dismiss();
            telemetryShareApp(context, TelemetryWrapper.Value.DISMISS);
        });
        dialogView.findViewById(R.id.dialog_share_app_btn_share).setOnClickListener(v -> {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));
            sendIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_promotion_text, context.getString(R.string.app_name), context.getString(R.string.share_app_google_play_url), context.getString(R.string.mozilla)));
            context.startActivity(Intent.createChooser(sendIntent, null));
            dialog.dismiss();
            telemetryShareApp(context, TelemetryWrapper.Value.SHARE);
        });
        dialog.setView(dialogView);
        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
        Settings.getInstance(context).setShareAppDialogDidShow();
    }

    private static void telemetryShareApp(final Context context, String value) {
        if (context instanceof MainActivity) {
            TelemetryWrapper.promoteShareClickEvent(value, TelemetryWrapper.Extra_Value.CONTEXTUAL_HINTS);
        } else if (context instanceof SettingsActivity) {
            TelemetryWrapper.promoteShareClickEvent(value, TelemetryWrapper.Extra_Value.SETTING);
        }
    }

    public static void showRateAppNotification(Context context) {

        // Brings up Rocket and display full screen "Love Rocket" dialog

        final Intent openRocket = IntentUtils.genFeedbackNotificationClickForBroadcastReceiver(context);
        final PendingIntent openRocketPending = PendingIntent.getBroadcast(context, REQUEST_RATE_CLICK, openRocket,
                PendingIntent.FLAG_ONE_SHOT);
        final String string = context.getString(R.string.rate_app_dialog_text_title, context.getString(R.string.app_name)) + "\uD83D\uDE00";
        final NotificationCompat.Builder builder = NotificationUtil.importantBuilder(context)
                .setContentText(string)
                .setContentIntent(openRocketPending);

        // Send this intent in Broadcast receiver so we can cancel the notification there.
        // Build notification action for rate 5 stars
        final Intent rateStar = IntentUtils.genRateStarNotificationActionForBroadcastReceiver(context);
        final PendingIntent rateStarPending = PendingIntent.getBroadcast(context, REQUEST_RATE_RATE, rateStar,
                PendingIntent.FLAG_ONE_SHOT);
        builder.addAction(R.drawable.notification_rating, context.getString(R.string.rate_app_notification_action_rate), rateStarPending);

        // Send this intent in Broadcast receiver so we can canel the notification there.
        // Build notification action for  feedback
        final Intent feedback = IntentUtils.genFeedbackNotificationActionForBroadcastReceiver(context);
        final PendingIntent feedbackPending = PendingIntent.getBroadcast(context, REQUEST_RATE_FEEDBACK, feedback,
                PendingIntent.FLAG_ONE_SHOT);
        builder.addAction(R.drawable.notification_feedback, context.getString(R.string.rate_app_notification_action_feedback), feedbackPending);

        // Show notification
        NotificationUtil.sendNotification(context, NotificationId.LOVE_FIREFOX, builder);
        Settings.getInstance(context).setRateAppNotificationDidShow();
    }

    public static void showDefaultSettingNotification(Context context) {

        // Let NotificationActionBroadcastReceiver handle what to do
        final Intent openDefaultBrowserSetting;
        final PendingIntent openRocketPending;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            openDefaultBrowserSetting = new Intent(android.provider.Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS);
            openRocketPending = PendingIntent.getActivity(context, REQUEST_DEFAULT_CLICK, openDefaultBrowserSetting, PendingIntent.FLAG_ONE_SHOT);
        } else {
            openDefaultBrowserSetting = IntentUtils.genDefaultBrowserSettingIntentForBroadcastReceiver(context);
            openRocketPending = PendingIntent.getBroadcast(context, REQUEST_DEFAULT_CLICK, openDefaultBrowserSetting,
                    PendingIntent.FLAG_ONE_SHOT);
        }

        final String title = context.getString(R.string.preference_default_browser) + "?\uD83D\uDE0A";
        NotificationCompat.Builder builder = NotificationUtil.importantBuilder(context)
                .setContentTitle(title)
                .setContentIntent(openRocketPending);

        // Show notification
        NotificationUtil.sendNotification(context, NotificationId.DEFAULT_BROWSER, builder);
        Settings.getInstance(context).setDefaultBrowserSettingDidShow();
    }

    public static void showPrivacyPolicyUpdateNotification(Context context) {

        final Intent privacyPolicyUpdateNotice = IntentUtils.genPrivacyPolicyUpdateNotificationActionForBroadcastReceiver(context);
        final PendingIntent openRocketPending = PendingIntent.getBroadcast(context, REQUEST_PRIVACY_POLICY_CLICK, privacyPolicyUpdateNotice,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = NotificationUtil.importantBuilder(context)
                .setContentTitle(context.getString(R.string.privacy_policy_update_notification_title))
                .setContentText(context.getString(R.string.privacy_policy_update_notification_action))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.privacy_policy_update_notification_action)))
                .setContentIntent(openRocketPending);

        // Show notification
        NotificationUtil.sendNotification(context, NotificationId.PRIVACY_POLICY_UPDATE, builder);
        NewFeatureNotice.getInstance(context).setPrivacyPolicyUpdateNoticeDidShow();
    }

    public static Dialog showMyShotOnBoarding(@NonNull final Activity activity, @NonNull final View targetView, @NonNull final DialogInterface.OnCancelListener cancelListener, View.OnClickListener learnMore) {
        int[] location = new int[2];
        int centerX, centerY;
        // Get target view's position
        targetView.getLocationInWindow(location);

        // Get spotlight circle's center
        centerX = location[0] + targetView.getMeasuredWidth() / 2;
        centerY = location[1] + targetView.getMeasuredHeight() / 2;

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.TabTrayTheme);

        // Inflate my shot on boarding view
        final ViewGroup root = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.myshot_onboarding, null);

        // Initialize FocusView and add it to my shot on boarding view's index 0(the bottom of Z-order)
        final FocusView focusView = new FocusView(activity, centerX, centerY, activity.getResources().getDimensionPixelSize(R.dimen.myshot_focus_view_radius));
        root.addView(focusView, 0);

        // Add a mock my shot menu view to determine the position of hint image and text. Also consuming the click event.
        final View myShotMockView = root.findViewById(R.id.my_shot_mock_menu);
        final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) myShotMockView.getLayoutParams();
        params.width = targetView.getMeasuredWidth();
        params.height = targetView.getMeasuredHeight();
        params.setMargins(location[0], location[1] - ViewUtils.getStatusBarHeight(activity), 0, 0);
        root.findViewById(R.id.my_shot_category_learn_more).setOnClickListener(learnMore);
        builder.setView(root);

        final Dialog dialog = builder.create();
        dialog.show();

        // Press back key will dismiss on boarding view and menu view
        dialog.setOnCancelListener(cancelListener);

        // Click my shot mock view will dismiss on boarding view and open my shot panel
        myShotMockView.setOnClickListener(v -> {
            dialog.dismiss();
            targetView.performClick();
        });
        // Click outside of the my shot mock view will dismiss on boarding view
        root.setOnClickListener(v -> {
            dialog.dismiss();
        });
        return dialog;

    }
}
