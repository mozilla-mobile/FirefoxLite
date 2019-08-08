package org.mozilla.rocket.shopping.search.ui

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import androidx.fragment.app.Fragment
import kotlinx.android.synthetic.main.fragment_shopping_search_result_content.web_view
import org.mozilla.focus.R
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.focus.webkit.DefaultWebViewClient

class ShoppingSearchResultContentFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shopping_search_result_content, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        initWebView()
    }

    override fun onStart() {
        super.onStart()
        web_view.loadUrl(arguments?.getString(EXTRA_URL))
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        WebView.setWebContentsDebuggingEnabled(true)

        val settings = web_view.settings
        settings.javaScriptEnabled = true
        settings.defaultTextEncodingName = "UTF-8"
        activity?.let {
            settings.userAgentString = WebViewProvider.getUserAgentString(it)
        }

        // Enable zoom, hide zoom-control.
        settings.builtInZoomControls = true
        settings.displayZoomControls = false
        // Make the web page fit in the screen width.
        settings.loadWithOverviewMode = true
        settings.useWideViewPort = true
        // (100) failed in Samsung Grand Prime, hTC Incredible, (1) is OK for them.
        web_view.setInitialScale(1)
        web_view.webViewClient = DefaultWebViewClient(activity)
        web_view.webChromeClient = WebChromeClient()
    }

    companion object {
        private const val EXTRA_URL = "url"

        fun newInstance(url: String): ShoppingSearchResultContentFragment {
            val args = Bundle().apply {
                putString(EXTRA_URL, url)
            }
            return ShoppingSearchResultContentFragment().apply {
                arguments = args
            }
        }
    }
}
