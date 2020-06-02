package org.mozilla.rocket.settings.defaultbrowser.data

import android.content.Context
import org.mozilla.focus.utils.Browsers
import org.mozilla.focus.utils.FirebaseHelper
import org.mozilla.rocket.util.toJsonObject

class DefaultBrowserLocalDataSource(private val applicationContext: Context) : DefaultBrowserDataSource {

    override fun isDefaultBrowser() = Browsers.isDefaultBrowser(applicationContext)

    override fun hasDefaultBrowser() = Browsers.hasDefaultBrowser(applicationContext)

    override fun getTutorialImagesUrl(): TutorialImagesUrl {
        val tutorialImagesUrl = FirebaseHelper.getFirebase().getRcString(RC_KEY_STR_SET_DEFAULT_BROWSER_TUTORIAL_IMAGES)
        return if (tutorialImagesUrl.isNotEmpty()) {
            TutorialImagesUrl(tutorialImagesUrl.toJsonObject())
        } else {
            TutorialImagesUrl("", "", "")
        }
    }

    companion object {
        const val RC_KEY_STR_SET_DEFAULT_BROWSER_TUTORIAL_IMAGES = "str_set_default_browser_tutorial_images"
    }
}