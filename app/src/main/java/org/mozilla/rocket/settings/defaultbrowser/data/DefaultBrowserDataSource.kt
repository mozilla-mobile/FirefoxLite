package org.mozilla.rocket.settings.defaultbrowser.data

interface DefaultBrowserDataSource {
    fun isDefaultBrowser(): Boolean
    fun hasDefaultBrowser(): Boolean
    fun hasSetDefaultBrowserInProgress(): Boolean
    fun setDefaultBrowserInProgress(isInProgress: Boolean)
    fun getTutorialImagesUrl(): TutorialImagesUrl
}