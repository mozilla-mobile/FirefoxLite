package org.mozilla.rocket.privately.browse

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import org.mozilla.focus.BuildConfig
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE
import org.mozilla.rocket.privately.SharedViewModel
import org.mozilla.rocket.tabs.TabsSession
import org.mozilla.rocket.tabs.TabsSessionProvider
import org.mozilla.rocket.tabs.utils.DefaultTabsChromeListener
import org.mozilla.rocket.tabs.utils.DefaultTabsViewListener

class BrowserFragment : LocaleAwareFragment(),
        BackKeyHandleable {

    private var listener: FragmentListener? = null

    private lateinit var tabsSession: TabsSession
    private lateinit var chromeListener: BrowserTabsChromeListener
    private lateinit var viewListener: BrowserTabsViewListener

    private lateinit var displayUrlView: TextView

    override fun onCreate(savedState: Bundle?) {
        super.onCreate(savedState)
    }

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
        displayUrlView.setOnClickListener {
            val listener = activity as FragmentListener
            listener.onNotified(BrowserFragment@ this, TYPE.SHOW_URL_INPUT, displayUrlView.text)
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
        // FIXME: for development purpose. Handle back key until url bar is empty
        if (displayUrlView.text != null && displayUrlView.text.isNotBlank()) {
            displayUrlView.text = null
            return true
        }
        return false
    }

    fun loadUrl(url: String?) {
        url?.let {
            if (it.isNotBlank()) {
                displayUrlView.text = url
            }
        }
    }

    private fun registerData(activity: FragmentActivity) {
        val shared = ViewModelProviders.of(activity).get(SharedViewModel::class.java)

        shared.getUrl().observe(this, Observer<String> { url -> loadUrl(url) })
    }

    class BrowserTabsChromeListener(val fragment: BrowserFragment) : DefaultTabsChromeListener() {
    }

    class BrowserTabsViewListener(val fragment: BrowserFragment) : DefaultTabsViewListener() {
    }
}