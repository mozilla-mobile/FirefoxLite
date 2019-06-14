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
import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
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
import org.mozilla.rocket.widget.CustomViewDialogData;
import org.mozilla.rocket.widget.PromotionDialog;

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

    public static PromotionDialog createRateAppDialog(@NonNull final Context context) {
        CustomViewDialogData data = new CustomViewDialogData();

        data.setDrawable(ContextCompat.getDrawable(context, R.drawable.promotion_02));

        final String configTitle = AppConfigWrapper.getRateAppDialogTitle();
        final String defaultTitle = context.getString(R.string.rate_app_dialog_text_title, context.getString(R.string.app_name));
        data.setTitle(TextUtils.isEmpty(configTitle) ? defaultTitle : configTitle);

        final String configContent = AppConfigWrapper.getRateAppDialogContent();
        final String defaultContent = context.getString(R.string.rate_app_dialog_text_content);
        data.setDescription(TextUtils.isEmpty(configContent) ? defaultContent : configContent);

        final String configPositiveText = AppConfigWrapper.getRateAppPositiveString();
        final String defaultPositiveText = context.getString(R.string.rate_app_dialog_btn_go_rate);
        final String positiveText = TextUtils.isEmpty(configPositiveText) ? defaultPositiveText : configPositiveText;
        data.setPositiveText(positiveText);

        final String configNegativeText = AppConfigWrapper.getRateAppNegativeString();
        final String defaultNegativeText = context.getString(R.string.rate_app_dialog_btn_feedback);
        final String negativeText = TextUtils.isEmpty(configNegativeText) ? defaultNegativeText : configNegativeText;
        data.setNegativeText(negativeText);

        data.setShowCloseButton(true);

        return new PromotionDialog(context, data)
                .onPositive(() -> {
                    IntentUtils.goToPlayStore(context);
                    telemetryFeedback(context, TelemetryWrapper.Value.POSITIVE);
                    return null;
                })
                .onNegative(() -> {
                    Settings.getInstance(context).setShareAppDialogDidShow();
                    IntentUtils.openUrl(context, context.getString(R.string.rate_app_feedback_url), true);
                    telemetryFeedback(context, TelemetryWrapper.Value.NEGATIVE);
                    return null;
                })
                .onClose(() -> {
                    Settings.getInstance(context).setRateAppDialogDidDismiss();
                    telemetryFeedback(context, TelemetryWrapper.Value.DISMISS);
                    return null;
                })
                .onCancel(() -> {
                    Settings.getInstance(context).setRateAppDialogDidDismiss();
                    telemetryFeedback(context, TelemetryWrapper.Value.DISMISS);
                    return null;
                })
                .addOnShowListener(() -> {
                    Settings.getInstance(context).setRateAppDialogDidShow();
                    return null;
                })
                .setCancellable(true);
    }

    private static void telemetryFeedback(final Context context, String value) {
        if (context instanceof MainActivity) {
            TelemetryWrapper.clickRateApp(value, TelemetryWrapper.Extra_Value.CONTEXTUAL_HINTS);
        } else if (context instanceof SettingsActivity) {
            TelemetryWrapper.clickRateApp(value, TelemetryWrapper.Extra_Value.SETTING);
        }
    }

    public static PromotionDialog createShareAppDialog(@NonNull final Context context) {
        CustomViewDialogData data = new CustomViewDialogData();
        data.setDrawable(ContextCompat.getDrawable(context, R.drawable.promotion_03));
        data.setTitle(AppConfigWrapper.getShareAppDialogTitle());
        data.setDescription(AppConfigWrapper.getShareAppDialogContent());
        data.setPositiveText(context.getString(R.string.share_app_dialog_btn_share));
        data.setShowCloseButton(true);

        return new PromotionDialog(context, data)
                .onPositive(() -> {
                    Intent sendIntent = new Intent(Intent.ACTION_SEND);
                    sendIntent.setType("text/plain");
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name));
                    sendIntent.putExtra(Intent.EXTRA_TEXT, AppConfigWrapper.getShareAppMessage());
                    context.startActivity(Intent.createChooser(sendIntent, null));
                    telemetryShareApp(context, TelemetryWrapper.Value.SHARE);
                    return null;
                })
                .onClose(() -> {
                    telemetryShareApp(context, TelemetryWrapper.Value.DISMISS);
                    return null;
                })
                .onCancel(() -> {
                    telemetryShareApp(context, TelemetryWrapper.Value.DISMISS);
                    return null;
                })
                .addOnShowListener(() -> {
                    Settings.getInstance(context).setShareAppDialogDidShow();
                    return null;
                })
                .setCancellable(true);
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
        showDefaultSettingNotification(context, null);
    }

    public static void showDefaultSettingNotification(Context context, String message) {

        // Let NotificationActionBroadcastReceiver handle what to do
        final Intent openDefaultBrowserSetting = IntentUtils.genDefaultBrowserSettingIntentForBroadcastReceiver(context);
        final PendingIntent openRocketPending = PendingIntent.getBroadcast(context, REQUEST_DEFAULT_CLICK, openDefaultBrowserSetting,
                PendingIntent.FLAG_ONE_SHOT);

        final String title;
        if (TextUtils.isEmpty(message)) {
            title = context.getString(R.string.preference_default_browser) + "?\uD83D\uDE0A";
        } else {
            title = message;
        }
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

    public static Dialog showSpotlight(@NonNull final Activity activity, @NonNull final View targetView, @NonNull DialogInterface.OnCancelListener onCancelListener, int messageId) {
        final ViewGroup container = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.spotlight, null);

        TextView messageTextView = container.findViewById(R.id.spotlight_message);
        messageTextView.setText(messageId);

        Dialog dialog = createSpotlightDialog(activity, targetView, container);

        // Press back key will dismiss on boarding view and menu view
        dialog.setOnCancelListener(onCancelListener);

        dialog.show();

        return dialog;
    }

    public static Dialog showMyShotOnBoarding(@NonNull final Activity activity, @NonNull final View targetView, @NonNull final DialogInterface.OnCancelListener cancelListener, View.OnClickListener learnMore) {

        final ViewGroup container = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.myshot_onboarding, null);

        container.findViewById(R.id.my_shot_category_learn_more).setOnClickListener(learnMore);

        Dialog dialog = createSpotlightDialog(activity, targetView, container);

        // Press back key will dismiss on boarding view and menu view
        dialog.setOnCancelListener(cancelListener);

        dialog.show();

        return dialog;

    }

    @CheckResult
    public static Dialog createSpotlightDialog(@NonNull final Activity activity, @NonNull final View targetView, final @NonNull ViewGroup container) {
        final int[] location = new int[2];
        final int centerX, centerY;
        // Get target view's position
        targetView.getLocationInWindow(location);

        // Get spotlight circle's center
        centerX = location[0] + targetView.getMeasuredWidth() / 2;
        centerY = location[1] + targetView.getMeasuredHeight() / 2;

        // Initialize FocusView and add it to container view's index 0(the bottom of Z-order)
        final FocusView focusView = new FocusView(activity, centerX, centerY, activity.getResources().getDimensionPixelSize(R.dimen.myshot_focus_view_radius));

        container.addView(focusView, 0);

        // Add a delegate view to determine the position of hint image and text. Also consuming the click/longClick event.
        final View delegateView = container.findViewById(R.id.spotlight_mock_menu);
        final RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) delegateView.getLayoutParams();
        params.width = targetView.getMeasuredWidth();
        params.height = targetView.getMeasuredHeight();
        params.setMargins(location[0], location[1] - ViewUtils.getStatusBarHeight(activity), 0, 0);

        final AlertDialog.Builder builder = new AlertDialog.Builder(activity, R.style.TabTrayTheme);
        builder.setView(container);

        final Dialog dialog = builder.create();

        // Click delegateView will dismiss on boarding view and open my shot panel
        delegateView.setOnClickListener(v -> {
            dialog.dismiss();
            targetView.performClick();
        });

        delegateView.setOnLongClickListener(v -> {
            dialog.dismiss();
            return targetView.performLongClick();
        });
        // Click outside of the delegateView will dismiss on boarding view
        container.setOnClickListener(v -> {
            dialog.dismiss();
        });
        return dialog;
    }

}
