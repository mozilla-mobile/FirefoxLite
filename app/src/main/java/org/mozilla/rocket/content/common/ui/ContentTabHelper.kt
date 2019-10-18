package org.mozilla.rocket.content.common.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import com.google.android.material.snackbar.Snackbar
import org.mozilla.focus.R
import org.mozilla.focus.download.EnqueueDownloadTask
import org.mozilla.focus.menu.WebContextMenu
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.web.HttpAuthenticationDialogBuilder
import org.mozilla.permissionhandler.PermissionHandle
import org.mozilla.permissionhandler.PermissionHandler
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.tabs.Session
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabView
import org.mozilla.rocket.tabs.TabViewClient
import org.mozilla.rocket.tabs.TabViewEngineSession
import org.mozilla.rocket.tabs.web.Download
import org.mozilla.urlutils.UrlUtils

private const val SITE_GLOBE = 0
private const val SITE_LOCK = 1
private const val ACTION_DOWNLOAD = 0

interface ContentTabViewContract {
    fun getHostActivity(): AppCompatActivity?
    fun getCurrentSession(): Session?
    fun getChromeViewModel(): ChromeViewModel
    fun getSiteIdentity(): ImageView?
    fun getDisplayUrlView(): TextView?
    fun getProgressBar(): ProgressBar?
    fun getFullScreenGoneViews(): List<View>
    fun getFullScreenInvisibleViews(): List<View>
    fun getFullScreenContainerView(): ViewGroup
}

class ContentTabHelper(private val contentTabViewContract: ContentTabViewContract) {
    private lateinit var permissionHandler: PermissionHandler

    fun initPermissionHandler() {
        permissionHandler = PermissionHandler(object : PermissionHandle {
            override fun doActionDirect(permission: String?, actionId: Int, params: Parcelable?) {
                contentTabViewContract.getHostActivity()?.let {
                    val download = params as Download

                    if (PackageManager.PERMISSION_GRANTED ==
                        ContextCompat.checkSelfPermission(it, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    ) {
                        // We do have the permission to write to the external storage. Proceed with the download.
                        queueDownload(download)
                    }
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

            override fun doActionNoPermission(
                permission: String?,
                actionId: Int,
                params: Parcelable?
            ) {
            }

            override fun makeAskAgainSnackBar(actionId: Int): Snackbar {
                val snackBarContainer: View? = contentTabViewContract.getHostActivity()?.findViewById(R.id.snack_bar_container)
                if (snackBarContainer != null) {
                    return PermissionHandler.makeAskAgainSnackBar(
                        contentTabViewContract.getHostActivity(),
                        snackBarContainer,
                        R.string.permission_toast_storage
                    )
                }

                throw IllegalStateException("No container to show Snackbar.")
            }

            override fun permissionDeniedToast(actionId: Int) {
                contentTabViewContract.getHostActivity()?.let {
                    Toast.makeText(it, R.string.permission_toast_storage_deny, Toast.LENGTH_LONG).show()
                }
            }

            override fun requestPermissions(actionId: Int) {
                contentTabViewContract.getHostActivity()?.let {
                    ActivityCompat.requestPermissions(it, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), actionId)
                }
            }

            private fun queueDownload(download: Download?) {
                contentTabViewContract.getHostActivity()?.let { activity ->
                    download?.let {
                        EnqueueDownloadTask(activity, it, contentTabViewContract.getDisplayUrlView()?.text.toString()).execute()
                    }
                }
            }
        })
    }

    fun getObserver(): Observer {
        return Observer(contentTabViewContract, permissionHandler)
    }

    class Observer(
        private val contentTabViewContract: ContentTabViewContract,
        private val permissionHandler: PermissionHandler
    ) : SessionManager.Observer, Session.Observer {

        private var systemVisibility = ViewUtils.SYSTEM_UI_VISIBILITY_NONE

        override fun updateFailingUrl(url: String?, updateFromError: Boolean) {
            // do nothing, exist for interface compatibility only.
        }

        override fun handleExternalUrl(url: String?): Boolean {
            // Block links to launch external apps
            return true
        }

        override fun onShowFileChooser(
            es: TabViewEngineSession,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: WebChromeClient.FileChooserParams?
        ): Boolean {
            // do nothing, exist for interface compatibility only.
            return false
        }

        var callback: TabView.FullscreenCallback? = null
        var session: Session? = null

        override fun onSessionAdded(session: Session, arguments: Bundle?) {
        }

        override fun onProgress(session: Session, progress: Int) {
            contentTabViewContract.getProgressBar()?.progress = progress
        }

        override fun onTitleChanged(session: Session, title: String?) {
            contentTabViewContract.getChromeViewModel().onFocusedTitleChanged(title)
            contentTabViewContract.getDisplayUrlView()?.let { displayUrlView ->
                session.let {
                    if (displayUrlView.text.toString() != it.url) {
                        displayUrlView.text = it.url
                    }
                }
            }
        }

        override fun onLongPress(session: Session, hitTarget: TabView.HitTarget) {
            contentTabViewContract.getHostActivity()?.let {
                WebContextMenu.show(true,
                    it,
                    DownloadCallback(it, permissionHandler),
                    hitTarget)
            }
        }

        override fun onEnterFullScreen(callback: TabView.FullscreenCallback, view: View?) {
            for (goneView in contentTabViewContract.getFullScreenGoneViews()) {
                goneView.visibility = View.GONE
            }
            for (invisibleView in contentTabViewContract.getFullScreenInvisibleViews()) {
                invisibleView.visibility = View.INVISIBLE
            }
            contentTabViewContract.getFullScreenContainerView().visibility = View.VISIBLE
            contentTabViewContract.getFullScreenContainerView().addView(view)

            // Switch to immersive mode: Hide system bars other UI controls
            systemVisibility = ViewUtils.switchToImmersiveMode(contentTabViewContract.getHostActivity())
        }

        override fun onExitFullScreen() {
            for (goneView in contentTabViewContract.getFullScreenGoneViews()) {
                goneView.visibility = View.VISIBLE
            }
            for (invisibleView in contentTabViewContract.getFullScreenInvisibleViews()) {
                invisibleView.visibility = View.VISIBLE
            }
            contentTabViewContract.getFullScreenContainerView().visibility = View.GONE
            contentTabViewContract.getFullScreenContainerView().removeAllViews()

            if (systemVisibility != ViewUtils.SYSTEM_UI_VISIBILITY_NONE) {
                ViewUtils.exitImmersiveMode(systemVisibility, contentTabViewContract.getHostActivity())
            }

            callback?.fullScreenExited()
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
            contentTabViewContract.getChromeViewModel().onFocusedUrlChanged(url)
            if (!UrlUtils.isInternalErrorURL(url)) {
                contentTabViewContract.getDisplayUrlView()?.text = url
            }
        }

        override fun onLoadingStateChanged(session: Session, loading: Boolean) {
            if (loading) {
                contentTabViewContract.getChromeViewModel().onPageLoadingStarted()
            } else {
                contentTabViewContract.getChromeViewModel().onPageLoadingStopped()
            }
        }

        override fun onSecurityChanged(session: Session, isSecure: Boolean) {
            val level = if (isSecure) SITE_LOCK else SITE_GLOBE
            contentTabViewContract.getSiteIdentity()?.setImageLevel(level)
        }

        override fun onSessionCountChanged(count: Int) {
            contentTabViewContract.getChromeViewModel().onTabCountChanged(count)
            if (count == 0) {
                session?.unregister(this)
            } else {
                session = contentTabViewContract.getCurrentSession()
                session?.register(this)
            }
        }

        override fun onDownload(
            session: Session,
            download: mozilla.components.browser.session.Download
        ): Boolean {
            contentTabViewContract.getHostActivity()?.let { activity ->
                if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    return false
                }

                val d = Download(
                    download.url,
                    download.fileName,
                    download.userAgent,
                    "",
                    download.contentType,
                    download.contentLength!!,
                    false
                )
                permissionHandler.tryAction(
                    activity,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    ACTION_DOWNLOAD,
                    d
                )
                return true
            }

            return false
        }

        override fun onHttpAuthRequest(
            callback: TabViewClient.HttpAuthCallback,
            host: String?,
            realm: String?
        ) {
            val builder = HttpAuthenticationDialogBuilder.Builder(contentTabViewContract.getHostActivity(), host, realm)
                .setOkListener { _, _, username, password -> callback.proceed(username, password) }
                .setCancelListener { callback.cancel() }
                .build()

            builder.createDialog()
            builder.show()
        }

        override fun onNavigationStateChanged(session: Session, canGoBack: Boolean, canGoForward: Boolean) {
            contentTabViewContract.getChromeViewModel().onNavigationStateChanged(canGoBack, canGoForward)
        }

        override fun onFocusChanged(session: Session?, factor: SessionManager.Factor) {
            contentTabViewContract.getChromeViewModel().onFocusedUrlChanged(session?.url)
            contentTabViewContract.getChromeViewModel().onFocusedTitleChanged(session?.title)
            session?.url?.let {
                if (!UrlUtils.isInternalErrorURL(it)) {
                    contentTabViewContract.getDisplayUrlView()?.text = it
                }
            }
            if (session != null) {
                val canGoBack = contentTabViewContract.getCurrentSession()?.canGoBack ?: false
                val canGoForward = contentTabViewContract.getCurrentSession()?.canGoForward ?: false
                contentTabViewContract.getChromeViewModel().onNavigationStateChanged(canGoBack, canGoForward)
            }
        }
    }

    private class DownloadCallback(val activity: AppCompatActivity, val permissionHandler: PermissionHandler) : org.mozilla.rocket.tabs.web.DownloadCallback {
        override fun onDownloadStart(download: Download) {
            activity.let {
                if (!it.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    return
                }
            }

            permissionHandler.tryAction(
                activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                ACTION_DOWNLOAD,
                download
            )
        }
    }
}