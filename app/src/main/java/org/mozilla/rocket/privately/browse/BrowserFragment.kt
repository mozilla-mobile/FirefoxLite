package org.mozilla.rocket.privately.browse

import android.Manifest
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.support.v4.content.ContextCompat
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.download.EnqueueDownloadTask
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.menu.WebContextMenu
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.tabs.TabCounter
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.widget.AnimatedProgressBar
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE
import org.mozilla.permissionhandler.PermissionHandle
import org.mozilla.permissionhandler.PermissionHandler
import org.mozilla.rocket.privately.SharedViewModel
import org.mozilla.rocket.tabs.Session
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabView.FullscreenCallback
import org.mozilla.rocket.tabs.TabView.HitTarget
import org.mozilla.rocket.tabs.TabViewEngineSession
import org.mozilla.rocket.tabs.TabsSessionProvider
import org.mozilla.rocket.tabs.utils.TabUtil
import org.mozilla.rocket.tabs.web.Download
import org.mozilla.rocket.tabs.web.DownloadCallback
import org.mozilla.threadutils.ThreadUtils
import org.mozilla.urlutils.UrlUtils

private const val SITE_GLOBE = 0
private const val SITE_LOCK = 1
private const val ACTION_DOWNLOAD = 0

class BrowserFragment : LocaleAwareFragment(),
        ScreenNavigator.BrowserScreen,
        BackKeyHandleable {

    private var listener: FragmentListener? = null

    private lateinit var permissionHandler: PermissionHandler
    private lateinit var clickListener: ClickListener
    private lateinit var sessionManager: SessionManager
    private lateinit var observer: Observer

    private lateinit var browserContainer: ViewGroup
    private lateinit var videoContainer: ViewGroup
    private lateinit var tabViewSlot: ViewGroup
    private lateinit var displayUrlView: TextView
    private lateinit var progressView: AnimatedProgressBar
    private lateinit var siteIdentity: ImageView

    private lateinit var btnLoad: ImageButton
    private lateinit var btnNext: ImageButton
    private lateinit var tabCounter: TabCounter

    private var isLoading: Boolean = false

    private var systemVisibility = ViewUtils.SYSTEM_UI_VISIBILITY_NONE

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        clickListener = ClickListener(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_private_browser, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        val fragmentActivity = activity
        if (fragmentActivity == null) {
            if (BuildConfig.DEBUG) {
                throw RuntimeException("No activity to use")
            }
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

        btnNext = (view.findViewById(R.id.btn_next))
        btnLoad = (view.findViewById(R.id.btn_load))
        tabCounter = view.findViewById(R.id.btn_tab_tray)

        with(clickListener) {
            view.findViewById<View>(R.id.btn_search).setOnClickListener(this)
            view.findViewById<View>(R.id.btn_open_new_tab).setOnClickListener(this)
            btnNext.setOnClickListener(this)
            btnLoad.setOnClickListener(this)
            tabCounter.setOnClickListener(this)
        }

        view.findViewById<View>(R.id.appbar).setOnApplyWindowInsetsListener { v, insets ->
            (v.layoutParams as LinearLayout.LayoutParams).topMargin = insets.systemWindowInsetTop
            insets
        }
        sessionManager = TabsSessionProvider.getOrThrow( activity)
        observer = Observer(this)
        sessionManager.register(observer)
        sessionManager.focusSession?.register(observer)

        activity?.let { registerData(it) }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        activity?.let { unregisterData(it) }

        sessionManager.focusSession?.unregister(observer)
        sessionManager.unregister(observer)
    }

    override fun onResume() {
        super.onResume()
        sessionManager.resume()
    }

    override fun onPause() {
        super.onPause()
        sessionManager.pause()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        permissionHandler = PermissionHandler(object : PermissionHandle {
            override fun doActionDirect(permission: String?, actionId: Int, params: Parcelable?) {

                this@BrowserFragment.context?.also {
                    val download = params as Download

                    if (PackageManager.PERMISSION_GRANTED ==
                            ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        // We do have the permission to write to the external storage. Proceed with the download.
                        queueDownload(download)
                    }
                } ?: run {
                    Log.e("BrowserFragment.kt", "No context to use, abort callback onDownloadStart")
                }
            }

            fun actionDownloadGranted(parcelable: Parcelable?) {
                val download = parcelable as Download
                queueDownload(download)
            }

            override fun doActionGranted(permission: String?, actionId: Int, params: Parcelable?) {
                actionDownloadGranted(params)
            }

            override fun doActionSetting(permission: String?, actionId: Int, params: Parcelable?) {
                actionDownloadGranted(params)
            }

            override fun doActionNoPermission(permission: String?, actionId: Int, params: Parcelable?) {
            }

            override fun makeAskAgainSnackBar(actionId: Int): Snackbar {
                activity?.also {
                    return PermissionHandler.makeAskAgainSnackBar(
                            this@BrowserFragment,
                            it.findViewById(R.id.container),
                            R.string.permission_toast_storage
                    )
                }
                throw IllegalStateException("No Activity to show Snackbar.")
            }

            override fun permissionDeniedToast(actionId: Int) {
                Toast.makeText(getContext(), R.string.permission_toast_storage_deny, Toast.LENGTH_LONG).show()
            }

            override fun requestPermissions(actionId: Int) {
                this@BrowserFragment.requestPermissions(arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), actionId)
            }

            private fun queueDownload(download: Download?) {
                activity?.let { activity ->
                    download?.let {
                        EnqueueDownloadTask(activity, it, displayUrlView.text.toString()).execute()
                    }
                }
            }
        })
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
        val tabView = focus.engineSession?.tabView ?: return false

        if (tabView.canGoBack()) {
            goBack()
            return true
        }

        sessionManager.dropTab(focus.id)
        ScreenNavigator.get(activity).popToHomeScreen(true)
        listener?.onNotified(this, TYPE.DROP_BROWSING_PAGES, null)
        return true
    }

    override fun getFragment(): Fragment {
        return this
    }

    override fun switchToTab(tabId: String?) {
        if (!TextUtils.isEmpty(tabId)) {
            sessionManager.switchToTab(tabId!!)
        }
    }

    override fun goForeground() {
        val tabView = sessionManager.focusSession?.engineSession?.tabView ?: return
        if (tabViewSlot.childCount == 0) {
            tabViewSlot.addView(tabView.view)
        }
    }

    override fun goBackground() {
        val focus = sessionManager.focusSession ?: return
        val tabView = focus.engineSession?.tabView ?: return
        focus.engineSession?.detach()
        tabViewSlot.removeView(tabView.view)
    }

    override fun loadUrl(url: String, openNewTab: Boolean, isFromExternal: Boolean, onViewReadyCallback: Runnable?) {
        if (url.isNotBlank()) {
            displayUrlView.text = url
            if (sessionManager.tabsCount == 0) {
                sessionManager.addTab(url, TabUtil.argument(null, false, true))
            } else {
                sessionManager.focusSession!!.engineSession?.tabView?.loadUrl(url)
            }

            ThreadUtils.postToMainThread(onViewReadyCallback)
        }
    }

    private fun goBack() = sessionManager.focusSession?.engineSession?.goBack()
    private fun goForward() = sessionManager.focusSession?.engineSession?.goForward()
    private fun stop() = sessionManager.focusSession?.engineSession?.stopLoading()
    private fun reload() = sessionManager.focusSession?.engineSession?.reload()

    private fun registerData(activity: FragmentActivity) {
        val shared = ViewModelProviders.of(activity).get(SharedViewModel::class.java)
        shared.getUrl().observe(this, Observer<String> { url ->
            url?.let { loadUrl(it, false, false, null) }
        })
    }

    private fun unregisterData(activity: FragmentActivity) {
        val shared = ViewModelProviders.of(activity).get(SharedViewModel::class.java)
        shared.getUrl().removeObservers(this)
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

    class ClickListener(val fragment: BrowserFragment) : View.OnClickListener {
        val parent: FragmentListener = if (fragment.activity is FragmentListener)
            fragment.activity as FragmentListener
        else throw RuntimeException("")

        override fun onClick(v: View?) {
            when (v?.id) {
                R.id.btn_search -> parent.onNotified(fragment, TYPE.SHOW_URL_INPUT, fragment.displayUrlView.text)
                R.id.btn_open_new_tab -> ScreenNavigator.get(fragment.activity).popToHomeScreen(true)
                R.id.btn_next -> fragment.onNextClicked()
                R.id.btn_tab_tray -> parent.onNotified(fragment, TYPE.SHOW_TAB_TRAY, null)
                R.id.btn_load -> fragment.onLoadClicked()
            }
        }
    }

    class Observer(val fragment: BrowserFragment) : SessionManager.Observer, Session.Observer {
        override fun updateFailingUrl(url: String?, updateFromError: Boolean) {
            // do nothing, exist for interface compatibility only.
        }

        override fun handleExternalUrl(url: String?): Boolean {
            // do nothing, exist for interface compatibility only.
            return false
        }

        override fun onShowFileChooser(es: TabViewEngineSession, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: WebChromeClient.FileChooserParams?): Boolean {
            // do nothing, exist for interface compatibility only.
            return false
        }

        var callback: FullscreenCallback? = null
        var session: Session? = null

        override fun onSessionAdded(session: Session, arguments: Bundle?) {
        }

        override fun onProgress(session: Session, progress: Int) {
            fragment.progressView.progress = progress
        }

        override fun onTitleChanged(session: Session, title: String?) {
            session.let {
                if (!fragment.displayUrlView.text.toString().equals(it.url)) {
                    fragment.displayUrlView.text = it.url
                }
            }
        }

        override fun onLongPress(session: Session, hitTarget: HitTarget) {
            fragment.activity?.let {
                WebContextMenu.show(true,
                        it,
                        PrivateDownloadCallback(fragment, session.url),
                        hitTarget)
            }
        }

        override fun onEnterFullScreen(callback: FullscreenCallback, view: View?) {
            with(fragment) {
                browserContainer.visibility = View.INVISIBLE
                videoContainer.visibility = View.VISIBLE
                videoContainer.addView(view)

                // Switch to immersive mode: Hide system bars other UI controls
                systemVisibility = ViewUtils.switchToImmersiveMode(activity)
            }
        }

        override fun onExitFullScreen() {
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
            session?.engineSession?.tabView?.let { if (it is WebView) it.clearFocus() }
        }

        override fun onUrlChanged(session: Session, url: String?) {
            if (!UrlUtils.isInternalErrorURL(url)) {
                fragment.displayUrlView.text = url
            }
        }

        override fun onLoadingStateChanged(session: Session, loading: Boolean) {
            fragment.isLoading = loading
            if (loading) {
                fragment.btnLoad.setImageResource(R.drawable.ic_close)
            } else {
                val es = fragment.sessionManager.focusSession?.engineSession ?: return
                fragment.btnNext.isEnabled = es.tabView?.canGoForward() ?: false
                fragment.btnLoad.setImageResource(R.drawable.ic_refresh)
            }
        }

        override fun onSecurityChanged(session: Session, isSecure: Boolean) {
            val level = if (isSecure) SITE_LOCK else SITE_GLOBE
            fragment.siteIdentity.setImageLevel(level)
        }

        override fun onSessionCountChanged(count: Int) {
            if (count == 0) {
                session?.unregister(this)
            } else {
                session = fragment.sessionManager.focusSession
                session?.register(this)
            }
        }

        override fun onDownload(session: Session, download: mozilla.components.browser.session.Download): Boolean {
            val activity = fragment.activity
            if (activity == null || !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                return false
            }

            val d = Download(
                    download.url,
                    download.fileName,
                    download.userAgent!!,
                    "",
                    download.contentType!!,
                    download.contentLength!!,
                    false
                    )
            fragment.permissionHandler.tryAction(
                    fragment,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ACTION_DOWNLOAD,
                    d
                    )
            return true
        }
    }

    class PrivateDownloadCallback(val fragment: BrowserFragment, val refererUrl: String?) : DownloadCallback {
        override fun onDownloadStart(download: Download) {
            fragment.activity?.let {
                if (!it.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    return
                }
            }

            fragment.permissionHandler.tryAction(
                    fragment,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ACTION_DOWNLOAD,
                    download
                    )
        }
    }
}