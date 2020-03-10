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
import org.mozilla.focus.widget.ResizableKeyboardLayout
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
    private var tabSession: Session? = null
    private lateinit var chromeViewModel: ChromeViewModel
    private lateinit var tabViewSlot: ViewGroup
    private lateinit var contentLayout: ResizableKeyboardLayout

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
        contentLayout = view.findViewById(R.id.main_content)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        sessionManager = TabsSessionProvider.getOrThrow(activity)

        if (tabSession == null) {
            val tabId = sessionManager.addTab("https://", TabUtil.argument(null, false, true))
            tabSession = sessionManager.getTabs().find { it.id == tabId }!!
            tabSession?.engineSession?.tabView?.apply {
                val enableTurboMode = arguments?.getBoolean(EXTRA_ENABLE_TURBO_MODE) ?: true
                setContentBlockingEnabled(enableTurboMode)

                val url = arguments?.getString(EXTRA_URL)
                if (!url.isNullOrEmpty()) {
                    loadUrl(url)
                }
            }
        }

        if (tabViewSlot.childCount == 0) {
            tabSession?.engineSession?.tabView?.apply {
                tabViewSlot.addView(view)
            }
        }

        observeChromeAction()
    }

    override fun onDestroyView() {
        tabViewSlot.removeAllViews()
        super.onDestroyView()
    }

    override fun applyLocale() {
        // We create and destroy a new WebView here to force the internal state of WebView to know
        // about the new language. See issue #666.
        val unneeded = WebView(context)
        unneeded.destroy()
    }

    override fun onBackPressed(): Boolean {
        val tabView = tabSession?.engineSession?.tabView ?: return false

        if (tabView.canGoBack()) {
            goBack()
            return true
        }

        tabSession?.id?.let {
            sessionManager.dropTab(it)
        }
        return false
    }

    fun switchToFocusTab() {
        tabSession?.id?.let {
            sessionManager.switchToTab(it)
        }
    }

    fun setOnKeyboardVisibilityChangedListener(listener: ResizableKeyboardLayout.OnKeyboardVisibilityChangedListener?) {
        contentLayout.setOnKeyboardVisibilityChangedListener(listener)
    }

    private fun goBack() = sessionManager.focusSession?.engineSession?.goBack()

    private fun goForward() = sessionManager.focusSession?.engineSession?.goForward()

    private fun stop() = sessionManager.focusSession?.engineSession?.stopLoading()

    private fun reload() = sessionManager.focusSession?.engineSession?.reload()

    private fun observeChromeAction() {
        chromeViewModel.refreshOrStop.observe(viewLifecycleOwner, Observer {
            if (chromeViewModel.isRefreshing.value == true) {
                stop()
            } else {
                reload()
            }
        })
        chromeViewModel.goNext.observe(viewLifecycleOwner, Observer {
            if (chromeViewModel.canGoForward.value == true) {
                goForward()
            }
        })

        val forceDisableImageBlocking =
            arguments?.getBoolean(EXTRA_FORCE_DISABLE_IMAGE_BLOCKING) ?: false
        if (forceDisableImageBlocking) {
            chromeViewModel.isRefreshing.observe(viewLifecycleOwner, Observer { isRefreshing ->
                if (!isRefreshing) {
                    tabSession?.engineSession?.tabView?.apply {
                        setImageBlockingEnabled(false)
                    }
                }
            })
        }
    }

    companion object {
        const val EXTRA_URL = "url"
        private const val EXTRA_ENABLE_TURBO_MODE = "enable_turbo_mode"
        private const val EXTRA_FORCE_DISABLE_IMAGE_BLOCKING = "force_disable_image_blocking"

        fun newInstance(url: String, enableTurboMode: Boolean = true, forceDisableImageBlocking: Boolean = false): ContentTabFragment {
            val args = Bundle().apply {
                putString(EXTRA_URL, url)
                putBoolean(EXTRA_ENABLE_TURBO_MODE, enableTurboMode)
                putBoolean(EXTRA_FORCE_DISABLE_IMAGE_BLOCKING, forceDisableImageBlocking)
            }
            return ContentTabFragment().apply {
                arguments = args
            }
        }

        fun newInstance(url: String, session: Session): ContentTabFragment {
            val args = Bundle().apply {
                putString(EXTRA_URL, url)
            }
            return ContentTabFragment().apply {
                arguments = args
                tabSession = session
            }
        }
    }
}
