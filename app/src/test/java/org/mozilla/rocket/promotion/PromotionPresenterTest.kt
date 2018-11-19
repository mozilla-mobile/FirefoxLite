package org.mozilla.rocket.promotion

import org.junit.Test
import org.mockito.Mockito
import org.mockito.Mockito.mock

class PromotionPresenterTest {

    private var settingRateAppDialogThreshold = 0L
    private var settingRateAppNotificationThreshold = 0L
    private var settingShareAppDialogThreshold = 0L
    private val model = mock(PromotionModel::class.java)
    private val view = mock(PromotionViewContract::class.java)

    @Test
    fun extendShareIfDismissRate1() {

        // rate app dialog threshold not reached
        settingRateAppDialogThreshold = 6L
        settingRateAppNotificationThreshold = settingRateAppDialogThreshold + 6L
        settingShareAppDialogThreshold = settingRateAppNotificationThreshold + 4L

        verifyLaunch(20)
    }

    @Test
    fun extendShareIfDismissRate2() {
        // rate app dialog threshold not reached
        settingRateAppDialogThreshold = 3L
        settingRateAppNotificationThreshold = settingRateAppDialogThreshold + 3L
        settingShareAppDialogThreshold = settingRateAppNotificationThreshold + 2L

        verifyLaunch(10)
    }

    private fun verifyLaunch(times: Int) {

        Mockito.`when`(model.rateAppDialogThreshold).thenReturn(settingRateAppDialogThreshold)
        Mockito.`when`(model.rateAppNotificationThreshold).thenReturn(settingRateAppNotificationThreshold)
        Mockito.`when`(model.shareAppDialogThreshold).thenReturn(settingShareAppDialogThreshold)
        Mockito.`when`(model.didShowRateDialog).thenReturn(false)

        for (i in 1..times) {
            Mockito.`when`(model.appCreateCount).thenReturn(i)
            PromotionPresenter.runPromotion(view, model)

            when {
                i < settingRateAppDialogThreshold.toInt() -> {
                    Mockito.verify(view, Mockito.times(0)).showRateAppDialog()
                    Mockito.verify(view, Mockito.times(0)).showRateAppNotification()
                    Mockito.verify(view, Mockito.times(0)).showShareAppDialog()
                }
                i == settingRateAppDialogThreshold.toInt() -> {
                    Mockito.verify(view, Mockito.times(1)).showRateAppDialog()
                    Mockito.verify(view, Mockito.times(0)).showRateAppNotification()
                    Mockito.verify(view, Mockito.times(0)).showShareAppDialog()
                    Mockito.`when`(model.didShowRateDialog).thenReturn(true)
                    Mockito.`when`(model.didDismissRateDialog).thenReturn(true)
                }
                i == settingRateAppNotificationThreshold.toInt() -> {
                    Mockito.verify(view, Mockito.times(1)).showRateAppDialog()
                    Mockito.verify(view, Mockito.times(1)).showRateAppNotification()
                    Mockito.verify(view, Mockito.times(0)).showShareAppDialog()
                    Mockito.`when`(model.didShowRateAppNotification).thenReturn(true)
                }
                i == settingShareAppDialogThreshold.toInt() -> {
                    Mockito.verify(view, Mockito.times(1)).showRateAppDialog()
                    Mockito.verify(view, Mockito.times(1)).showRateAppNotification()
                    Mockito.verify(view, Mockito.times(1)).showShareAppDialog()
                    Mockito.`when`(model.didShowShareDialog).thenReturn(true)
                }
                i > settingShareAppDialogThreshold.toInt() -> {
                    Mockito.verify(view, Mockito.times(1)).showRateAppDialog()
                    Mockito.verify(view, Mockito.times(1)).showRateAppNotification()
                    Mockito.verify(view, Mockito.times(1)).showShareAppDialog()
                }
            }
        }
    }

    @Test
    fun runPromotionFromIntent() {

        Mockito.`when`(model.rateAppDialogThreshold).thenReturn(settingRateAppDialogThreshold)
        Mockito.`when`(model.rateAppNotificationThreshold).thenReturn(settingRateAppNotificationThreshold)
        Mockito.`when`(model.shareAppDialogThreshold).thenReturn(settingShareAppDialogThreshold)
        Mockito.`when`(model.showRateAppDialogFromIntent).thenReturn(false)
        PromotionPresenter.runPromotionFromIntent(view, model)
        Mockito.verify(view, Mockito.times(0)).showRateAppDialogFromIntent()

        Mockito.`when`(model.showRateAppDialogFromIntent).thenReturn(true)
        PromotionPresenter.runPromotionFromIntent(view, model)
        Mockito.verify(view, Mockito.times(1)).showRateAppDialogFromIntent()
    }

    @Test
    fun shouldShowPrivacyPolicyUpdate() {

        Mockito.`when`(model.shouldShowPrivacyPolicyUpdate).thenReturn(false)
        PromotionPresenter.runPromotion(view, model)
        Mockito.verify(view, Mockito.times(0)).showPrivacyPolicyUpdateNotification()

        Mockito.`when`(model.shouldShowPrivacyPolicyUpdate).thenReturn(true)
        PromotionPresenter.runPromotion(view, model)
        Mockito.verify(view, Mockito.times(1)).showPrivacyPolicyUpdateNotification()
    }
}