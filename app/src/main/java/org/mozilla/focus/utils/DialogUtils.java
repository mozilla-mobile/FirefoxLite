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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.FragmentActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.CircleCrop;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;

import org.mozilla.focus.R;
import org.mozilla.focus.activity.MainActivity;
import org.mozilla.focus.activity.SettingsActivity;
import org.mozilla.focus.notification.NotificationId;
import org.mozilla.focus.notification.NotificationUtil;
import org.mozilla.focus.telemetry.TelemetryWrapper;
import org.mozilla.focus.widget.FocusView;
import org.mozilla.focus.widget.RoundRecFocusView;
import org.mozilla.rocket.widget.CustomViewDialogData;
import org.mozilla.rocket.widget.PromotionDialog;

import kotlin.Unit;

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

    public static void showLoginMultipleTimesWarningDialog(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.msrp_disqualification_title_1)
                .setMessage(R.string.msrp_disqualification_body_1)
                .setPositiveButton(R.string.msrp_disqualification_button_1, (dialog1, which) -> {
                })
                .setCancelable(false)
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.palettePeach100));
    }

    public static void showLoginMultipleTimesFinalWarningDialog(Context context) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.msrp_disqualification_title_2)
                .setMessage(R.string.msrp_disqualification_body_1)
                .setPositiveButton(R.string.msrp_disqualification_button_1, (dialog1, which) -> {
                })
                .setCancelable(false)
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.palettePeach100));
    }

    public static void showAccountDisabledDialog(Context context, DialogInterface.OnDismissListener dismissListener) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle(R.string.msrp_disqualification_title_3)
                .setMessage(R.string.msrp_disqualification_body_3)
                .setPositiveButton(R.string.msrp_disqualification_button_3, (dialog1, which) -> {
                })
                .setCancelable(false)
                .setOnDismissListener(dismissListener)
                .create();
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.palettePeach100));
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

    public static Dialog showShoppingSearchSpotlight(
            @NonNull final Activity activity,
            @NonNull final View targetView,
            @NonNull final DialogInterface.OnDismissListener dismissListener) {

        final ViewGroup container = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.onboarding_spotlight_shopping_search, null);

        Dialog dialog = createShoppingSearchSpotlightDialog(activity, targetView, container);

        dialog.setOnDismissListener(dismissListener);
        dialog.show();

        return dialog;

    }

    public static Dialog showContentServiceOnboardingSpotlight(
            @NonNull final FragmentActivity activity,
            @NonNull final View targetView,
            @NonNull final DialogInterface.OnDismissListener dismissListener,
            View.OnClickListener ok) {

        final ViewGroup container = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.onboarding_spotlight_content_services, null);

        container.findViewById(R.id.next).setOnClickListener(ok);

        Dialog dialog = createContentServiceSpotlightDialog(activity, targetView, container,
                activity.getResources().getDimensionPixelSize(R.dimen.content_service_focus_view_radius),
                activity.getResources().getDimensionPixelSize(R.dimen.content_service_focus_view_height),
                activity.getResources().getDimensionPixelSize(R.dimen.content_service_focus_view_width),
                false);

        dialog.setOnDismissListener(dismissListener);
        dialog.show();

        return dialog;
    }

    public static Dialog showContentServiceRequestClickSpotlight(
            @NonNull final FragmentActivity activity,
            @NonNull final View targetView,
            @NonNull final String couponName,
            @NonNull final DialogInterface.OnDismissListener dismissListener) {

        final ViewGroup container = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.onboarding_spotlight_content_services_request_click, null);
        TextView text = container.findViewById(R.id.content_services_plateform_onboarding_message);
        text.setText(activity.getString(R.string.msrp_home_hint, couponName));

        Dialog dialog = createContentServiceSpotlightDialog(activity, targetView, container,
                activity.getResources().getDimensionPixelSize(R.dimen.content_service_focus_view_radius),
                activity.getResources().getDimensionPixelSize(R.dimen.content_service_focus_view_height),
                activity.getResources().getDimensionPixelSize(R.dimen.content_service_focus_view_width),
                true);

        dialog.setOnDismissListener(dismissListener);
        dialog.show();

        return dialog;
    }

    public static Dialog showTravelSpotlight(
            @NonNull final Activity activity,
            @NonNull final View targetView,
            @NonNull final String cityName,
            @NonNull final DialogInterface.OnDismissListener dismissListener,
            View.OnClickListener ok) {

        final ViewGroup container = (ViewGroup) LayoutInflater.from(activity).inflate(R.layout.onboarding_spotlight_travel, null);
        TextView title = container.findViewById(R.id.travel_details_onboarding_title);
        TextView message = container.findViewById(R.id.travel_details_onboarding_message);
        title.setText(activity.getString(R.string.travel_details_onboarding_title, cityName));
        message.setText(activity.getString(R.string.travel_details_onboarding_message, cityName));
        container.findViewById(R.id.next).setOnClickListener(ok);

        Dialog dialog = createTravelSpotlightDialog(
                activity,
                targetView,
                container,
                activity.getResources().getDimensionPixelSize(R.dimen.travel_focus_view_radius),
                activity.getResources().getDimensionPixelSize(R.dimen.travel_focus_view_height),
                activity.getResources().getDimensionPixelSize(R.dimen.travel_focus_view_width));

        dialog.setOnDismissListener(dismissListener);
        dialog.show();

        return dialog;

    }

    @CheckResult
    private static Dialog createSpotlightDialog(@NonNull final Activity activity, @NonNull final View targetView, final @NonNull ViewGroup container) {
        return createSpotlightDialog(
                activity,
                targetView,
                container,
                0,
                activity.getResources().getDimensionPixelSize(R.dimen.myshot_focus_view_radius),
                0,
                0,
                FocusViewType.CIRCLE, ContextCompat.getColor(activity, R.color.myShotOnBoardingBackground),
                true
        );
    }

    @CheckResult
    private static Dialog createShoppingSearchSpotlightDialog(
            @NonNull final Activity activity,
            @NonNull final View targetView,
            final @NonNull ViewGroup container) {
        return createSpotlightDialog(
                activity,
                targetView,
                container,
                0,
                activity.getResources().getDimensionPixelSize(R.dimen.shopping_focus_view_radius),
                0,
                0,
                FocusViewType.CIRCLE,
                ContextCompat.getColor(activity, R.color.paletteBlack50),
                true
        );
    }

    @CheckResult
    private static Dialog createContentServiceSpotlightDialog(
            @NonNull final Activity activity,
            @NonNull final View targetView,
            final @NonNull ViewGroup container,
            final @NonNull Integer radius,
            final @NonNull Integer height,
            final @NonNull Integer width,
            final @NonNull Boolean cancelOnTouchOutside) {
        return createSpotlightDialog(
                activity,
                targetView,
                container,
                activity.getResources().getDimensionPixelSize(R.dimen.content_services_offset),
                radius,
                height,
                width,
                FocusViewType.ROUND_REC,
                ContextCompat.getColor(activity, R.color.paletteBlack50),
                cancelOnTouchOutside
        );
    }

    @CheckResult
    private static Dialog createTravelSpotlightDialog(
            @NonNull final Activity activity,
            @NonNull final View targetView,
            final @NonNull ViewGroup container,
            final @NonNull Integer radius,
            final @NonNull Integer height,
            final @NonNull Integer width) {
        return createSpotlightDialog(
                activity,
                targetView,
                container,
                0,
                radius,
                height,
                width,
                FocusViewType.ROUND_REC,
                ContextCompat.getColor(activity, R.color.paletteBlack50),
                false
        );
    }

    @CheckResult
    private static Dialog createSpotlightDialog(
            @NonNull final Activity activity,
            @NonNull final View targetView,
            final @NonNull ViewGroup container,
            final @NonNull Integer offsetY,
            final @NonNull Integer radius,
            final @NonNull Integer height,
            final @NonNull Integer width,
            final FocusViewType type,
            final @NonNull Integer backgroundDimColor,
            final @NonNull Boolean cancelOnTouchOutside) {
        final int[] location = new int[2];
        final int centerX, centerY;
        // Get target view's position
        targetView.getLocationInWindow(location);

        // Get spotlight circle's center
        centerX = location[0] + targetView.getMeasuredWidth() / 2;
        centerY = location[1] + targetView.getMeasuredHeight() / 2;

        // Initialize FocusView and add it to container view's index 0(the bottom of Z-order)
        View focusView = getFocusView(activity, centerX, centerY, offsetY, radius, height, width, type, backgroundDimColor);

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

        if (cancelOnTouchOutside) {
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
        }
        return dialog;
    }

    public static PromotionDialog createMissionCompleteDialog(@NonNull final Context context, String imageUrl) {
        CustomViewDialogData data = new CustomViewDialogData();

        int width = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 228, context.getResources().getDisplayMetrics());
        int height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 134, context.getResources().getDisplayMetrics());

        // TODO: don't know why image rendered with weird size
//        data.setDrawable(context.getDrawable(R.drawable.coupon));
        // TODO: temporarily workaround
        Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Bitmap couponBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.coupon);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        canvas.drawBitmap(couponBitmap, 0, 0, paint);
        data.setDrawable(new BitmapDrawable(context.getResources(), resultBitmap));

        data.setImgWidth(width);
        data.setImgHeight(height);
        data.setTitle(context.getString(R.string.msrp_completed_popup_title));
        data.setDescription(context.getString(R.string.msrp_completed_popup_body));
        data.setPositiveText(context.getString(R.string.msrp_completed_popup_button1));
        data.setNegativeText(context.getString(R.string.msrp_completed_popup_button2));
        data.setShowCloseButton(true);

        PromotionDialog dialog = new PromotionDialog(context, data)
                .setCancellable(false);
        ImageView imageView = dialog.getView().findViewById(R.id.image);
        Target target = Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .apply(new RequestOptions().transform(new CircleCrop()))
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        imageView.setImageBitmap(getCouponImage(context, width, height, resource));
                    }
                });
        dialog.addOnDismissListener(() -> {
            Glide.with(context).clear(target);
            return Unit.INSTANCE;
        });


        return dialog;
    }

    private static Bitmap getCouponImage(Context context, int width, int height, Bitmap imageBitmap) {
        int imageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64, context.getResources().getDisplayMetrics());
        int shiftX = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -15, context.getResources().getDisplayMetrics());
        int shiftY = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -1, context.getResources().getDisplayMetrics());

        Bitmap resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Bitmap couponBitmap = BitmapFactory.decodeResource(context.getResources(), R.drawable.coupon);
        Canvas canvas = new Canvas(resultBitmap);
        Paint paint = new Paint();
        canvas.drawBitmap(couponBitmap, 0, 0, paint);
        int centerX = width / 2 + shiftX;
        int centerY = height / 2 + shiftY;
        Rect src = new Rect(0, 0, imageBitmap.getWidth(), imageBitmap.getHeight());
        Rect target = new Rect(
            centerX - (imageSize / 2),
            centerY - (imageSize / 2),
            centerX + (imageSize / 2),
            centerY + (imageSize / 2)
        );
        canvas.drawBitmap(imageBitmap, src, target, paint);

        couponBitmap.recycle();

        return resultBitmap;
    }

    private static View getFocusView(Context context, int centerX, int centerY, int offsetY, int radius, int height, int width, FocusViewType type, int backgroundDimColor) {
        switch (type) {
            case CIRCLE:
                return new FocusView(context, centerX, centerY, radius, backgroundDimColor);
            case ROUND_REC:
                return new RoundRecFocusView(context, centerX, centerY, offsetY, radius, height, width, backgroundDimColor);
            default: {
                return new FocusView(context, centerX, centerY, radius, backgroundDimColor);
            }
        }
    }

    enum FocusViewType {
        CIRCLE,
        ROUND_REC,
    }
}
