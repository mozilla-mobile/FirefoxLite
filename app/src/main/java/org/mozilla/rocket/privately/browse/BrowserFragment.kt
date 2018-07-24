package org.mozilla.rocket.privately.browse

import android.app.DownloadManager
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.Intent
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
import android.widget.TextView
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.menu.WebContextMenu
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.UrlUtils
import org.mozilla.focus.widget.AnimatedProgressBar
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE
import org.mozilla.rocket.privately.SharedViewModel
import org.mozilla.rocket.tabs.Tab
import org.mozilla.rocket.tabs.TabView.HitTarget
import org.mozilla.rocket.tabs.TabsSession
import org.mozilla.rocket.tabs.TabsSessionProvider
import org.mozilla.rocket.tabs.utils.DefaultTabsChromeListener
import org.mozilla.rocket.tabs.utils.DefaultTabsViewListener
import org.mozilla.rocket.tabs.utils.TabUtil
import org.mozilla.rocket.tabs.web.Download
import org.mozilla.rocket.tabs.web.DownloadCallback

private const val SITE_GLOBE = 0
private const val SITE_LOCK = 1

class BrowserFragment : LocaleAwareFragment(),
        BackKeyHandleable {

    private var listener: FragmentListener? = null

    private lateinit var tabsSession: TabsSession
    private lateinit var chromeListener: BrowserTabsChromeListener
    private lateinit var viewListener: BrowserTabsViewListener

    private lateinit var tabViewSlot: ViewGroup
    private lateinit var displayUrlView: TextView
    private lateinit var progressView: AnimatedProgressBar
    private lateinit var siteIdentity: ImageView

    private lateinit var btnNext: ImageButton

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
            BuildConfig.DEBUG?.let { throw RuntimeException("No activity to use") }
        } else {
            if (fragmentActivity is TabsSessionProvider.SessionHost) {
                tabsSession = fragmentActivity.tabsSession
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

        tabViewSlot = view.findViewById(R.id.tab_view_slot)
        progressView = view.findViewById(R.id.progress)

        view.findViewById<View>(R.id.btn_mode).setOnClickListener { onModeClicked() }
        view.findViewById<View>(R.id.btn_search).setOnClickListener { onSearchClicked() }
        view.findViewById<View>(R.id.btn_refresh).setOnClickListener { onRefreshClicked() }
        view.findViewById<View>(R.id.btn_share).setOnClickListener { onShareClicked() }

        btnNext = (view.findViewById<View>(R.id.btn_next) as ImageButton)
                .also {
                    it.isEnabled = false
                    it.setOnClickListener { onNextClicked() }
                }
    }

    override fun onResume() {
        super.onResume()
        tabsSession.resume()
        tabsSession.addTabsChromeListener(chromeListener)
        tabsSession.addTabsViewListener(viewListener)
    }

    override fun onPause() {
        super.onPause()
        tabsSession.removeTabsViewListener(viewListener)
        tabsSession.removeTabsChromeListener(chromeListener)
        tabsSession.pause()
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
        val focus = tabsSession.focusTab ?: return false
        val tabView = focus.tabView ?: return false

        if (tabView.canGoBack()) {
            goBack()
            return true
        }

        tabsSession.dropTab(focus.id)
        return false
    }

    fun loadUrl(url: String?) {
        url?.let {
            if (it.isNotBlank()) {
                displayUrlView.text = url
                if (tabsSession.tabsCount == 0) {
                    tabsSession.addTab(url, TabUtil.argument(null, false, true))
                } else {
                    tabsSession.focusTab!!.tabView!!.loadUrl(url)
                }
            }
        }
    }

    private fun goBack() {
        val focus = tabsSession.focusTab ?: return
        val tabView = focus.tabView ?: return
        tabView.goBack()
    }

    private fun goForward() {
        val focus = tabsSession.focusTab ?: return
        val tabView = focus.tabView ?: return
        tabView.goForward()
    }

    private fun reload() {
        val focus = tabsSession.focusTab ?: return
        val tabView = focus.tabView ?: return
        tabView.reload()
    }

    private fun registerData(activity: FragmentActivity) {
        val shared = ViewModelProviders.of(activity).get(SharedViewModel::class.java)

        shared.getUrl().observe(this, Observer<String> { url -> loadUrl(url) })
    }

    private fun onModeClicked() {
        val listener = activity as FragmentListener
        listener.onNotified(BrowserFragment@ this, TYPE.TOGGLE_PRIVATE_MODE, null)
        TelemetryWrapper.togglePrivateMode(false)
    }

    private fun onNextClicked() {
        goForward()
    }

    private fun onSearchClicked() {
        val listener = activity as FragmentListener
        listener.onNotified(BrowserFragment@ this, TYPE.SHOW_URL_INPUT, displayUrlView.text)
    }

    private fun onRefreshClicked() {
        reload()
    }

    private fun onShareClicked() {
        val url = tabsSession.focusTab?.url ?: return
        if (UrlUtils.isInternalErrorURL(url)) {
            return
        }

        Intent().also {
            it.action = Intent.ACTION_SEND
            it.type = "text/plain"
            it.putExtra(Intent.EXTRA_TEXT, url)
        }.let {
            startActivity(Intent.createChooser(it, getString(R.string.share_dialog_title)))
        }
    }

    class BrowserTabsChromeListener(val fragment: BrowserFragment) : DefaultTabsChromeListener() {
        override fun onTabAdded(tab: Tab, arguments: Bundle?) {
            super.onTabAdded(tab, arguments)
            fragment.tabViewSlot.addView(tab.tabView!!.view)
        }

        override fun onProgressChanged(tab: Tab, progress: Int) {
            super.onProgressChanged(tab, progress)
            fragment.progressView.progress = progress
        }

        override fun onReceivedTitle(tab: Tab, title: String?) {
            if (!fragment.displayUrlView.text.toString().equals(tab.getUrl())) {
                fragment.displayUrlView.text = tab.url
            }
        }

        override fun onLongPress(tab: Tab, hitTarget: HitTarget?) {
            val activity = fragment.activity
            if (activity != null && hitTarget != null) {
                WebContextMenu.show(true,
                        activity,
                        PrivateDownloadCallback(activity, tab.url),
                        hitTarget)
            }
        }
    }

    class BrowserTabsViewListener(val fragment: BrowserFragment) : DefaultTabsViewListener() {
        override fun onURLChanged(tab: Tab, url: String?) {
            if (!UrlUtils.isInternalErrorURL(url)) {
                fragment.displayUrlView.text = url
            }
        }

        override fun onTabStarted(tab: Tab) {
            super.onTabStarted(tab)
            fragment.siteIdentity.setImageLevel(SITE_GLOBE)
        }

        override fun onTabFinished(tab: Tab, isSecure: Boolean) {
            super.onTabFinished(tab, isSecure)
            val focus = fragment.tabsSession.focusTab ?: return
            val tabView = focus.tabView ?: return
            fragment.btnNext.isEnabled = tabView.canGoForward()

            val level = if (isSecure) SITE_LOCK else SITE_GLOBE
            fragment.siteIdentity.setImageLevel(level)
        }
    }

    class PrivateDownloadCallback(val context: Context, val refererUrl: String) : DownloadCallback {
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