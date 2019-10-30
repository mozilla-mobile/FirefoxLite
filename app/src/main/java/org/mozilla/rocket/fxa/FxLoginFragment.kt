/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.rocket.fxa

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import org.mozilla.focus.BuildConfig.FXA_API_URL
import org.mozilla.focus.BuildConfig.FXA_CLIENT_ID
import org.mozilla.focus.R
import java.net.URL

open class FxLoginFragment : Fragment() {

    private val safeArgs: FxLoginFragmentArgs by navArgs()
    private val requestCode by lazy { safeArgs.requestCode }
    private val uid by lazy { safeArgs.uid }
    private var mWebView: WebView? = null
    private var listener: OnLoginCompleteListener? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view: View = inflater.inflate(R.layout.fragment_fx_login, container, false)
        val progress = view.findViewById<ProgressBar>(R.id.login_progress)
        val webView = view.findViewById<WebView>(R.id.login_webview)
        // Need JS, cookies and localStorage.
        webView.settings.domStorageEnabled = true
        webView.settings.javaScriptEnabled = true
        CookieManager.getInstance().setAcceptCookie(true)

        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                if (url != null && url.contains(REDIRECT_URL)) {
                    progress.visibility = View.VISIBLE
                    val map = getQueryMap(URL(url).query)

                    if (map.size > 1 && map["login_success"]?.toBoolean() == true) {
                        listener?.onLoginSuccess(
                            requestCode,
                            map["jwt"] ?: error("missing required field`jwt`"),
                            map["disabled"]?.toBoolean() ?: error("missing required field`disabled`"),
                            map["times"]?.toInt() ?: error("missing required field`times`")
                        )
                    } else {
                        listener?.onLoginFailure()
                    }
                }

                super.onPageStarted(view, url, favicon)
            }

            override fun shouldOverrideUrlLoading(view: WebView?, url: String?): Boolean {
                if (url?.contains("code=") == true) {
                    val code = Uri.parse(url).getQueryParameter("code")
                    Log.d("Sample", "code=$code")

                    return false
                }
                return false
            }
        }
        webView.loadUrl(getFxaAuthorizationEndpoint(uid))

        mWebView = webView

        return view
    }

    @Suppress("TooGenericExceptionThrown")
    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnLoginCompleteListener) {
            listener = context
        } else {
            throw IllegalStateException("$context must implement OnLoginCompleteListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    override fun onPause() {
        super.onPause()
        mWebView?.onPause()
    }

    override fun onResume() {
        super.onResume()
        mWebView?.onResume()
    }

    interface OnLoginCompleteListener {
        fun onLoginSuccess(requestCode: Int, jwt: String, isDisabled: Boolean, statusCode: Int)
        fun onLoginFailure()
    }

    fun getQueryMap(query: String): Map<String, String> {
        val params = query.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val map = HashMap<String, String>()
        for (param in params) {
            val name = param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0]
            val value = param.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1]
            map[name] = value
        }
        return map
    }

    private fun getFxaAuthorizationEndpoint(uid: String): String {
        // wait for auth.id to be init

        return AUTH_END.replace(AUTH_END_UID_PLACEHOLDER, uid)
    }

    companion object {
        // scope:"profile", "https://identity.mozilla.com/apps/oldsync"
        const val AUTH_END_UID_PLACEHOLDER = "[auth_end_uid_placeholder]"
        const val AUTH_END = "$FXA_API_URL?client_id=$FXA_CLIENT_ID&state=$AUTH_END_UID_PLACEHOLDER&scope=profile"
        const val REDIRECT_URL = "?jwt="

        const val STATUS_CODE_NONE = 0
        const val STATUS_CODE_WARNING = 1
        const val STATUS_CODE_FINAL_WARNING = 2
    }
}
