package org.mozilla.rocket.privately.browse

import android.app.DownloadManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.menu.WebContextMenu
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.widget.AnimatedProgressBar
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE
import org.mozilla.rocket.privately.SharedViewModel
import org.mozilla.rocket.tabs.Session
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabView.FullscreenCallback
import org.mozilla.rocket.tabs.TabView.HitTarget
import org.mozilla.rocket.tabs.TabsSessionProvider
import org.mozilla.rocket.tabs.utils.DefaultTabsChromeListener
import org.mozilla.rocket.tabs.utils.DefaultTabsViewListener
import org.mozilla.rocket.tabs.utils.TabUtil
import org.mozilla.rocket.tabs.web.Download
import org.mozilla.rocket.tabs.web.DownloadCallback
import org.mozilla.urlutils.UrlUtils

private const val SITE_GLOBE = 0
private const val SITE_LOCK = 1

class BrowserFragment : LocaleAwareFragment(),
        BackKeyHandleable {

    private var listener: FragmentListener? = null

    private lateinit var sessionManager: SessionManager
    private lateinit var chromeListener: BrowserTabsChromeListener
    private lateinit var viewListener: BrowserTabsViewListener

    private lateinit var browserContainer: ViewGroup
    private lateinit var videoContainer: ViewGroup
    private lateinit var tabViewSlot: ViewGroup
    private lateinit var displayUrlView: TextView
    private lateinit var progressView: AnimatedProgressBar
    private lateinit var siteIdentity: ImageView

    private lateinit var btnLoad: ImageButton
    private lateinit var btnNext: ImageButton

    private var isLoading: Boolean = false

    private var systemVisibility = ViewUtils.SYSTEM_UI_VISIBILITY_NONE

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_private_browser, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val fragmentActivity = activity
        if (fragmentActivity == null) {
            BuildConfig.DEBUG.let { throw RuntimeException("No activity to use") }
        } else {
            if (fragmentActivity is TabsSessionProvider.SessionHost) {
                sessionManager = fragmentActivity.sessionManager
                chromeListener = BrowserTabsChromeListener(this)
                viewListener = BrowserTabsViewListener(this)
            }

            registerData(fragmentActivity)
        }
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        displayUrlView = view.findViewById(R.id.display_url)
        displayUrlView.setOnClickListener { onSearchClicked() }

        siteIdentity = view.findViewById(R.id.site_identity)

        browserContainer = view.findViewById(R.id.browser_container)
        videoContainer = view.findViewById(R.id.video_container)
        tabViewSlot = view.findViewById(R.id.tab_view_slot)
        progressView = view.findViewById(R.id.progress)

        view.findViewById<View>(R.id.btn_mode).setOnClickListener { onModeClicked() }
        view.findViewById<View>(R.id.btn_search).setOnClickListener { onSearchClicked() }
        view.findViewById<View>(R.id.btn_delete).setOnClickListener { onDeleteClicked() }

        btnLoad = (view.findViewById<ImageButton>(R.id.btn_load))
                .also { it.setOnClickListener { onLoadClicked() } }

        btnNext = (view.findViewById<View>(R.id.btn_next) as ImageButton)
                .also {
                    it.isEnabled = false
                    it.setOnClickListener { onNextClicked() }
                }

        view.findViewById<View>(R.id.appbar).setOnApplyWindowInsetsListener { v, insets ->
            (v.layoutParams as LinearLayout.LayoutParams).topMargin = insets.systemWindowInsetTop
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        sessionManager.resume()
        sessionManager.addTabsChromeListener(chromeListener)
        sessionManager.addTabsViewListener(viewListener)
    }

    override fun onPause() {
        super.onPause()
        sessionManager.removeTabsViewListener(viewListener)
        sessionManager.removeTabsChromeListener(chromeListener)
        sessionManager.pause()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is FragmentListener) {
            listener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun applyLocale() {
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See issue #666.
        val unneeded = WebView(getContext())
        unneeded.destroy()
    }

    override fun onBackPressed(): Boolean {
        val focus = sessionManager.focusSession ?: return false
        val tabView = focus.tabView ?: return false

        if (tabView.canGoBack()) {
            goBack()
            return true
        }

        sessionManager.dropTab(focus.id)
        return false
    }

    fun loadUrl(url: String?) {
        url?.let {
            if (it.isNotBlank()) {
                displayUrlView.text = url
                if (sessionManager.tabsCount == 0) {
                    sessionManager.addTab(url, TabUtil.argument(null, false, true))
                } else {
                    sessionManager.focusSession!!.tabView!!.loadUrl(url)
                }
            }
        }
    }

    private fun goBack() {
        val focus = sessionManager.focusSession ?: return
        val tabView = focus.tabView ?: return
        tabView.goBack()
    }

    private fun goForward() {
        val focus = sessionManager.focusSession ?: return
        val tabView = focus.tabView ?: return
        tabView.goForward()
    }

    private fun stop() {
        val focus = sessionManager.focusSession ?: return
        val tabView = focus.tabView ?: return
        tabView.stopLoading()
    }

    private fun reload() {
        val focus = sessionManager.focusSession ?: return
        val tabView = focus.tabView ?: return
        tabView.reload()
    }

    private fun registerData(activity: FragmentActivity) {
        val shared = ViewModelProviders.of(activity).get(SharedViewModel::class.java)

        shared.getUrl().observe(this, Observer<String> { url -> loadUrl(url) })
    }

    private fun onModeClicked() {
        val listener = activity as FragmentListener
        listener.onNotified(this, TYPE.TOGGLE_PRIVATE_MODE, null)
        TelemetryWrapper.togglePrivateMode(false)
    }

    private fun onNextClicked() {
        goForward()
    }

    private fun onSearchClicked() {
        val listener = activity as FragmentListener
        listener.onNotified(this, TYPE.SHOW_URL_INPUT, displayUrlView.text)
    }

    private fun onLoadClicked() {
        when (isLoading) {
            true -> stop()
            else -> reload()
        }
    }

    private fun onDeleteClicked() {
        val listener = activity as FragmentListener
        for (tab in sessionManager.getTabs()) {
            sessionManager.dropTab(tab.id)
        }
        listener.onNotified(this, TYPE.DROP_BROWSING_PAGES, null)
    }

    class BrowserTabsChromeListener(val fragment: BrowserFragment) : DefaultTabsChromeListener() {

        var callback: FullscreenCallback? = null

        override fun onTabAdded(session: Session, arguments: Bundle?) {
            super.onTabAdded(session, arguments)
            fragment.tabViewSlot.addView(session.tabView!!.view)
        }

        override fun onProgressChanged(session: Session, progress: Int) {
            super.onProgressChanged(session, progress)
            fragment.progressView.progress = progress
        }

        override fun onReceivedTitle(session: Session, title: String?) {
            if (!fragment.displayUrlView.text.toString().equals(session.url)) {
                fragment.displayUrlView.text = session.url
            }
        }

        override fun onLongPress(session: Session, hitTarget: HitTarget?) {
            val activity = fragment.activity
            if (activity != null && hitTarget != null) {
                WebContextMenu.show(true,
                        activity,
                        PrivateDownloadCallback(activity, session.url),
                        hitTarget)
            }
        }

        override fun onEnterFullScreen(session: Session, callback: FullscreenCallback, fullscreenContent: View?) {
            with(fragment) {
                browserContainer.visibility = View.INVISIBLE
                videoContainer.visibility = View.VISIBLE
                videoContainer.addView(fullscreenContent)

                // Switch to immersive mode: Hide system bars other UI controls
                systemVisibility = ViewUtils.switchToImmersiveMode(activity)
            }
        }

        override fun onExitFullScreen(session: Session) {
            with(fragment) {
                browserContainer.visibility = View.VISIBLE
                videoContainer.visibility = View.INVISIBLE
                videoContainer.removeAllViews()

                if (systemVisibility != ViewUtils.SYSTEM_UI_VISIBILITY_NONE) {
                    ViewUtils.exitImmersiveMode(systemVisibility, activity)
                }
            }

            callback?.let { it.fullScreenExited() }
            callback = null

            // WebView gets focus, but unable to open the keyboard after exit Fullscreen for Android 7.0+
            // We guess some component in WebView might lock focus
            // So when user touches the input text box on Webview, it will not trigger to open the keyboard
            // It may be a WebView bug.
            // The workaround is clearing WebView focus
            // The WebView will be normal when it gets focus again.
            // If android change behavior after, can remove this.
            session.tabView?.let { if (it is WebView) it.clearFocus() }
        }
    }

    class BrowserTabsViewListener(val fragment: BrowserFragment) : DefaultTabsViewListener() {
        override fun onURLChanged(session: Session, url: String?) {
            if (!UrlUtils.isInternalErrorURL(url)) {
                fragment.displayUrlView.text = url
            }
        }

        override fun onTabStarted(session: Session) {
            super.onTabStarted(session)
            fragment.siteIdentity.setImageLevel(SITE_GLOBE)
            fragment.isLoading = true
            fragment.btnLoad.setImageResource(R.drawable.ic_close)
        }

        override fun onTabFinished(session: Session, isSecure: Boolean) {
            super.onTabFinished(session, isSecure)
            val focus = fragment.sessionManager.focusSession ?: return
            val tabView = focus.tabView ?: return
            fragment.btnNext.isEnabled = tabView.canGoForward()

            val level = if (isSecure) SITE_LOCK else SITE_GLOBE
            fragment.siteIdentity.setImageLevel(level)
            fragment.isLoading = false
            fragment.btnLoad.setImageResource(R.drawable.ic_refresh)
        }
    }

    class PrivateDownloadCallback(val context: Context, val refererUrl: String?) : DownloadCallback {
        override fun onDownloadStart(download: Download) {
            if (!TextUtils.isEmpty(download.url)) {
                val cookie = CookieManager.getInstance().getCookie(download.getUrl());
                val request = DownloadManager.Request(Uri.parse(download.url))
                        .addRequestHeader("User-Agent", download.getUserAgent())
                        .addRequestHeader("Cookie", cookie)
                        .addRequestHeader("Referer", refererUrl)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setMimeType(download.getMimeType())

                val mgr = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                mgr.enqueue(request)
            }
        }
    }
}