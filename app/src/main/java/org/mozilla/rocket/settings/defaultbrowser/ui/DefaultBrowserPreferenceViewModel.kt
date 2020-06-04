package org.mozilla.rocket.settings.defaultbrowser.ui

import android.os.Build
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.rocket.download.SingleLiveEvent
import org.mozilla.rocket.settings.defaultbrowser.data.DefaultBrowserRepository

class DefaultBrowserPreferenceViewModel(private val defaultBrowserRepository: DefaultBrowserRepository) : ViewModel() {

    private val _uiModel = MutableLiveData<DefaultBrowserPreferenceUiModel>()
    val uiModel: LiveData<DefaultBrowserPreferenceUiModel>
        get() = _uiModel

    val openDefaultAppsSettings = SingleLiveEvent<Unit>()
    val openAppDetailSettings = SingleLiveEvent<Unit>()
    val openSumoPage = SingleLiveEvent<Unit>()
    val triggerWebOpen = SingleLiveEvent<Unit>()

    val openDefaultAppsSettingsTutorialDialog = SingleLiveEvent<Unit>()
    val openUrlTutorialDialog = SingleLiveEvent<Unit>()

    val successToSetDefaultBrowser = SingleLiveEvent<Unit>()
    val failToSetDefaultBrowser = SingleLiveEvent<Unit>()

    private var isDefaultBrowser: Boolean = false
    private var hasDefaultBrowser: Boolean = false

    private var tryToSetDefaultBrowser: Boolean
        get() = defaultBrowserRepository.hasSetDefaultBrowserInProgress()
        set(value) {
            defaultBrowserRepository.setDefaultBrowserInProgress(value)
        }

    fun refreshSettings() {
        isDefaultBrowser = defaultBrowserRepository.isDefaultBrowser()
        hasDefaultBrowser = defaultBrowserRepository.hasDefaultBrowser()
        val tutorialImagesUrl = defaultBrowserRepository.getTutorialImagesUrl()

        _uiModel.value = DefaultBrowserPreferenceUiModel(
            isDefaultBrowser,
            hasDefaultBrowser,
            tutorialImagesUrl.flow1TutorialStep1ImageUrl,
            tutorialImagesUrl.flow1TutorialStep2ImageUrl,
            tutorialImagesUrl.flow2TutorialStep2ImageUrl
        )
    }

    fun performAction() {
        performSettingDefaultBrowserAction()
    }

    fun performActionFromNotification() {
        if (!isDefaultBrowser) {
            performSettingDefaultBrowserAction()
        }
    }

    private fun performSettingDefaultBrowserAction() {
        when {
            isDefaultBrowser -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    openDefaultAppsSettings.call()
                } else {
                    openAppDetailSettings.call()
                }
            }
            hasDefaultBrowser -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    openDefaultAppsSettingsTutorialDialog.call()
                    TelemetryWrapper.showSetDefaultBySettingsMessage()
                } else {
                    // TODO: Change to the flow #4 in SPEC
                    openSumoPage.call()
                }
            }
            else -> {
                openUrlTutorialDialog.call()
                TelemetryWrapper.showSetDefaultByLinkMessage()
            }
        }
    }

    fun onResume() {
        refreshSettings()
        if (tryToSetDefaultBrowser) {
            if (isDefaultBrowser) {
                successToSetDefaultBrowser.call()
                TelemetryWrapper.showSetDefaultSuccessToast()
            } else {
                failToSetDefaultBrowser.call()
                TelemetryWrapper.showSetDefaultTryAgainSnackbar()
            }
            tryToSetDefaultBrowser = false
        }
    }

    fun onPause() {
    }

    fun clickGoToSystemDefaultAppsSettings() {
        tryToSetDefaultBrowser = true
        openDefaultAppsSettings.call()
        TelemetryWrapper.clickSetDefaultBySettingsMessage(TelemetryWrapper.Extra_Value.OK)
    }

    fun cancelGoToSystemDefaultAppsSettings() {
        TelemetryWrapper.clickSetDefaultBySettingsMessage(TelemetryWrapper.Extra_Value.CANCEL)
    }

    fun clickOpenUrl() {
        tryToSetDefaultBrowser = true
        triggerWebOpen.call()
        TelemetryWrapper.clickSetDefaultByLinkMessage(TelemetryWrapper.Extra_Value.OK)
    }

    fun cancelOpenUrl() {
        TelemetryWrapper.clickSetDefaultByLinkMessage(TelemetryWrapper.Extra_Value.CANCEL)
    }

    data class DefaultBrowserPreferenceUiModel(
        val isDefaultBrowser: Boolean,
        val hasDefaultBrowser: Boolean,
        val flow1TutorialStep1ImageUrl: String,
        val flow1TutorialStep2ImageUrl: String,
        val flow2TutorialStep2ImageUrl: String
    )
}
