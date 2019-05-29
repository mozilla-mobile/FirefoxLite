package org.mozilla.rocket.privately

import android.app.Activity
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebViewDatabase
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.rocket.tabs.TabView
import org.mozilla.rocket.tabs.TabViewProvider
import org.mozilla.strictmodeviolator.StrictModeViolation

class PrivateTabViewProvider(private val host: Activity) : TabViewProvider() {

    override fun create(): TabView {
        return WebViewProvider.create(host, null, WebViewSettingsHook) as TabView
    }

    @Suppress("DEPRECATION")
    override fun purify(context: Context?) {
        StrictModeViolation.tempGrant({ it.permitDiskWrites().permitDiskReads() }) {
            CookieManager.getInstance().removeAllCookies(null)
            WebStorage.getInstance().deleteAllData()
            val webViewDatabase = WebViewDatabase.getInstance(context)
            webViewDatabase.clearFormData()
            webViewDatabase.clearHttpAuthUsernamePassword()
        }
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