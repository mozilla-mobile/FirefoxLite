package org.mozilla.rocket.settings.defaultbrowser.data

import android.content.Context
import org.mozilla.focus.utils.Browsers

class DefaultBrowserLocalDataSource(private val applicationContext: Context) : DefaultBrowserDataSource {

    override fun isDefaultBrowser() = Browsers.isDefaultBrowser(applicationContext)

    override fun hasDefaultBrowser() = Browsers.hasDefaultBrowser(applicationContext)
}