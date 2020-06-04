package org.mozilla.rocket.settings.defaultbrowser.data

import android.content.Context
import android.preference.PreferenceManager
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.util.toJsonObject
import org.mozilla.strictmodeviolator.StrictModeViolation

class DefaultBrowserLocalDataSource(private val applicationContext: Context) : DefaultBrowserDataSource {

    private val preference by lazy {
        StrictModeViolation.tempGrant({ builder ->
            builder.permitDiskReads()
        }, {
            PreferenceManager.getDefaultSharedPreferences(applicationContext)
        })
    }

    override fun isDefaultBrowser() = Browsers.isDefaultBrowser(applicationContext)

    override fun hasDefaultBrowser() = Browsers.hasDefaultBrowser(applicationContext)

    override fun hasSetDefaultBrowserInProgress() =
        preference.getBoolean(KEY_SET_DEFAULT_BROWSER_IN_PROGRESS, false)

    override fun setDefaultBrowserInProgress(isInProgress: Boolean) {
        preference.edit().putBoolean(KEY_SET_DEFAULT_BROWSER_IN_PROGRESS, isInProgress).apply()
    }

    override fun getTutorialImagesUrl(): TutorialImagesUrl {
        val tutorialImagesUrl = FirebaseHelper.getFirebase().getRcString(RC_KEY_STR_SET_DEFAULT_BROWSER_TUTORIAL_IMAGES)
        return if (tutorialImagesUrl.isNotEmpty()) {
            TutorialImagesUrl(tutorialImagesUrl.toJsonObject())
        } else {
            TutorialImagesUrl("", "", "")
        }
    }

    companion object {
        const val KEY_SET_DEFAULT_BROWSER_IN_PROGRESS = "set_default_browser_in_progress"
        const val RC_KEY_STR_SET_DEFAULT_BROWSER_TUTORIAL_IMAGES = "str_set_default_browser_tutorial_images"
    }
}
