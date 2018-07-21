/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils;

import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.activity.SettingsActivity;
import org.mozilla.focus.notification.NotificationId;
import org.mozilla.focus.notification.NotificationUtil;
import org.mozilla.focus.telemetry.TelemetryWrapper;

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
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                Settings.getInstance(context).setRateAppDialogDidDismiss();
                telemetryFeedback(context, TelemetryWrapper.Value.DISMISS);
            }
        });

        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_rate_app_dialog, (ViewGroup) null);

        final TextView textView = dialogView.findViewById(R.id.rate_app_dialog_textview_title);
        textView.setText(context.getString(R.string.rate_app_dialog_text_title, context.getString(R.string.app_name)));

        dialogView.findViewById(R.id.dialog_rate_app_btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    Settings.getInstance(context).setRateAppDialogDidDismiss();
                    dialog.dismiss();
                    telemetryFeedback(context, TelemetryWrapper.Value.DISMISS);
                }
            }
        });
        dialogView.findViewById(R.id.dialog_rate_app_btn_go_rate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                IntentUtils.goToPlayStore(context);

                if (dialog != null) {
                    dialog.dismiss();
                }
                telemetryFeedback(context, TelemetryWrapper.Value.POSITIVE);
            }
        });
        final String title = FirebaseHelper.getRcString(context, FirebaseHelper.RATE_APP_DIALOG_TEXT_TITLE);
        if (title != null) {
            ((TextView) dialogView.findViewById(R.id.rate_app_dialog_textview_title)).setText(title);
        }

        final String content = FirebaseHelper.getRcString(context, FirebaseHelper.RATE_APP_DIALOG_TEXT_CONTENT);
        if (content != null) {
            ((TextView) dialogView.findViewById(R.id.rate_app_dialog_text_content)).setText(content);
        }

        dialogView.findViewById(R.id.dialog_rate_app_btn_feedback).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Users set negative feedback, don't ask them to share in the future
                Settings.getInstance(context).setShareAppDialogDidShow();
                IntentUtils.openUrl(context, context.getString(R.string.rate_app_feedback_url), true);
                if (dialog != null) {
                    dialog.dismiss();
                }
                telemetryFeedback(context, TelemetryWrapper.Value.NEGATIVE);
            }
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
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                telemetryShareApp(context, TelemetryWrapper.Value.DISMISS);
            }
        });

        View dialogView = LayoutInflater.from(context).inflate(R.layout.layout_share_app_dialog, (ViewGroup) null);

        final TextView textView = dialogView.findViewById(R.id.share_app_dialog_textview_title);
        textView.setText(context.getString(R.string.share_app_dialog_text_title, context.getString(R.string.app_name)));

        dialogView.findViewById(R.id.dialog_share_app_btn_close).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dialog != null) {
                    dialog.dismiss();
                    telemetryShareApp(context, TelemetryWrapper.Value.DISMISS);
                }
            }
        });
        dialogView.findViewById(R.id.dialog_share_app_btn_share).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent(Intent.ACTION_SEND);
                sendIntent.setType("text/plain");
                sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));
                sendIntent.putExtra(Intent.EXTRA_TEXT, context.getString(R.string.share_app_promotion_text, context.getString(R.string.app_name), context.getString(R.string.share_app_google_play_url)));
                context.startActivity(Intent.createChooser(sendIntent, null));
                if (dialog != null) {
                    dialog.dismiss();
                }
                telemetryShareApp(context, TelemetryWrapper.Value.SHARE);
            }
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
        final String string = context.getString(R.string.rate_app_dialog_text_title) + "\uD83D\uDE00";
        final NotificationCompat.Builder builder = NotificationUtil.generateNotificationBuilder(context, openRocketPending)
                .setContentText(string);

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
        final Intent openDefaultBrowserSetting = IntentUtils.genDefaultBrowserSettingIntentForBroadcastReceiver(context);
        final PendingIntent openRocketPending = PendingIntent.getBroadcast(context, REQUEST_DEFAULT_CLICK, openDefaultBrowserSetting,
                PendingIntent.FLAG_ONE_SHOT);

        final String title = context.getString(R.string.preference_default_browser) + "?\uD83D\uDE0A";
        NotificationCompat.Builder builder = NotificationUtil.generateNotificationBuilder(context, openRocketPending)
                .setContentTitle(title);

        // Show notification
        NotificationUtil.sendNotification(context, NotificationId.DEFAULT_BROWSER, builder);
        Settings.getInstance(context).setDefaultBrowserSettingDidShow();
    }

    public static void showPrivacyPolicyUpdateNotification(Context context) {

        final Intent privacyPolicyUpdateNotice = IntentUtils.genPrivacyPolicyUpdateNotificationActionForBroadcastReceiver(context);
        final PendingIntent openRocketPending = PendingIntent.getBroadcast(context, REQUEST_PRIVACY_POLICY_CLICK, privacyPolicyUpdateNotice,
                PendingIntent.FLAG_ONE_SHOT);

        NotificationCompat.Builder builder = NotificationUtil.generateNotificationBuilder(context, openRocketPending)
                .setContentTitle(context.getString(R.string.privacy_policy_update_notification_title))
                .setContentText(context.getString(R.string.privacy_policy_update_notification_action))
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.privacy_policy_update_notification_action)));

        // Show notification
        NotificationUtil.sendNotification(context, NotificationId.PRIVACY_POLICY_UPDATE, builder);
        NewFeatureNotice.getInstance(context).setPrivacyPolicyUpdateNoticeDidShow();
    }
}
