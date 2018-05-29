package org.mozilla.focus.activity

import android.support.annotation.CheckResult
import org.mozilla.focus.utils.AppConfigWrapper
import kotlin.properties.Delegates

interface MainViewContract {
    fun postSurveyNotification()
    fun showRateAppDialog()
    fun showRateAppNotification()
    fun showShareAppDialog()
    fun showPrivacyPolicyUpdateNotification()
    fun showRateAppDialogFromIntent()
}

class MainPresenter(private val mainViewContract: MainViewContract) {

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


    fun accumulateAppCreateCount() = !didShowRateDialog || !didShowShareDialog || isSurveyEnabled || !didShowRateAppNotification


    fun runPromotion() {
        if (runPromotionFromIntent()) {
            // Don't run other promotion if we already displayed above promotion
            return
        }

        if (!didShowRateDialog && appCreateCount >= rateAppDialogThreshold) {
            mainViewContract.showRateAppDialog()

        } else if (didDismissRateDialog && !didShowRateAppNotification && appCreateCount >= rateAppNotificationThreshold) {
            mainViewContract.showRateAppNotification()

        } else if (!didShowShareDialog && appCreateCount >= shareAppDialogThreshold) {
            mainViewContract.showShareAppDialog()

        }

        if (isSurveyEnabled && appCreateCount >= AppConfigWrapper.getSurveyNotificationLaunchTimeThreshold()) {
            mainViewContract.postSurveyNotification()
        }

        if (shouldShowPrivacyPolicyUpdate) {
            mainViewContract.showPrivacyPolicyUpdateNotification()
        }
    }

    // return true if promotion is already handled
    @CheckResult
    fun runPromotionFromIntent(): Boolean {
        // When we receive this action, it means we need to show "Love Rocket" dialog
        if (showRateAppDialogFromIntent) {
            mainViewContract.showRateAppDialogFromIntent()

            return true
        }
        return false
    }

}