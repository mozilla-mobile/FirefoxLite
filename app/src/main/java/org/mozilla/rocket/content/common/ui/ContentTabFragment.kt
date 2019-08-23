package org.mozilla.rocket.content.common.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import androidx.lifecycle.Observer
import dagger.Lazy
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.tabs.Session
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabsSessionProvider
import org.mozilla.rocket.tabs.utils.TabUtil
import javax.inject.Inject

class ContentTabFragment : LocaleAwareFragment(), BackKeyHandleable {

    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    private lateinit var sessionManager: SessionManager
    private lateinit var tabSession: Session
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var tabViewSlot: ViewGroup

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_content_tab, container, false)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        tabViewSlot = view.findViewById(R.id.tab_view_slot)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sessionManager = TabsSessionProvider.getOrThrow(activity)

        val tabId = sessionManager.addTab("https://", TabUtil.argument(null, false, true))
        tabSession = sessionManager.getTabs().find { it.id == tabId }!!
        if (tabViewSlot.childCount == 0) {
            tabSession.engineSession?.tabView?.apply {
                val enableTurboMode = arguments?.getBoolean(EXTRA_ENABLE_TURBO_MODE) ?: true
                setContentBlockingEnabled(enableTurboMode)
                tabViewSlot.addView(view)
            }
        }

        observeChromeAction()

        arguments?.getString(EXTRA_URL)?.apply { loadUrl(this) }
    }

    override fun onResume() {
        super.onResume()
        sessionManager.resume()
    }

    override fun onPause() {
        super.onPause()
        sessionManager.pause()
    }

    override fun applyLocale() {
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See issue #666.
        val unneeded = WebView(context)
        unneeded.destroy()
    }

    override fun onBackPressed(): Boolean {
        val tabView = tabSession.engineSession?.tabView ?: return false

        if (tabView.canGoBack()) {
            goBack()
            return true
        }

        sessionManager.dropTab(tabSession.id)
        return false
    }

    private fun goBack() = sessionManager.focusSession?.engineSession?.goBack()

    private fun goForward() = sessionManager.focusSession?.engineSession?.goForward()

    private fun stop() = sessionManager.focusSession?.engineSession?.stopLoading()

    private fun reload() = sessionManager.focusSession?.engineSession?.reload()

    private fun observeChromeAction() {
        chromeViewModel.refreshOrStop.observe(this, Observer {
            if (chromeViewModel.isRefreshing.value == true) {
                stop()
            } else {
                reload()
            }
        })
        chromeViewModel.goNext.observe(this, Observer {
            if (chromeViewModel.canGoForward.value == true) {
                goForward()
            }
        })
    }

    private fun loadUrl(url: String) {
        if (url.isNotBlank()) {
            tabSession.engineSession?.tabView?.apply {
                loadUrl(url)
            }
        }
    }

    companion object {
        private const val EXTRA_URL = "url"
        private const val EXTRA_ENABLE_TURBO_MODE = "enable_turbo_mode"

        fun newInstance(url: String, enableTurboMode: Boolean = true): ContentTabFragment {
            val args = Bundle().apply {
                putString(EXTRA_URL, url)
                putBoolean(EXTRA_ENABLE_TURBO_MODE, enableTurboMode)
            }
            return ContentTabFragment().apply {
                arguments = args
            }
        }
    }
}
