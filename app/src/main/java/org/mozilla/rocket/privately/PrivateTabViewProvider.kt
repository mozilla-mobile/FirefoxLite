package org.mozilla.rocket.privately

import android.app.Activity
import android.webkit.WebSettings
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.rocket.tabs.TabView
import org.mozilla.rocket.tabs.TabViewProvider

class PrivateTabViewProvider(private val host: Activity) : TabViewProvider() {

    override fun create(): TabView {
        return WebViewProvider.create(host, null, WebViewSettingsHook) as TabView
    }

    object WebViewSettingsHook : WebViewProvider.WebSettingsHook {
        override fun modify(settings: WebSettings?) {
            if (settings == null) {
                return
            }

            settings.setSupportMultipleWindows(false)
        }
    }
}