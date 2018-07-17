package org.mozilla.rocket.privately.browse

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import org.mozilla.focus.R
import org.mozilla.focus.locale.LocaleAwareFragment
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.focus.widget.FragmentListener.TYPE

private const val ARGUMENT_INIT_URL = "_init_url_"

class BrowserFragment : LocaleAwareFragment() {

    private var listener: FragmentListener? = null
    private var pendingUrl: String = ""

    private lateinit var displayUrlView: TextView

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {

        if (savedInstanceState == null) {
            pendingUrl = getInitUrl()
        }

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_private_browser, container, false)
    }

    override fun onViewCreated(view: View, savedState: Bundle?) {
        super.onViewCreated(view, savedState)

        displayUrlView = view.findViewById(R.id.display_url)
        displayUrlView.setOnClickListener {
            val listener = activity as FragmentListener
            listener.onNotified(BrowserFragment@ this, TYPE.SHOW_URL_INPUT, displayUrlView.text)
        }

        if (pendingUrl.isNotBlank()) {
            loadUrl(pendingUrl)
        }
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

    fun loadUrl(url: String) {
        displayUrlView.text = url
        pendingUrl = ""
    }

    private fun getInitUrl(): String {
        return arguments?.getString(ARGUMENT_INIT_URL) ?: ""
    }

    companion object {
        @JvmStatic
        fun createArgument(url: String): Bundle {
            return Bundle().apply { putString(ARGUMENT_INIT_URL, url) }
        }
    }
}