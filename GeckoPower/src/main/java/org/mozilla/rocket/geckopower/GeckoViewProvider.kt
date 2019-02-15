package org.mozilla.rocket.geckopower

import android.content.Context
import android.os.Bundle
import android.os.Message
import android.util.AttributeSet
import android.view.View
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.rocket.tabs.SiteIdentity
import org.mozilla.rocket.tabs.TabChromeClient
import org.mozilla.rocket.tabs.TabView
import org.mozilla.rocket.tabs.TabViewClient
import org.mozilla.rocket.tabs.web.DownloadCallback
import org.mozilla.rocket.util.IGeckoViewProvider

class GeckoViewProvider : IGeckoViewProvider {

    // require Activity context
    override fun create(ctx: Context): TabView {

        // Create default settings (optional) and enable tracking protection for all future sessions.
//        val defaultSettings = DefaultSettings().apply {
//            trackingProtectionPolicy = EngineSession.TrackingProtectionPolicy.all()
//        }

        val runtime = GeckoRuntime.create(ctx)

        val geckoWebView = GeckoWebView(ctx, null)
//        val engine = GeckoEngine(ctx, defaultSettings, runtime)
        val session = GeckoSession()

        session.open(runtime)
        geckoWebView.session = session
        session.loadUri("about:buildconfig") // Or any other URL...
        return geckoWebView
    }
}

class GeckoWebView(context: Context?, attrs: AttributeSet?) : GeckoView(context, attrs),
    TabView {
    override fun setContentBlockingEnabled(enabled: Boolean) {
    }

    override fun bindOnNewWindowCreation(msg: Message) {
    }

    override fun setImageBlockingEnabled(enabled: Boolean) {
    }

    override fun isBlockingEnabled(): Boolean {
        return false
    }

    override fun performExitFullScreen() {
    }

    override fun setViewClient(viewClient: TabViewClient?) {
    }

    override fun setChromeClient(chromeClient: TabChromeClient?) {
    }

    override fun setDownloadCallback(callback: DownloadCallback?) {
    }

    override fun setFindListener(callback: TabView.FindListener?) {
    }

    override fun onPause() {
    }

    override fun onResume() {
    }

    override fun destroy() {
    }

    override fun reload() {
    }

    override fun stopLoading() {
    }

    override fun getUrl(): String {
        return ""
    }

    override fun getTitle(): String {
        return ""
    }

    override fun getSecurityState(): Int {
        return SiteIdentity.SECURE
    }

    override fun loadUrl(url: String) {
        this.session.loadUri(url)
    }

    override fun cleanup() {
    }

    override fun goForward() {
    }

    override fun goBack() {
    }

    override fun canGoForward(): Boolean {
        return true
    }

    override fun canGoBack(): Boolean {
        return true
    }

    override fun restoreViewState(inState: Bundle?) {
    }

    override fun saveViewState(outState: Bundle?) {
    }

    override fun insertBrowsingHistory() {
    }

    override fun getView(): View {
        return this
    }
}