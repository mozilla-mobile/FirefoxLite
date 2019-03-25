package org.mozilla.rocket.promotion

import android.content.Context
import android.support.annotation.VisibleForTesting
import org.mozilla.focus.utils.AppConfigWrapper
import org.mozilla.focus.utils.IntentUtils
import org.mozilla.focus.utils.NewFeatureNotice
import org.mozilla.focus.utils.SafeIntent
import org.mozilla.focus.utils.Settings
import kotlin.properties.Delegates

interface PromotionViewContract {
    fun postSurveyNotification()
    fun showRateAppDialog()
    fun showRateAppNotification()
    fun showShareAppDialog()
    fun showPrivacyPolicyUpdateNotification()
    fun showRateAppDialogFromIntent()
}

class PromotionModel {

    // using a notnull delegate will make sure if the value is not set, it'll throw exception
    var didShowRateDialog by Delegates.notNull<Boolean>()
    var didShowShareDialog by Delegates.notNull<Boolean>()
    var isSurveyEnabled by Delegates.notNull<Boolean>()
    var didShowRateAppNotification by Delegates.notNull<Boolean>()
    var didDismissRateDialog by Delegates.notNull<Boolean>()
    var appCreateCount by Delegates.notNull<Int>()

    var rateAppDialogThreshold by Delegates.notNull<Long>()
    var rateAppNotificationThreshold by Delegates.notNull<Long>()
    var shareAppDialogThreshold by Delegates.notNull<Long>()

    var shouldShowPrivacyPolicyUpdate by Delegates.notNull<Boolean>()

    var showRateAppDialogFromIntent by Delegates.notNull<Boolean>()

    constructor(context: Context, safeIntent: SafeIntent) : this(Settings.getInstance(context).eventHistory, NewFeatureNotice.getInstance(context), safeIntent)
    @VisibleForTesting
    constructor(history: Settings.EventHistory, newFeatureNotice: NewFeatureNotice, safeIntent: SafeIntent) {

        parseIntent(safeIntent)

        didShowRateDialog = history.contains(Settings.Event.ShowRateAppDialog)
        didShowShareDialog = history.contains(Settings.Event.ShowShareAppDialog)
        didDismissRateDialog = history.contains(Settings.Event.DismissRateAppDialog)
        didShowRateAppNotification = history.contains(Settings.Event.ShowRateAppNotification)
        isSurveyEnabled = AppConfigWrapper.isSurveyNotificationEnabled() && !history.contains(Settings.Event.PostSurveyNotification)
        if (accumulateAppCreateCount()) {
            history.add(Settings.Event.AppCreate)
        }
        appCreateCount = history.getCount(Settings.Event.AppCreate)
        rateAppDialogThreshold = AppConfigWrapper.getRateDialogLaunchTimeThreshold()
        rateAppNotificationThreshold = AppConfigWrapper.getRateAppNotificationLaunchTimeThreshold()
        shareAppDialogThreshold = AppConfigWrapper.getShareDialogLaunchTimeThreshold(didDismissRateDialog)

        shouldShowPrivacyPolicyUpdate = newFeatureNotice.shouldShowPrivacyPolicyUpdate()
    }

    fun parseIntent(safeIntent: SafeIntent?) {
        showRateAppDialogFromIntent = safeIntent?.getBooleanExtra(IntentUtils.EXTRA_SHOW_RATE_DIALOG, false) == true
    }

    private fun accumulateAppCreateCount() = !didShowRateDialog || !didShowShareDialog || isSurveyEnabled || !didShowRateAppNotification
}

class PromotionPresenter {
    companion object {

        @JvmStatic
        fun runPromotion(promotionViewContract: PromotionViewContract, promotionModel: PromotionModel) {
            if (runPromotionFromIntent(promotionViewContract, promotionModel)) {
                // Don't run other promotion if we already displayed above promotion
                return
            }

            if (!promotionModel.didShowRateDialog && promotionModel.appCreateCount >= promotionModel.rateAppDialogThreshold) {
                promotionViewContract.showRateAppDialog()
            } else if (promotionModel.didDismissRateDialog && !promotionModel.didShowRateAppNotification && promotionModel.appCreateCount >= promotionModel.rateAppNotificationThreshold) {
                promotionViewContract.showRateAppNotification()
            } else if (!promotionModel.didShowShareDialog && promotionModel.appCreateCount >= promotionModel.shareAppDialogThreshold) {
                promotionViewContract.showShareAppDialog()
            }

            if (promotionModel.isSurveyEnabled && promotionModel.appCreateCount >= AppConfigWrapper.getSurveyNotificationLaunchTimeThreshold()) {
                promotionViewContract.postSurveyNotification()
            }

            if (promotionModel.shouldShowPrivacyPolicyUpdate) {
                promotionViewContract.showPrivacyPolicyUpdateNotification()
            }
        }

        @JvmStatic
        // return true if promotion is already handled
        fun runPromotionFromIntent(promotionViewContract: PromotionViewContract, promotionModel: PromotionModel): Boolean {
            // When we receive this action, it means we need to show "Love Rocket" dialog
            if (promotionModel.showRateAppDialogFromIntent) {
                promotionViewContract.showRateAppDialogFromIntent()
                return true
            }
            return false
        }
    }
}