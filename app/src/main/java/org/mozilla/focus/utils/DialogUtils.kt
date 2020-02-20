/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */
package org.mozilla.focus.utils

import android.app.Activity
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.appcompat.app.AlertDialog
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.activity.SettingsActivity
import org.mozilla.focus.notification.NotificationId
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.TelemetryWrapper.clickRateApp
import org.mozilla.focus.telemetry.TelemetryWrapper.promoteShareClickEvent
import org.mozilla.focus.widget.FocusView
import org.mozilla.focus.widget.RoundRecFocusView
import org.mozilla.rocket.content.travel.ui.TravelCitySearchViewModel
import org.mozilla.rocket.content.travel.ui.TravelCityViewModel
import org.mozilla.rocket.extension.inflate
import org.mozilla.rocket.widget.CustomViewDialogData
import org.mozilla.rocket.widget.PromotionDialog

object DialogUtils {
    // default values for RemoteConfig
    const val APP_CREATE_THRESHOLD_FOR_RATE_DIALOG = 6
    const val APP_CREATE_THRESHOLD_FOR_RATE_NOTIFICATION = APP_CREATE_THRESHOLD_FOR_RATE_DIALOG + 6
    const val APP_CREATE_THRESHOLD_FOR_SHARE_DIALOG = APP_CREATE_THRESHOLD_FOR_RATE_DIALOG + 4
    private const val REQUEST_RATE_CLICK = 1
    private const val REQUEST_RATE_RATE = 2
    private const val REQUEST_RATE_FEEDBACK = 3
    private const val REQUEST_DEFAULT_CLICK = 4
    private const val REQUEST_PRIVACY_POLICY_CLICK = 5

    @JvmStatic
    fun createRateAppDialog(context: Context): PromotionDialog {
        val data = CustomViewDialogData()
        data.drawable = ContextCompat.getDrawable(context, R.drawable.promotion_02)
        val configTitle = AppConfigWrapper.getRateAppDialogTitle()
        val defaultTitle = context.getString(R.string.rate_app_dialog_text_title, context.getString(R.string.app_name))
        data.title = if (TextUtils.isEmpty(configTitle)) defaultTitle else configTitle
        val configContent = AppConfigWrapper.getRateAppDialogContent()
        val defaultContent = context.getString(R.string.rate_app_dialog_text_content)
        data.description = if (TextUtils.isEmpty(configContent)) defaultContent else configContent
        val configPositiveText = AppConfigWrapper.getRateAppPositiveString()
        val defaultPositiveText = context.getString(R.string.rate_app_dialog_btn_go_rate)
        val positiveText = if (TextUtils.isEmpty(configPositiveText)) defaultPositiveText else configPositiveText
        data.positiveText = positiveText
        val configNegativeText = AppConfigWrapper.getRateAppNegativeString()
        val defaultNegativeText = context.getString(R.string.rate_app_dialog_btn_feedback)
        val negativeText = if (TextUtils.isEmpty(configNegativeText)) defaultNegativeText else configNegativeText
        data.negativeText = negativeText
        data.showCloseButton = true
        return PromotionDialog(context, data)
                .onPositive {
                    IntentUtils.goToPlayStore(context)
                    telemetryFeedback(context, TelemetryWrapper.Value.POSITIVE)
                }
                .onNegative {
                    Settings.getInstance(context).setShareAppDialogDidShow()
                    IntentUtils.openUrl(context, context.getString(R.string.rate_app_feedback_url), true)
                    telemetryFeedback(context, TelemetryWrapper.Value.NEGATIVE)
                }
                .onClose {
                    Settings.getInstance(context).setRateAppDialogDidDismiss()
                    telemetryFeedback(context, TelemetryWrapper.Value.DISMISS)
                }
                .onCancel {
                    Settings.getInstance(context).setRateAppDialogDidDismiss()
                    telemetryFeedback(context, TelemetryWrapper.Value.DISMISS)
                }
                .addOnShowListener {
                    Settings.getInstance(context).setRateAppDialogDidShow()
                }
                .setCancellable(true)
    }

    private fun telemetryFeedback(context: Context, value: String) {
        if (context is MainActivity) {
            clickRateApp(value, TelemetryWrapper.Extra_Value.CONTEXTUAL_HINTS)
        } else if (context is SettingsActivity) {
            clickRateApp(value, TelemetryWrapper.Extra_Value.SETTING)
        }
    }

    @JvmStatic
    fun createShareAppDialog(context: Context): PromotionDialog {
        val data = CustomViewDialogData()
        data.drawable = ContextCompat.getDrawable(context, R.drawable.promotion_03)
        data.title = AppConfigWrapper.getShareAppDialogTitle()
        data.description = AppConfigWrapper.getShareAppDialogContent()
        data.positiveText = context.getString(R.string.share_app_dialog_btn_share)
        data.showCloseButton = true
        return PromotionDialog(context, data)
                .onPositive {
                    val sendIntent = Intent(Intent.ACTION_SEND)
                    sendIntent.type = "text/plain"
                    sendIntent.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.app_name))
                    sendIntent.putExtra(Intent.EXTRA_TEXT, AppConfigWrapper.getShareAppMessage())
                    context.startActivity(Intent.createChooser(sendIntent, null))
                    telemetryShareApp(context, TelemetryWrapper.Value.SHARE)
                }
                .onClose {
                    telemetryShareApp(context, TelemetryWrapper.Value.DISMISS)
                }
                .onCancel {
                    telemetryShareApp(context, TelemetryWrapper.Value.DISMISS)
                }
                .addOnShowListener {
                    Settings.getInstance(context).setShareAppDialogDidShow()
                }
                .setCancellable(true)
    }

    private fun telemetryShareApp(context: Context, value: String) {
        if (context is MainActivity) {
            promoteShareClickEvent(value, TelemetryWrapper.Extra_Value.CONTEXTUAL_HINTS)
        } else if (context is SettingsActivity) {
            promoteShareClickEvent(value, TelemetryWrapper.Extra_Value.SETTING)
        }
    }

    fun showLoginMultipleTimesWarningDialog(context: Context?) {
        val dialog = AlertDialog.Builder(context!!)
                .setTitle(R.string.msrp_disqualification_title_1)
                .setMessage(R.string.msrp_disqualification_body_1)
                .setPositiveButton(R.string.msrp_disqualification_button_1) { _: DialogInterface?, _: Int -> }
                .setCancelable(false)
                .create()
        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.palettePeach100))
    }

    fun showLoginMultipleTimesFinalWarningDialog(context: Context?) {
        val dialog = AlertDialog.Builder(context!!)
                .setTitle(R.string.msrp_disqualification_title_2)
                .setMessage(R.string.msrp_disqualification_body_1)
                .setPositiveButton(R.string.msrp_disqualification_button_1) { _: DialogInterface?, _: Int -> }
                .setCancelable(false)
                .create()
        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.palettePeach100))
    }

    fun showAccountDisabledDialog(context: Context?, dismissListener: DialogInterface.OnDismissListener?) {
        val dialog = AlertDialog.Builder(context!!)
                .setTitle(R.string.msrp_disqualification_title_3)
                .setMessage(R.string.msrp_disqualification_body_3)
                .setPositiveButton(R.string.msrp_disqualification_button_3) { _: DialogInterface?, _: Int -> }
                .setCancelable(false)
                .setOnDismissListener(dismissListener)
                .create()
        dialog.show()
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(ContextCompat.getColor(context, R.color.palettePeach100))
    }

    @JvmStatic
    fun showRateAppNotification(context: Context) { // Brings up Rocket and display full screen "Love Rocket" dialog
        val openRocket = IntentUtils.genFeedbackNotificationClickForBroadcastReceiver(context)
        val openRocketPending = PendingIntent.getBroadcast(context, REQUEST_RATE_CLICK, openRocket,
                PendingIntent.FLAG_ONE_SHOT)
        val string = context.getString(R.string.rate_app_dialog_text_title, context.getString(R.string.app_name)) + "\uD83D\uDE00"
        val builder = NotificationUtil.importantBuilder(context)
                .setContentText(string)
                .setContentIntent(openRocketPending)
        // Send this intent in Broadcast receiver so we can cancel the notification there.
        // Build notification action for rate 5 stars
        val rateStar = IntentUtils.genRateStarNotificationActionForBroadcastReceiver(context)
        val rateStarPending = PendingIntent.getBroadcast(context, REQUEST_RATE_RATE, rateStar,
                PendingIntent.FLAG_ONE_SHOT)
        builder.addAction(R.drawable.notification_rating, context.getString(R.string.rate_app_notification_action_rate), rateStarPending)
        // Send this intent in Broadcast receiver so we can canel the notification there.
        // Build notification action for  feedback
        val feedback = IntentUtils.genFeedbackNotificationActionForBroadcastReceiver(context)
        val feedbackPending = PendingIntent.getBroadcast(context, REQUEST_RATE_FEEDBACK, feedback,
                PendingIntent.FLAG_ONE_SHOT)
        builder.addAction(R.drawable.notification_feedback, context.getString(R.string.rate_app_notification_action_feedback), feedbackPending)
        // Show notification
        NotificationUtil.sendNotification(context, NotificationId.LOVE_FIREFOX, builder)
        Settings.getInstance(context).setRateAppNotificationDidShow()
    }

    @JvmOverloads
    fun showDefaultSettingNotification(context: Context, message: String? = null) { // Let NotificationActionBroadcastReceiver handle what to do
        val openDefaultBrowserSetting = IntentUtils.genDefaultBrowserSettingIntentForBroadcastReceiver(context)
        val openRocketPending = PendingIntent.getBroadcast(context, REQUEST_DEFAULT_CLICK, openDefaultBrowserSetting,
                PendingIntent.FLAG_ONE_SHOT)
        val title: String? = if (TextUtils.isEmpty(message)) {
            context.getString(R.string.preference_default_browser) + "?\uD83D\uDE0A"
        } else {
            message
        }
        val builder = NotificationUtil.importantBuilder(context)
                .setContentTitle(title)
                .setContentIntent(openRocketPending)
        // Show notification
        NotificationUtil.sendNotification(context, NotificationId.DEFAULT_BROWSER, builder)
        Settings.getInstance(context).setDefaultBrowserSettingDidShow()
    }

    @JvmStatic
    fun showPrivacyPolicyUpdateNotification(context: Context) {
        val privacyPolicyUpdateNotice = IntentUtils.genPrivacyPolicyUpdateNotificationActionForBroadcastReceiver(context)
        val openRocketPending = PendingIntent.getBroadcast(context, REQUEST_PRIVACY_POLICY_CLICK, privacyPolicyUpdateNotice,
                PendingIntent.FLAG_ONE_SHOT)
        val builder = NotificationUtil.importantBuilder(context)
                .setContentTitle(context.getString(R.string.privacy_policy_update_notification_title))
                .setContentText(context.getString(R.string.privacy_policy_update_notification_action))
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.privacy_policy_update_notification_action)))
                .setContentIntent(openRocketPending)
        // Show notification
        NotificationUtil.sendNotification(context, NotificationId.PRIVACY_POLICY_UPDATE, builder)
        NewFeatureNotice.getInstance(context).setPrivacyPolicyUpdateNoticeDidShow()
    }

    fun showNightModeBrightnessSpotlight(activity: Activity, targetView: View, onCancelListener: DialogInterface.OnCancelListener, messageId: Int): Dialog {
        val container = activity.inflate(R.layout.spotlight) as ViewGroup
        val messageTextView = container.findViewById<TextView>(R.id.spotlight_message)
        messageTextView.setText(messageId)
        val dialog = createSpotlightDialog(
            activity,
            targetView,
            container,
            SpotlightConfigs(
                FocusViewType.CIRCLE,
                radius = activity.resources.getDimensionPixelSize(R.dimen.myshot_focus_view_radius),
                backgroundDimColor = ContextCompat.getColor(activity, R.color.myShotOnBoardingBackground)
            ),
            true
        )
        // Press back key will dismiss on boarding view and menu view
        dialog.setOnCancelListener(onCancelListener)
        dialog.show()
        return dialog
    }

    fun showMyShotOnBoarding(activity: Activity, targetView: View, cancelListener: DialogInterface.OnCancelListener, learnMore: View.OnClickListener?): Dialog {
        val container = activity.inflate(R.layout.myshot_onboarding) as ViewGroup
        container.findViewById<View>(R.id.my_shot_category_learn_more).setOnClickListener(learnMore)
        val dialog = createSpotlightDialog(
            activity,
            targetView,
            container,
            SpotlightConfigs(
                FocusViewType.CIRCLE,
                radius = activity.resources.getDimensionPixelSize(R.dimen.myshot_focus_view_radius),
                backgroundDimColor = ContextCompat.getColor(activity, R.color.myShotOnBoardingBackground)
            ),
            true
        )
        // Press back key will dismiss on boarding view and menu view
        dialog.setOnCancelListener(cancelListener)
        dialog.show()
        return dialog
    }

    fun showShoppingSearchSpotlight(
        activity: Activity,
        targetView: View,
        dismissListener: DialogInterface.OnDismissListener
    ): Dialog {
        val container = activity.inflate(R.layout.onboarding_spotlight_shopping_search) as ViewGroup
        val dialog = createShoppingSearchSpotlightDialog(activity, targetView, container)
        dialog.setOnDismissListener(dismissListener)
        dialog.show()
        return dialog
    }

    fun showContentServiceOnboardingSpotlight(
        activity: FragmentActivity,
        targetView: View,
        dismissListener: DialogInterface.OnDismissListener,
        ok: View.OnClickListener?
    ): Dialog {
        val container = activity.inflate(R.layout.onboarding_spotlight_content_services) as ViewGroup
        container.findViewById<View>(R.id.next).setOnClickListener(ok)
        val dialog = createContentServiceSpotlightDialog(activity, targetView, container,
                activity.resources.getDimensionPixelSize(R.dimen.content_service_focus_view_radius),
                false)
        dialog.setOnDismissListener(dismissListener)
        dialog.show()
        return dialog
    }

    fun showContentServiceInTopSitesOnboardingSpotlight(
        activity: FragmentActivity,
        targetView: View,
        dismissListener: DialogInterface.OnDismissListener,
        ok: View.OnClickListener?
    ): Dialog {
        val container = activity.inflate(R.layout.onboarding_spotlight_content_services_in_top_sites) as ViewGroup
        val dialog = createContentServiceInTopSitesSpotlightDialog(activity, targetView, container,
                activity.resources.getDimensionPixelSize(R.dimen.content_service_focus_view_radius),
                false, ok)
        dialog.setOnDismissListener(dismissListener)
        dialog.show()
        return dialog
    }

    fun showContentServiceRequestClickSpotlight(
        activity: FragmentActivity,
        targetView: View,
        couponName: String,
        dismissListener: DialogInterface.OnDismissListener
    ): Dialog {
        val container = activity.inflate(R.layout.onboarding_spotlight_content_services_request_click) as ViewGroup
        val text = container.findViewById<TextView>(R.id.content_services_plateform_onboarding_message)
        text.text = activity.getString(R.string.msrp_home_hint, couponName)
        val dialog = createContentServiceSpotlightDialog(activity, targetView, container,
                activity.resources.getDimensionPixelSize(R.dimen.content_service_focus_view_radius),
                true)
        dialog.setOnDismissListener(dismissListener)
        dialog.show()
        return dialog
    }

    fun showTravelSpotlight(
        activity: Activity,
        targetView: View,
        cityName: String,
        dismissListener: DialogInterface.OnDismissListener,
        ok: View.OnClickListener?
    ): Dialog {
        val container = activity.inflate(R.layout.onboarding_spotlight_travel) as ViewGroup
        val title = container.findViewById<TextView>(R.id.travel_details_onboarding_title)
        val message = container.findViewById<TextView>(R.id.travel_details_onboarding_message)
        title.text = activity.getString(R.string.travel_onboarding_save_title, cityName)
        message.text = activity.getString(R.string.travel_onboarding_save_description, cityName)
        container.findViewById<View>(R.id.next).setOnClickListener(ok)
        val dialog = createTravelSpotlightDialog(
                activity,
                targetView,
                container,
                activity.resources.getDimensionPixelSize(R.dimen.travel_focus_view_radius),
                activity.resources.getDimensionPixelSize(R.dimen.travel_focus_view_height),
                activity.resources.getDimensionPixelSize(R.dimen.travel_focus_view_width))
        dialog.setOnDismissListener(dismissListener)
        dialog.show()
        return dialog
    }

    fun showGameSpotlight(
        activity: Activity,
        targetView: View,
        dismissListener: DialogInterface.OnDismissListener,
        ok: View.OnClickListener?
    ): Dialog {
        val container = activity.inflate(R.layout.onboarding_spotlight_game_recent_played) as ViewGroup
        container.findViewById<View>(R.id.next).setOnClickListener(ok)
        val dialog = createGameSpotlightDialog(
                activity,
                targetView,
                container,
                activity.resources.getDimensionPixelSize(R.dimen.game_focus_view_radius),
                activity.resources.getDimensionPixelSize(R.dimen.game_focus_view_height),
                activity.resources.getDimensionPixelSize(R.dimen.game_focus_view_width))
        dialog.setOnDismissListener(dismissListener)
        dialog.show()
        return dialog
    }

    fun showTravelDiscoverySearchOptionDialog(context: Context, viewModel: TravelCitySearchViewModel) {
        val data = CustomViewDialogData()
        data.drawable = ContextCompat.getDrawable(context, R.drawable.ic_search_option)
        val title = context.getString(R.string.travel_dialog_1_title)
        data.title = title
        val content = context.getString(R.string.travel_dialog_1_description, context.getString(R.string.app_name))
        data.description = content
        val positiveText = context.getString(R.string.travel_dialog_1_action_1)
        data.positiveText = positiveText
        val negativeText = context.getString(R.string.travel_dialog_1_action_2)
        data.negativeText = negativeText
        val dialog = PromotionDialog(context, data)
                .onPositive {
                    viewModel.onSearchOptionClick(context, true)
                }
                .onNegative {
                    viewModel.onSearchOptionClick(context, false)
                }
                .onCancel {
                    viewModel.onDismissSearchOption()
                }
                .setCancellable(true)
        dialog.show()
    }

    fun showChangeTravelSearchSettingDialog(context: Context, viewModel: TravelCityViewModel) {
        val data = CustomViewDialogData()
        data.drawable = ContextCompat.getDrawable(context, R.drawable.search_with_firefox)
        val title = context.getString(R.string.travel_dialog_2_title)
        data.title = title
        val content = context.getString(R.string.travel_dialog_2_description, context.getString(R.string.app_name))
        data.description = content
        val positiveText = context.getString(R.string.travel_dialog_2_action)
        data.positiveText = positiveText
        data.showCloseButton = true
        data.showDoNotAskMeAgainButton = true
        val dialog = PromotionDialog(context, data)
                .onPositive {
                    viewModel.onChangeSearchSettingAction(true)
                }
                .onClose {
                    viewModel.onChangeSearchSettingAction(false)
                }
                .onDoNotAskMeAgain { isSelected: Boolean? ->
                    viewModel.onDoNotAskMeAgainAction(isSelected!!)
                }
        dialog.show()
    }

    @CheckResult
    private fun createShoppingSearchSpotlightDialog(
        activity: Activity,
        targetView: View,
        container: ViewGroup
    ): Dialog {
        return createSpotlightDialog(
            activity,
            targetView,
            container,
            SpotlightConfigs(FocusViewType.CIRCLE, radius = activity.resources.getDimensionPixelSize(R.dimen.shopping_focus_view_radius)),
            true
        )
    }

    @CheckResult
    private fun createContentServiceSpotlightDialog(
        activity: Activity,
        targetView: View,
        container: ViewGroup,
        radius: Int,
        cancelOnTouchOutside: Boolean
    ): Dialog {
        return createSpotlightDialog(
            activity,
            targetView,
            container,
            SpotlightConfigs(
                type = FocusViewType.ROUND_REC,
                width = activity.resources.getDimensionPixelSize(R.dimen.content_service_focus_view_width),
                height = activity.resources.getDimensionPixelSize(R.dimen.content_service_focus_view_height),
                yOffsetRatio = -0.31f,
                radius = radius
            ),
            cancelOnTouchOutside
        )
    }

    @CheckResult
    private fun createContentServiceInTopSitesSpotlightDialog(
        activity: Activity,
        targetView: View,
        container: ViewGroup,
        radius: Int,
        cancelOnTouchOutside: Boolean,
        ok: View.OnClickListener?
    ): Dialog {
        val attachedView = activity.inflate(R.layout.content_services_in_top_sites_onboarding_attached_view)
        attachedView.findViewById<View>(R.id.next).setOnClickListener(ok)
        val attachedViewConfigs = AttachedViewConfigs(
            position = AttachedPosition.TOP,
            gravity = AttachedGravity.CENTER_SCREEN
        )
        return createSpotlightDialog(
            activity,
            targetView,
            container,
            SpotlightConfigs(
                type = FocusViewType.ROUND_REC,
                widthRatio = 0.65f,
                heightRatio = 0.53f,
                xOffsetRatio = -0.115f,
                yOffsetRatio = -0.29f,
                radius = radius
            ),
            cancelOnTouchOutside,
            attachedView = attachedView,
            attachedViewConfigs = attachedViewConfigs
        )
    }

    @CheckResult
    private fun createTravelSpotlightDialog(
        activity: Activity,
        targetView: View,
        container: ViewGroup,
        radius: Int,
        height: Int,
        width: Int
    ): Dialog {
        return createSpotlightDialog(
            activity,
            targetView,
            container,
            SpotlightConfigs(
                type = FocusViewType.ROUND_REC,
                width = width,
                height = height,
                radius = radius
            ),
            false
        )
    }

    @CheckResult
    private fun createGameSpotlightDialog(
        activity: Activity,
        targetView: View,
        container: ViewGroup,
        radius: Int,
        height: Int,
        width: Int
    ): Dialog {
        return createSpotlightDialog(
            activity,
            targetView,
            container,
            SpotlightConfigs(
                type = FocusViewType.ROUND_REC,
                width = width,
                height = height,
                radius = radius
            ),
            false
        )
    }

    @CheckResult
    private fun createSpotlightDialog(
        activity: Activity,
        targetView: View,
        container: ViewGroup,
        configs: SpotlightConfigs,
        cancelOnTouchOutside: Boolean,
        attachedView: View? = null,
        attachedViewConfigs: AttachedViewConfigs? = null
    ): Dialog {
        val location = IntArray(2)
        // Get target view's position
        targetView.getLocationInWindow(location)

        val offsetX = (configs.xOffsetRatio * targetView.measuredWidth).toInt()
        val offsetY = (configs.yOffsetRatio * targetView.measuredHeight).toInt()

        // Get spotlight circle's center
        val centerX = location[0] + targetView.measuredWidth / 2 + offsetX
        val centerY = location[1] + targetView.measuredHeight / 2 + offsetY

        val spotlightWidth: Int = if (configs.width > 0) {
            configs.width
        } else {
            (configs.widthRatio * targetView.measuredWidth).toInt()
        }
        val spotlightHeight: Int = if (configs.height > 0) {
            configs.height
        } else {
            (configs.heightRatio * targetView.measuredHeight).toInt()
        }

        // Initialize FocusView and add it to container view's index 0(the bottom of Z-order)
        val focusView = getFocusView(activity, centerX, centerY,
                configs.radius, spotlightHeight, spotlightWidth,
                configs.type, configs.backgroundDimColor) as ViewGroup
        container.addView(focusView, 0, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT))

        attachedView?.run {
            requireNotNull(attachedViewConfigs) { "Must to have attachedViewConfigs" }
            focusView.addView(attachedView, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                when (attachedViewConfigs.position) {
                    AttachedPosition.LEFT, AttachedPosition.RIGHT -> {
                        if (attachedViewConfigs.position == AttachedPosition.LEFT) {
                            addRule(RelativeLayout.LEFT_OF, R.id.spotlight_placeholder)
                        } else {
                            addRule(RelativeLayout.RIGHT_OF, R.id.spotlight_placeholder)
                        }
                        when (attachedViewConfigs.gravity) {
                            AttachedGravity.START -> addRule(RelativeLayout.ALIGN_TOP, R.id.spotlight_placeholder)
                            AttachedGravity.END -> addRule(RelativeLayout.ALIGN_BOTTOM, R.id.spotlight_placeholder)
                            AttachedGravity.START_SCREEN -> addRule(RelativeLayout.ALIGN_PARENT_TOP)
                            AttachedGravity.CENTER_SCREEN -> addRule(RelativeLayout.CENTER_VERTICAL)
                            AttachedGravity.END_SCREEN -> addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                        }
                    }
                    AttachedPosition.TOP, AttachedPosition.BOTTOM -> {
                        if (attachedViewConfigs.position == AttachedPosition.TOP) {
                            addRule(RelativeLayout.ABOVE, R.id.spotlight_placeholder)
                        } else {
                            addRule(RelativeLayout.BELOW, R.id.spotlight_placeholder)
                        }
                        when (attachedViewConfigs.gravity) {
                            AttachedGravity.START -> addRule(RelativeLayout.ALIGN_START, R.id.spotlight_placeholder)
                            AttachedGravity.END -> addRule(RelativeLayout.ALIGN_END, R.id.spotlight_placeholder)
                            AttachedGravity.START_SCREEN -> addRule(RelativeLayout.ALIGN_PARENT_START)
                            AttachedGravity.CENTER_SCREEN -> addRule(RelativeLayout.CENTER_HORIZONTAL)
                            AttachedGravity.END_SCREEN -> addRule(RelativeLayout.ALIGN_PARENT_END)
                        }
                    }
                }

                topMargin = attachedViewConfigs.marginTop
                bottomMargin = attachedViewConfigs.marginBottom
                leftMargin = attachedViewConfigs.marginStart
                rightMargin = attachedViewConfigs.marginEnd
            })
        }

        // Add a delegate view to determine the position of hint image and text. Also consuming the click/longClick event.
        val delegateView = container.findViewById<View>(R.id.spotlight_mock_menu)
        val params = delegateView.layoutParams as RelativeLayout.LayoutParams
        params.width = targetView.measuredWidth
        params.height = targetView.measuredHeight
        params.setMargins(location[0], location[1] - ViewUtils.getStatusBarHeight(activity), 0, 0)
        val builder = AlertDialog.Builder(activity, R.style.TabTrayTheme)
        builder.setView(container)
        val dialog: Dialog = builder.create()
        if (cancelOnTouchOutside) { // Click delegateView will dismiss on boarding view and open my shot panel
            delegateView.setOnClickListener {
                dialog.dismiss()
                targetView.performClick()
            }
            delegateView.setOnLongClickListener {
                dialog.dismiss()
                targetView.performLongClick()
            }
            // Click outside of the delegateView will dismiss on boarding view
            container.setOnClickListener { dialog.dismiss() }
        }
        return dialog
    }

    fun createMissionCompleteDialog(context: Context, imageUrl: String?): PromotionDialog {
        val data = CustomViewDialogData()
        val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 228f, context.resources.displayMetrics).toInt()
        val height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 134f, context.resources.displayMetrics).toInt()
        // TODO: don't know why image rendered with weird size
        //        data.setDrawable(context.getDrawable(R.drawable.coupon));
        // TODO: temporarily workaround
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val couponBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.coupon)
        val canvas = Canvas(resultBitmap)
        val paint = Paint()
        canvas.drawBitmap(couponBitmap, 0f, 0f, paint)
        data.drawable = BitmapDrawable(context.resources, resultBitmap)
        data.imgWidth = width
        data.imgHeight = height
        data.title = context.getString(R.string.msrp_completed_popup_title)
        data.description = context.getString(R.string.msrp_completed_popup_body)
        data.positiveText = context.getString(R.string.msrp_completed_popup_button1)
        data.negativeText = context.getString(R.string.msrp_completed_popup_button2)
        data.showCloseButton = true
        val dialog = PromotionDialog(context, data)
                .setCancellable(false)
        val imageView = dialog.view.findViewById<ImageView>(R.id.image)
        val target: Target<*> = Glide.with(context)
                .asBitmap()
                .load(imageUrl)
                .apply(RequestOptions().transform(CircleCrop()))
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>) {
                        imageView.setImageBitmap(getCouponImage(context, width, height, resource))
                    }
                })
        dialog.addOnDismissListener {
            Glide.with(context).clear(target)
        }
        return dialog
    }

    private fun getCouponImage(context: Context, width: Int, height: Int, imageBitmap: Bitmap): Bitmap {
        val imageSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 64f, context.resources.displayMetrics).toInt()
        val shiftX = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -15f, context.resources.displayMetrics).toInt()
        val shiftY = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, -1f, context.resources.displayMetrics).toInt()
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val couponBitmap = BitmapFactory.decodeResource(context.resources, R.drawable.coupon)
        val canvas = Canvas(resultBitmap)
        val paint = Paint()
        canvas.drawBitmap(couponBitmap, 0f, 0f, paint)
        val centerX = width / 2 + shiftX
        val centerY = height / 2 + shiftY
        val src = Rect(0, 0, imageBitmap.width, imageBitmap.height)
        val target = Rect(
            centerX - imageSize / 2,
            centerY - imageSize / 2,
            centerX + imageSize / 2,
            centerY + imageSize / 2
        )
        canvas.drawBitmap(imageBitmap, src, target, paint)
        couponBitmap.recycle()
        return resultBitmap
    }

    private fun getFocusView(context: Context, centerX: Int, centerY: Int, radius: Int, height: Int, width: Int, type: FocusViewType, backgroundDimColor: Int): View {
        return when (type) {
            FocusViewType.CIRCLE -> FocusView(context, centerX, centerY, radius, backgroundDimColor)
            FocusViewType.ROUND_REC -> RoundRecFocusView(context, centerX, centerY, radius, height, width, backgroundDimColor)
        }
    }

    fun createMissionForceUpdateDialog(context: Context, title: String?, description: String?, imageUrl: String?): PromotionDialog {
        val data = CustomViewDialogData()
        val width = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 134f, context.resources.displayMetrics).toInt()
        val height = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 134f, context.resources.displayMetrics).toInt()
        data.imgWidth = width
        data.imgHeight = height
        data.title = title
        data.description = description
        data.positiveText = context.getString(R.string.msrp_force_update_dialog_positive_btn)
        data.negativeText = context.getString(R.string.msrp_force_update_dialog_negative_btn)
        data.showCloseButton = true
        val dialog = PromotionDialog(context, data)
                .setCancellable(true)
        if (imageUrl != null) {
            val imageView = dialog.view.findViewById<ImageView>(R.id.image)
            imageView.visibility = View.VISIBLE
            val target: Target<*> = Glide.with(context)
                    .asBitmap()
                    .load(imageUrl)
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>) {
                            imageView.setImageBitmap(resource)
                        }
                    })
            dialog.addOnDismissListener {
                Glide.with(context).clear(target)
            }
        }
        return dialog
    }

    enum class FocusViewType {
        CIRCLE, ROUND_REC
    }

    data class SpotlightConfigs(
        val type: FocusViewType,
        val width: Int = Int.MIN_VALUE,
        val height: Int = Int.MIN_VALUE,
        val widthRatio: Float = 1.0f,
        val heightRatio: Float = 1.0f,
        val xOffsetRatio: Float = 0f,
        val yOffsetRatio: Float = 0f,
        val radius: Int = 0,
        val backgroundDimColor: Int = 0x80000000.toInt()
    )

    data class AttachedViewConfigs(
        val position: AttachedPosition,
        val gravity: AttachedGravity = AttachedGravity.START,
        val marginStart: Int = 0,
        val marginTop: Int = 0,
        val marginEnd: Int = 0,
        val marginBottom: Int = 0
    )

    enum class AttachedGravity {
        START, END, START_SCREEN, CENTER_SCREEN, END_SCREEN
    }

    enum class AttachedPosition {
        LEFT, TOP, RIGHT, BOTTOM
    }
}