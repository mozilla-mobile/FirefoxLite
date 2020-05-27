package org.mozilla.rocket.settings.defaultbrowser.data

class DefaultBrowserRepository(private val defaultBrowserDataSource: DefaultBrowserDataSource) {

    fun isDefaultBrowser() = defaultBrowserDataSource.isDefaultBrowser()

    fun hasDefaultBrowser() = defaultBrowserDataSource.hasDefaultBrowser()
}