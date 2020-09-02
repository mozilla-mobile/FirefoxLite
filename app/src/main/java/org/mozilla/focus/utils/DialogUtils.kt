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
import android.graphics.Typeface
import android.graphics.drawable.BitmapDrawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.text.style.StyleSpan
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
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
import kotlinx.android.synthetic.main.myshot_onboarding.view.my_shot_category_learn_more
import kotlinx.android.synthetic.main.onboarding_spotlight_content_services_request_click.view.content_services_plateform_onboarding_message
import kotlinx.android.synthetic.main.spotlight_message.view.spotlight_message
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.activity.SettingsActivity
import org.mozilla.focus.notification.NotificationId
import org.mozilla.focus.notification.NotificationUtil
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.TelemetryWrapper.clickRateApp
import org.mozilla.focus.telemetry.TelemetryWrapper.promoteShareClickEvent
import org.mozilla.focus.utils.SpotlightDialog.AttachedGravity
import org.mozilla.focus.utils.SpotlightDialog.AttachedPosition
import org.mozilla.focus.utils.SpotlightDialog.AttachedViewConfigs
import org.mozilla.focus.utils.SpotlightDialog.SpotlightConfigs.CircleSpotlightConfigs
import org.mozilla.focus.utils.SpotlightDialog.SpotlightConfigs.RectangleSpotlightConfigs
import org.mozilla.rocket.extension.dpToPx
import org.mozilla.rocket.extension.inflate
import org.mozilla.rocket.home.HomeViewModel
import org.mozilla.rocket.settings.defaultbrowser.ui.DefaultBrowserPreferenceViewModel
import org.mozilla.rocket.settings.defaultbrowser.ui.DefaultBrowserTutorialDialog
import org.mozilla.rocket.settings.defaultbrowser.ui.DefaultBrowserTutorialDialogData
import org.mozilla.rocket.theme.ThemeSettingDialogBuilder
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

    @JvmStatic
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

    fun showMyShotOnBoarding(activity: Activity, targetView: View, cancelListener: DialogInterface.OnCancelListener, learnMore: View.OnClickListener?): Dialog =
            SpotlightDialog.Builder(activity, targetView)
                    .spotlightConfigs(
                        CircleSpotlightConfigs(
                            radius = activity.resources.getDimensionPixelSize(R.dimen.myshot_focus_view_radius),
                            backgroundDimColor = ContextCompat.getColor(activity, R.color.myShotOnBoardingBackground)
                        )
                    )
                    .addView(
                        activity.inflate(R.layout.myshot_onboarding).apply {
                            my_shot_category_learn_more.setOnClickListener(learnMore)
                        },
                        RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                            addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                        }
                    )
                    .setAttachedView(
                        (activity.inflate(R.layout.spotlight_hand_pointer) as ImageView).apply {
                            scaleX = -1f
                        },
                        AttachedViewConfigs(
                            position = AttachedPosition.TOP,
                            gravity = AttachedGravity.START_ALIGN_END,
                            marginStart = activity.dpToPx(-42f),
                            marginBottom = activity.dpToPx(-42f)
                        )
                    )
                    .addView(
                        activity.inflate(R.layout.spotlight_message).apply {
                            spotlight_message.setText(R.string.my_shot_on_boarding_message)
                        }, RelativeLayout.LayoutParams(RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                            addRule(RelativeLayout.ABOVE, R.id.spotlight_hand_pointer) // Root view in spotlight_hand_pointer.xml
                            addRule(RelativeLayout.CENTER_HORIZONTAL)
                        })
                    )
                    .cancelListener(cancelListener)
                    .build()
                    .also { it.show() }

    fun showShoppingSearchSpotlight(
        activity: Activity,
        targetView: View,
        dismissListener: DialogInterface.OnDismissListener
    ): Dialog =
            SpotlightDialog.Builder(activity, targetView)
                    .spotlightConfigs(
                        CircleSpotlightConfigs(radius = activity.resources.getDimensionPixelSize(R.dimen.shopping_focus_view_radius))
                    )
                    .addView(activity.inflate(R.layout.onboarding_spotlight_shopping_search))
                    .dismissListener(dismissListener)
                    .build()
                    .also { it.show() }

    fun showContentServiceRequestClickSpotlight(
        activity: FragmentActivity,
        targetView: View,
        couponName: String,
        dismissListener: DialogInterface.OnDismissListener
    ): Dialog = SpotlightDialog.Builder(activity, targetView)
            .spotlightConfigs(
                RectangleSpotlightConfigs(
                    width = activity.resources.getDimensionPixelSize(R.dimen.content_service_focus_view_width),
                    cornerRadius = activity.resources.getDimensionPixelSize(R.dimen.content_service_focus_view_radius)
                )
            )
            .setAttachedView(
                activity.inflate(R.layout.onboarding_spotlight_content_services_request_click).apply {
                    content_services_plateform_onboarding_message.text = activity.getString(R.string.msrp_home_hint, couponName)
                },
                AttachedViewConfigs(
                    position = AttachedPosition.BOTTOM,
                    gravity = AttachedGravity.CENTER_SCREEN
                )
            )
            .dismissListener(dismissListener)
            .build()
            .also { it.show() }

    fun showThemeSettingDialog(activity: FragmentActivity, homeViewModel: HomeViewModel) {
        ThemeSettingDialogBuilder(activity, homeViewModel).show()
    }

    fun showSetAsDefaultBrowserDialog(activity: FragmentActivity, onPositiveButtonClicked: () -> Unit, onNegativeButtonClicked: () -> Unit) {
        val customContentView = View.inflate(activity, R.layout.dialog_set_as_default_browser, null)
        customContentView.findViewById<TextView>(R.id.description).apply {
            text = context.getString(
                R.string.set_as_default_dialog_subtitle,
                context.getString(R.string.app_name)
            )
        }

        val dialog = AlertDialog.Builder(activity)
            .setView(customContentView)
            .setPositiveButton(R.string.travel_dialog_2_action) { dialogInterface: DialogInterface, _: Int ->
                onPositiveButtonClicked()
                dialogInterface.dismiss()
            }
            .setNegativeButton(R.string.update_app_dialog_btn_later) { dialogInterface: DialogInterface, _: Int ->
                onNegativeButtonClicked()
                dialogInterface.dismiss()
            }
            .setCancelable(false)
            .create()

        dialog.show()

        val buttonPositive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        val buttonNegative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        @Suppress("DEPRECATION")
        if (buttonPositive != null && buttonNegative != null) {
            buttonPositive.setTextAppearance(activity, R.style.TutorialDialogPositiveButtonStyle)
            buttonNegative.setTextAppearance(activity, R.style.TutorialDialogNegativeButtonStyle)
        }
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

    fun showGoToSystemAppsSettingsDialog(context: Context, viewModel: DefaultBrowserPreferenceViewModel) {
        val title = context.getString(
            R.string.setting_default_browser_instruction_system_settings,
            context.getString(R.string.app_name)
        )
        val styleSpan = StyleSpan(Typeface.BOLD)
        val firstStepDescription = context.getString(R.string.instruction_select)
            .highlightPlaceholder(context.getString(R.string.browser_app), styleSpan)
        val secondStepDescription = context.getString(R.string.instruction_select)
            .highlightPlaceholder(context.getString(R.string.app_name), styleSpan)

        val data = DefaultBrowserTutorialDialogData(
            title = title,
            firstStepDescription = firstStepDescription,
            firstStepImageDefaultResId = R.drawable.go_to_system_default_apps_step_1,
            firstStepImageUrl = viewModel.uiModel.value?.flow1TutorialStep1ImageUrl ?: "",
            firstStepImageWidth = context.resources.getDimensionPixelSize(R.dimen.set_default_browser_tutorial_image_width),
            firstStepImageHeight = context.resources.getDimensionPixelSize(R.dimen.set_default_browser_tutorial_flow_1_step_1_image_height),
            secondStepDescription = secondStepDescription,
            secondStepImageDefaultResId = R.drawable.go_to_system_default_apps_step_2,
            secondStepImageUrl = viewModel.uiModel.value?.flow1TutorialStep2ImageUrl ?: "",
            secondStepImageWidth = context.resources.getDimensionPixelSize(R.dimen.set_default_browser_tutorial_image_width),
            secondStepImageHeight = context.resources.getDimensionPixelSize(R.dimen.set_default_browser_tutorial_flow_1_step_2_image_height),
            positiveText = context.getString(R.string.setting_default_browser_instruction_button),
            negativeText = context.getString(R.string.action_cancel)
        )
        val dialog = DefaultBrowserTutorialDialog(context, data)
            .onPositive {
                viewModel.clickGoToSystemDefaultAppsSettings()
            }
            .onNegative {
                viewModel.cancelGoToSystemDefaultAppsSettings()
            }
        dialog.show()
    }

    private fun String.highlightPlaceholder(highlight: String, styleSpan: StyleSpan): Spannable {
        val content = String.format(this, highlight)
        val start = content.indexOf(highlight)
        val end = start + highlight.length
        val highlightSpan = SpannableStringBuilder(content)
        highlightSpan.setSpan(styleSpan, start, end, Spannable.SPAN_INCLUSIVE_INCLUSIVE)
        return highlightSpan
    }

    fun showOpenUrlDialog(context: Context, viewModel: DefaultBrowserPreferenceViewModel) {
        val title = context.getString(
            R.string.setting_default_browser_instruction_open_external_link,
            context.getString(R.string.app_name)
        )
        val styleSpan = StyleSpan(Typeface.BOLD)
        val firstStepDescription = context.getString(R.string.instruction_select)
            .highlightPlaceholder(context.getString(R.string.app_name), styleSpan)
        val secondStepDescription = context.getString(R.string.instruction_tap)
            .highlightPlaceholder(context.getString(R.string.always), styleSpan)

        val data = DefaultBrowserTutorialDialogData(
            title = title,
            firstStepDescription = firstStepDescription,
            firstStepImageDefaultResId = 0,
            firstStepImageUrl = "",
            firstStepImageWidth = 0,
            firstStepImageHeight = 0,
            secondStepDescription = secondStepDescription,
            secondStepImageDefaultResId = R.drawable.open_external_step,
            secondStepImageUrl = viewModel.uiModel.value?.flow2TutorialStep2ImageUrl ?: "",
            secondStepImageWidth = context.resources.getDimensionPixelSize(R.dimen.set_default_browser_tutorial_image_width),
            secondStepImageHeight = context.resources.getDimensionPixelSize(R.dimen.set_default_browser_tutorial_flow_2_step_2_image_height),
            positiveText = context.getString(R.string.firstrun_close_button),
            negativeText = context.getString(R.string.action_cancel)
        )
        val dialog = DefaultBrowserTutorialDialog(context, data)
            .onPositive {
                viewModel.clickOpenUrl()
            }
            .onNegative {
                viewModel.cancelOpenUrl()
            }
        dialog.show()
    }
}
