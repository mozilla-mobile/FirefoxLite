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
import org.mozilla.focus.BuildConfig.FXA_API_URL
import org.mozilla.focus.BuildConfig.FXA_CLIENT_ID
import org.mozilla.focus.R
import org.mozilla.focus.activity.MainActivity
import org.mozilla.focus.navigation.ScreenNavigator
import java.net.URL

class FxLoginFragment : Fragment(), ScreenNavigator.FxLoginScreen {

    override fun getFragment() = this

    private lateinit var prevUid: String
    private var mWebView: WebView? = null
    private var listener: OnLoginCompleteListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            prevUid = it.getString(PREV_UID) ?: error("get no argument PREV_UID")
        }
    }

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

                    if (map.size > 1) {
                        listener?.onLoginComplete(
                            map["jwt"] ?: error("missing required field`jwt`"),
                            this@FxLoginFragment
                        )
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
        webView.loadUrl(getFxaAuthorizationEndpoint(prevUid))

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
        fun onLoginComplete(jwt: String, fragmentFx: FxLoginFragment)
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
        const val PREV_UID = "authUrl"
        // scope:"profile", "https://identity.mozilla.com/apps/oldsync"
        const val AUTH_END_UID_PLACEHOLDER = "[auth_end_uid_placeholder]"
        const val AUTH_END = "$FXA_API_URL?client_id=$FXA_CLIENT_ID&state=$AUTH_END_UID_PLACEHOLDER&scope=profile"
        const val REDIRECT_URL = "?jwt="

        fun create(uid: String?): FxLoginFragment {
            if (uid == null) {
                throw java.lang.IllegalStateException("uid can't be null")
            }
            return FxLoginFragment().apply {
                arguments = Bundle().apply {
                    putString(PREV_UID, uid)
                }
            }
        }
    }
}

class MissionDetailFragment : Fragment(), ScreenNavigator.MissionDetailScreen {

    override fun getFragment() = this

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val inflate: View = inflater.inflate(R.layout.fragment_mission_detail, container, false)
        inflate.findViewById<View>(R.id.dmd_sign_in).setOnClickListener {
            val isOnMain = activity is MainActivity
            if (isOnMain) {
                (activity as MainActivity).loginFxa()
            }
        }
        return inflate
    }
}

class RedeemFragment : Fragment(), ScreenNavigator.RedeemSceen {

    override fun getFragment() = this

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_redeem, container, false)
    }
}