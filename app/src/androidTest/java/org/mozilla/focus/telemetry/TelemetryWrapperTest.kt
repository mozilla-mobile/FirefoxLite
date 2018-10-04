package org.mozilla.focus.telemetry

import android.content.Context
import android.preference.PreferenceManager
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.text.TextUtils
import org.junit.Before

import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.R
import org.mozilla.focus.generated.LocaleList
import org.mozilla.focus.locale.LocaleManager
import org.mozilla.focus.locale.Locales
import org.mozilla.focus.screenshot.ScreenshotManager

import java.util.Locale

import org.mozilla.focus.telemetry.TelemetryWrapper.Object.SETTING
import org.mozilla.focus.telemetry.TelemetryWrapper.Value.AUDIO
import org.mozilla.focus.telemetry.TelemetryWrapper.Value.EME
import org.mozilla.focus.telemetry.TelemetryWrapper.Value.MIDI
import org.mozilla.focus.telemetry.TelemetryWrapper.Value.VIDEO

@RunWith(AndroidJUnit4::class)
class TelemetryWrapperTest {

    lateinit var context: Context
    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefName = context.getString(R.string.pref_key_telemetry)
        val preferences = PreferenceManager.getDefaultSharedPreferences(context)
        preferences.edit().putBoolean(prefName, false).apply()

    }

    private fun assertFirebaseEvent(category: String, method: String, `object`: String?, value: String) {
        val eventFromBuilder = TelemetryWrapper.EventBuilder(category, method, `object`, value).firebaseEvent
        val event = FirebaseEvent(category, method, `object`, value)
        assert(event == eventFromBuilder)
    }

    @Test
    fun toggleFirstRunPageEvent() {

        // TelemetryWrapper.toggleFirstRunPageEvent(false);
        assertFirebaseEvent(TelemetryWrapper.Category.ACTION, TelemetryWrapper.Method.CHANGE, TelemetryWrapper.Object.FIRSTRUN, TelemetryWrapper.Value.TURBO)

    }

    @Test
    fun finishFirstRunEvent() {
        TelemetryWrapper.finishFirstRunEvent(3000000)
    }

    @Test
    fun browseIntentEvent() {
        TelemetryWrapper.browseIntentEvent()
    }

    @Test
    fun textSelectionIntentEvent() {
        TelemetryWrapper.textSelectionIntentEvent()
    }

    @Test
    fun launchByAppLauncherEvent() {
        TelemetryWrapper.launchByAppLauncherEvent()
    }

    @Test
    fun launchByHomeScreenShortcutEvent() {
        TelemetryWrapper.launchByHomeScreenShortcutEvent()
    }

    @Test
    fun launchByTextSelectionSearchEvent() {
        TelemetryWrapper.launchByTextSelectionSearchEvent()
    }

    @Test
    fun launchByExternalAppEvent() {
        TelemetryWrapper.launchByExternalAppEvent()
    }

    @Test
    fun settingsEvent() {
        val pm = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
        for (key in pm.all.keys) {

            TelemetryWrapper.settingsEvent(key, java.lang.Boolean.FALSE.toString())
        }
    }

    @Test
    fun settingsClickEvent() {
        val pm = PreferenceManager.getDefaultSharedPreferences(InstrumentationRegistry.getInstrumentation().targetContext)
        for (key in pm.all.keys) {
            // invalid events should be by pass
            TelemetryWrapper.settingsClickEvent(key)
        }
    }

    @Test
    fun settingsLearnMoreClickEvent() {

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        TelemetryWrapper.settingsLearnMoreClickEvent(context.getString(R.string.pref_key_turbo_mode))
        TelemetryWrapper.settingsLearnMoreClickEvent(context.getString(R.string.pref_key_telemetry))

    }

    @Test
    fun settingsLocaleChangeEvent() {
        val localeManager = LocaleManager.getInstance()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (value in LocaleList.BUNDLED_LOCALES) {
            val locale: Locale?
            if (TextUtils.isEmpty(value)) {
                localeManager.resetToSystemLocale(context)
                locale = localeManager.getCurrentLocale(context)
            } else {
                locale = Locales.parseLocaleCode(value)
                localeManager.setSelectedLocale(context, value)
            }
            TelemetryWrapper.settingsLocaleChangeEvent(context.getString(R.string.pref_key_locale), locale.toString(), false)
        }
    }

    @Test
    fun startSession() {
        TelemetryWrapper.startSession()
    }


    @Test
    fun stopMainActivity() {
        TelemetryWrapper.stopMainActivity()
    }

    @Test
    fun openWebContextMenuEvent() {
        TelemetryWrapper.openWebContextMenuEvent()
    }

    @Test
    fun cancelWebContextMenuEvent() {
        TelemetryWrapper.cancelWebContextMenuEvent()
    }

    @Test
    fun shareLinkEvent() {
        TelemetryWrapper.shareLinkEvent()
    }

    @Test
    fun shareImageEvent() {
        TelemetryWrapper.shareImageEvent()
    }

    @Test
    fun saveImageEvent() {
        TelemetryWrapper.saveImageEvent()
    }

    @Test
    fun copyLinkEvent() {
        TelemetryWrapper.copyLinkEvent()
    }

    @Test
    fun copyImageEvent() {
        TelemetryWrapper.copyImageEvent()
    }

    @Test
    fun addNewTabFromContextMenu() {
        TelemetryWrapper.addNewTabFromContextMenu()
    }

    @Test
    fun browseGeoLocationPermissionEvent() {
        TelemetryWrapper.browseGeoLocationPermissionEvent()
    }

    @Test
    fun browseFilePermissionEvent() {
        TelemetryWrapper.browseFilePermissionEvent()
    }

    @Test
    fun browsePermissionEvent() {
        TelemetryWrapper.browsePermissionEvent(arrayOf(AUDIO, VIDEO, EME, MIDI))
    }

    @Test
    fun browseEnterFullScreenEvent() {
        TelemetryWrapper.browseEnterFullScreenEvent()
    }

    @Test
    fun browseExitFullScreenEvent() {
        TelemetryWrapper.browseExitFullScreenEvent()
    }

    @Test
    fun showMenuHome() {
        TelemetryWrapper.showMenuHome()
    }

    @Test
    fun showTabTrayHome() {
        TelemetryWrapper.showTabTrayHome()
    }

    @Test
    fun showTabTrayToolbar() {
        TelemetryWrapper.showTabTrayToolbar()
    }

    @Test
    fun showMenuToolbar() {
        TelemetryWrapper.showMenuToolbar()
    }

    @Test
    fun clickMenuDownload() {
        TelemetryWrapper.clickMenuDownload()
    }

    @Test
    fun clickMenuHistory() {
        TelemetryWrapper.clickMenuHistory()

    }

    @Test
    fun clickMenuCapture() {
        TelemetryWrapper.clickMenuCapture()

    }

    @Test
    fun showPanelDownload() {
        TelemetryWrapper.showPanelDownload()

    }

    @Test
    fun showPanelHistory() {
        TelemetryWrapper.showPanelHistory()

    }

    @Test
    fun showPanelCapture() {
        TelemetryWrapper.showPanelCapture()

    }

    @Test
    fun menuTurboChangeToTrue() {
        TelemetryWrapper.menuTurboChangeTo(true)
    }

    @Test
    fun menuTurboChangeToFalse() {
        TelemetryWrapper.menuTurboChangeTo(false)
    }

    @Test
    fun menuBlockImageChangeToTrue() {
        TelemetryWrapper.menuBlockImageChangeTo(true)
    }

    @Test
    fun menuBlockImageChangeToFalse() {
        TelemetryWrapper.menuBlockImageChangeTo(false)
    }


    @Test
    fun clickMenuClearCache() {
        TelemetryWrapper.clickMenuClearCache()

    }

    @Test
    fun clickMenuSettings() {
        TelemetryWrapper.clickMenuSettings()
    }

    @Test
    fun clickMenuExit() {
        TelemetryWrapper.clickMenuExit()
    }

    @Test
    fun clickToolbarForward() {
        TelemetryWrapper.clickToolbarForward()

    }

    @Test
    fun clickToolbarReload() {
        TelemetryWrapper.clickToolbarReload()

    }

    @Test
    fun clickToolbarShare() {
        TelemetryWrapper.clickToolbarShare()

    }

    @Test
    fun clickAddToHome() {
        TelemetryWrapper.clickAddToHome()

    }

    @Test
    fun clickToolbarCapture() {
        val sm = ScreenshotManager()
        val categories = sm.getCategories(context).values
        val version = sm.categoryVersion
        categories.forEach {
            TelemetryWrapper.clickToolbarCapture(it, version)
        }
    }

    @Test
    fun clickTopSiteOn() {
        TelemetryWrapper.clickTopSiteOn(0)

    }

    @Test
    fun removeTopSite() {
        TelemetryWrapper.removeTopSite(true)
        TelemetryWrapper.removeTopSite(false)
    }

    @Test
    fun addNewTabFromHome() {
        TelemetryWrapper.addNewTabFromHome()

    }

    @Test
    fun urlBarEvent() {
        TelemetryWrapper.urlBarEvent(true, true)
        TelemetryWrapper.urlBarEvent(true, false)
        TelemetryWrapper.urlBarEvent(false, true)
        TelemetryWrapper.urlBarEvent(false, false)
    }

    @Test
    fun searchSelectEvent() {
        TelemetryWrapper.searchSelectEvent()
    }

    @Test
    fun searchSuggestionLongClick() {
        TelemetryWrapper.searchSuggestionLongClick()
    }

    @Test
    fun searchClear() {
        TelemetryWrapper.searchClear()

    }

    @Test
    fun searchDismiss() {
        TelemetryWrapper.searchDismiss()

    }

    @Test
    fun showSearchBarHome() {
        TelemetryWrapper.showSearchBarHome()

    }

    @Test
    fun clickUrlbar() {
        TelemetryWrapper.clickUrlbar()
    }

    @Test
    fun clickToolbarSearch() {
        TelemetryWrapper.clickToolbarSearch()

    }

    @Test
    fun clickAddTabToolbar() {
        TelemetryWrapper.clickAddTabToolbar()

    }

    @Test
    fun clickAddTabTray() {
        TelemetryWrapper.clickAddTabTray()

    }

    @Test
    fun clickTabFromTabTray() {
        TelemetryWrapper.clickTabFromTabTray()

    }

    @Test
    fun closeTabFromTabTray() {
        TelemetryWrapper.closeTabFromTabTray()

    }

    @Test
    fun downloadRemoveFile() {
        TelemetryWrapper.downloadRemoveFile()

    }

    @Test
    fun downloadDeleteFile() {
        TelemetryWrapper.downloadDeleteFile()

    }

    @Test
    fun downloadOpenFile() {
        TelemetryWrapper.downloadOpenFile(true)
        TelemetryWrapper.downloadOpenFile(false)

    }

    @Test
    fun showFileContextMenu() {
        TelemetryWrapper.showFileContextMenu()

    }

    @Test
    fun historyOpenLink() {
        TelemetryWrapper.historyOpenLink()

    }

    @Test
    fun historyRemoveLink() {
        TelemetryWrapper.historyRemoveLink()

    }

    @Test
    fun showHistoryContextMenu() {
        TelemetryWrapper.showHistoryContextMenu()

    }

    @Test
    fun clearHistory() {
        TelemetryWrapper.clearHistory()

    }

    @Test
    fun openCapture() {
        TelemetryWrapper.openCapture()

    }

    @Test
    fun openCaptureLink() {
        val sm = ScreenshotManager()
        val categories = sm.getCategories(context).values
        val version = sm.categoryVersion
        categories.forEach {
            TelemetryWrapper.openCaptureLink(it, version)
        }
    }

    @Test
    fun editCaptureImage() {
        val sm = ScreenshotManager()
        val categories = sm.getCategories(context).values
        val version = sm.categoryVersion
        categories.forEach {
            TelemetryWrapper.editCaptureImage(true, it, version)
            TelemetryWrapper.editCaptureImage(false, it, version)
        }
    }

    @Test
    fun shareCaptureImage() {
        val sm = ScreenshotManager()
        val categories = sm.getCategories(context).values
        val version = sm.categoryVersion
        categories.forEach {
            TelemetryWrapper.shareCaptureImage(true, it, version)
            TelemetryWrapper.shareCaptureImage(false, it, version)
        }
    }

    @Test
    fun showCaptureInfo() {
        val sm = ScreenshotManager()
        val categories = sm.getCategories(context).values
        val version = sm.categoryVersion
        categories.forEach {
            TelemetryWrapper.showCaptureInfo(it, version)
        }
    }

    @Test
    fun deleteCaptureImage() {
        val sm = ScreenshotManager()
        val categories = sm.getCategories(context).values
        val version = sm.categoryVersion
        categories.forEach {
            TelemetryWrapper.deleteCaptureImage(it, version)
        }
    }

    @Test
    fun feedbackClickEventContextualHint() {
        TelemetryWrapper.feedbackClickEvent(TelemetryWrapper.Value.DISMISS, TelemetryWrapper.Extra_Value.CONTEXTUAL_HINTS)
        TelemetryWrapper.feedbackClickEvent(TelemetryWrapper.Value.POSITIVE, TelemetryWrapper.Extra_Value.CONTEXTUAL_HINTS)
        TelemetryWrapper.feedbackClickEvent(TelemetryWrapper.Value.NEGATIVE, TelemetryWrapper.Extra_Value.CONTEXTUAL_HINTS)
    }

    @Test
    fun feedbackClickEventSetting() {
        TelemetryWrapper.feedbackClickEvent(TelemetryWrapper.Value.DISMISS, TelemetryWrapper.Extra_Value.SETTING)
        TelemetryWrapper.feedbackClickEvent(TelemetryWrapper.Value.POSITIVE, TelemetryWrapper.Extra_Value.SETTING)
        TelemetryWrapper.feedbackClickEvent(TelemetryWrapper.Value.NEGATIVE, TelemetryWrapper.Extra_Value.SETTING)
    }

    @Test
    fun showFeedbackDialog() {
        TelemetryWrapper.showFeedbackDialog()
    }

    @Test
    fun showRateAppNotification() {
        TelemetryWrapper.showRateAppNotification()
    }

    @Test
    fun clickRateAppNotification() {
        TelemetryWrapper.clickRateAppNotification()
    }

    @Test
    fun clickRateAppNotificationPOSITIVE() {
        TelemetryWrapper.clickRateAppNotification(TelemetryWrapper.Value.POSITIVE)
    }

    @Test
    fun clickRateAppNotificationNEGATIVE() {
        TelemetryWrapper.clickRateAppNotification(TelemetryWrapper.Value.NEGATIVE)
    }

    @Test
    fun showDefaultSettingNotification() {
        TelemetryWrapper.showDefaultSettingNotification()
    }

    @Test
    fun clickDefaultSettingNotification() {
        TelemetryWrapper.clickDefaultSettingNotification()
    }

    @Test
    fun onDefaultBrowserServiceFailed() {
        TelemetryWrapper.onDefaultBrowserServiceFailed()
    }

    @Test
    fun promoteShareClickEventSetting() {
        TelemetryWrapper.promoteShareClickEvent(TelemetryWrapper.Value.DISMISS, TelemetryWrapper.Extra_Value.SETTING)
        TelemetryWrapper.promoteShareClickEvent(TelemetryWrapper.Value.SHARE, TelemetryWrapper.Extra_Value.SETTING)
    }

    @Test
    fun promoteShareClickEventSettingContextualHints() {
        TelemetryWrapper.promoteShareClickEvent(TelemetryWrapper.Value.DISMISS, TelemetryWrapper.Extra_Value.CONTEXTUAL_HINTS)
        TelemetryWrapper.promoteShareClickEvent(TelemetryWrapper.Value.SHARE, TelemetryWrapper.Extra_Value.CONTEXTUAL_HINTS)
    }

    @Test
    fun showPromoteShareDialog() {
        TelemetryWrapper.showPromoteShareDialog()
    }

    @Test
    fun validPrefKeyFromWhitelistShouldPass() {

        // call lazyInit() to fill the whitelist
        TelemetryWrapper.EventBuilder.lazyInit()

        // make sure layInit() inits the whitelist successfully.
        assert(FirebaseEvent.getPrefKeyWhitelist().size > 0)

        // whitelist-ed pref key should worked with firebaseEvent and telemetryEvent
        for (validKey in FirebaseEvent.getPrefKeyWhitelist().values) {
            TelemetryWrapper.EventBuilder(TelemetryWrapper.Category.ACTION, TelemetryWrapper.Method.CHANGE, SETTING, validKey)
        }
    }

    @Test
    fun clickWifiFinderSurvey() {
        TelemetryWrapper.clickWifiFinderSurvey()
    }

    @Test
    fun clickWifiFinderSurveyFeedbackPositive() {
        TelemetryWrapper.clickWifiFinderSurveyFeedback(true)
    }

    @Test
    fun clickWifiFinderSurveyFeedbackNegative() {
        TelemetryWrapper.clickWifiFinderSurveyFeedback(false)
    }

    @Test
    fun dismissWifiFinderSurvey() {
        TelemetryWrapper.dismissWifiFinderSurvey()
    }

    @Test
    fun clickVpnSurvey() {
        TelemetryWrapper.clickVpnSurvey()
    }

    @Test
    fun clickVpnSurveyFeedbackPositive() {
        TelemetryWrapper.clickVpnSurveyFeedback(true)
    }

    @Test
    fun clickVpnSurveyFeedbackNegative() {
        TelemetryWrapper.clickVpnSurveyFeedback(false)
    }

    @Test
    fun dismissVpnFinderSurvey() {
        TelemetryWrapper.dismissVpnSurvey()
    }
}
