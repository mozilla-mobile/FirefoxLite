/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.urlinput

import android.arch.lifecycle.Observer
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import mozilla.components.browser.domains.DomainAutoCompleteProvider
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.focus.Inject
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.SearchUtils
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.focus.widget.FlowLayout
import org.mozilla.focus.widget.FragmentListener
import org.mozilla.rocket.urlinput.SearchPortal
import org.mozilla.rocket.urlinput.SearchPortalAdapter
import java.util.Locale

/**
 * Fragment for displaying he URL input controls.
 */
class UrlInputFragment : Fragment(), UrlInputContract.View, View.OnClickListener,
        View.OnLongClickListener, ScreenNavigator.UrlInputScreen {

    private val autoCompleteProvider: DomainAutoCompleteProvider = DomainAutoCompleteProvider()
    private lateinit var presenter: UrlInputContract.Presenter

    private lateinit var urlView: InlineAutocompleteEditText
    private lateinit var suggestionView: FlowLayout
    private lateinit var clearView: View
    private lateinit var dismissView: View
    private lateinit var searchPortalView: RecyclerView
    private var lastRequestTime: Long = 0
    private var autoCompleteInProgress: Boolean = false
    private var allowSuggestion: Boolean = false

    override fun onCreate(bundle: Bundle?) {
        super.onCreate(bundle)
        val userAgent = WebViewProvider.getUserAgentString(activity)
        this.presenter = UrlInputPresenter(SearchEngineManager.getInstance()
                .getDefaultSearchEngine(activity), userAgent)

        context?.let {
            autoCompleteProvider.initialize(it.applicationContext, true, false)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_urlinput, container, false)

        dismissView = view.findViewById(R.id.dismiss)
        dismissView.setOnClickListener(this)

        clearView = view.findViewById(R.id.clear)
        clearView.setOnClickListener(this)

        suggestionView = view.findViewById<View>(R.id.search_suggestion) as FlowLayout

        urlView = view.findViewById<View>(R.id.url_edit) as InlineAutocompleteEditText
        urlView.setOnTextChangeListener(::onTextChange)
        urlView.setOnCommitListener(::onCommit)
        urlView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            // Avoid showing keyboard again when returning to the previous page by back key.
            if (hasFocus) {
                ViewUtils.showKeyboard(urlView)
            } else {
                ViewUtils.hideKeyboard(urlView)
            }
        }

        urlView.setOnFilterListener(::onFilter)
        urlView.imeOptions = urlView.imeOptions or ViewUtils.IME_FLAG_NO_PERSONALIZED_LEARNING

        initByArguments()

        searchPortalView = view.findViewById(R.id.search_portal)
        searchPortalView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val searchPortalAdapter = SearchPortalAdapter(fun (portal: SearchPortal) {
            if (TextUtils.isEmpty(urlView.text)) openUrl(portal.homeUrl)
            else openUrl(String.format(portal.searchUrlPattern, urlView.text))
            TelemetryWrapper.clickQuickSearchEngine(portal.name)
        })
        searchPortalView.adapter = searchPortalAdapter
        Inject.obtainSearchPortalViewModel(activity).searchPortalObservable.observe(viewLifecycleOwner, Observer { searchPortals ->
            searchPortalAdapter.submitList(searchPortals)
        })

        return view
    }

    override fun onStart() {
        super.onStart()
        presenter.setView(this)
        urlView.requestFocus()
    }

    override fun onStop() {
        super.onStop()
        presenter.setView(null)
    }

    override fun onLongClick(view: View): Boolean {
        when (view.id) {
            R.id.suggestion_item -> {
                setUrlText((view as TextView).text)
                TelemetryWrapper.searchSuggestionLongClick()
                return true
            }
            else -> return false
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.clear -> {
                urlView.setText("")
                urlView.requestFocus()
                TelemetryWrapper.searchClear()
            }
            R.id.dismiss -> {
                dismiss()
                TelemetryWrapper.searchDismiss()
            }
            R.id.suggestion_item -> onSuggestionClicked((view as TextView).text)
            else -> throw IllegalStateException("Unhandled view in onClick()")
        }
    }

    private fun initByArguments() {
        val args = arguments
        if (args?.containsKey(ARGUMENT_URL) == true) {
            val url = args.getString(ARGUMENT_URL)
            urlView.setText(url)
            clearView.visibility = if (TextUtils.isEmpty(url)) View.GONE else View.VISIBLE
        }
        allowSuggestion = args?.getBoolean(ARGUMENT_ALLOW_SUGGESTION, true) ?: false
    }

    private fun onSuggestionClicked(tag: CharSequence) {
        search(tag.toString())
        TelemetryWrapper.urlBarEvent(SupportUtils.isUrl(tag.toString()), true)
    }

    private fun dismiss() {
        // This method is called from animation callbacks. In the short time frame between the animation
        // starting and ending the activity can be paused. In this case this code can throw an
        // IllegalStateException because we already saved the state (of the activity / fragment) before
        // this transaction is committed. To avoid this we commit while allowing a state loss here.
        // We do not save any state in this fragment (It's getting destroyed) so this should not be a problem.
        val activity = activity
        if (activity is FragmentListener) {
            (activity as FragmentListener).onNotified(this, FragmentListener.TYPE.DISMISS_URL_INPUT, true)
        }
    }

    private fun onCommit() {
        val input = urlView.autocompleteResult.formattedText.let {
            if (it.isEmpty()) urlView.text.toString() else it
        }
        search(input)
        TelemetryWrapper.urlBarEvent(SupportUtils.isUrl(input), false)
    }

    private fun search(input: String) {
        if (!input.trim { it <= ' ' }.isEmpty()) {
            ViewUtils.hideKeyboard(urlView)

            val isUrl = SupportUtils.isUrl(input)

            val url = if (isUrl)
                SupportUtils.normalize(input)
            else
                SearchUtils.createSearchUrl(context, input)

            val isOpenInNewTab = openUrl(url)

            if (isOpenInNewTab) {
                TelemetryWrapper.addNewTabFromHome()
            }
        }
    }

    /**
     * @param url the URL to open
     * @return true if open URL in new tab.
     */
    private fun openUrl(url: String): Boolean {
        var openNewTab = false

        val args = arguments
        if (args != null && args.containsKey(ARGUMENT_PARENT_FRAGMENT)) {
            openNewTab = ScreenNavigator.HOME_FRAGMENT_TAG == args.getString(ARGUMENT_PARENT_FRAGMENT)
        }

        val activity = activity
        if (activity is FragmentListener) {
            val listener = activity as? FragmentListener
            val msgType = if (openNewTab)
                FragmentListener.TYPE.OPEN_URL_IN_NEW_TAB
            else
                FragmentListener.TYPE.OPEN_URL_IN_CURRENT_TAB

            listener?.onNotified(this, msgType, url)
        }

        return openNewTab
    }

    override fun setUrlText(text: CharSequence?) {
        text?.let {
            this.urlView.setText(text)
            this.urlView.setSelection(text.length)
        }
    }

    override fun setSuggestions(texts: List<CharSequence>?) {
        this.suggestionView.removeAllViews()
        if (texts == null) {
            return
        }

        val searchKey = urlView.originalText.trim { it <= ' ' }.toLowerCase(Locale.getDefault())
        for (i in texts.indices) {
            val item = View.inflate(context, R.layout.tag_text, null) as TextView
            val str = texts[i].toString()
            val idx = str.toLowerCase(Locale.getDefault()).indexOf(searchKey)
            if (idx != -1) {
                val builder = SpannableStringBuilder(texts[i])
                builder.setSpan(android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                        idx,
                        idx + searchKey.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                item.text = builder
            } else {
                item.text = texts[i]
            }

            item.setOnClickListener(this)
            item.setOnLongClickListener(this)
            this.suggestionView.addView(item)
        }
    }

    private fun onFilter(searchText: String, view: InlineAutocompleteEditText?) {
        // If the UrlInputFragment has already been hidden, don't bother with filtering. Because of the text
        // input architecture on Android it's possible for onFilter() to be called after we've already
        // hidden the Fragment, see the relevant bug for more background:
        // https://github.com/mozilla-mobile/focus-android/issues/441#issuecomment-293691141
        if (!isVisible) {
            return
        }
        autoCompleteInProgress = true
        val result = autoCompleteProvider.autocomplete(searchText)
        view?.applyAutocompleteResult(InlineAutocompleteEditText.AutocompleteResult(result.text, result.source, result.size) { result.url })
        autoCompleteInProgress = false
    }

    private fun onTextChange(originalText: String, @Suppress("UNUSED_PARAMETER") autocompleteText: String) {
        if (autoCompleteInProgress) {
            return
        }
        if (allowSuggestion) {
            this@UrlInputFragment.presenter.onInput(originalText, detectThrottle())
        }
        val visibility = if (TextUtils.isEmpty(originalText)) View.GONE else View.VISIBLE
        this@UrlInputFragment.clearView.visibility = visibility
    }

    private fun detectThrottle(): Boolean {
        val now = System.currentTimeMillis()
        val throttled = now - lastRequestTime < REQUEST_THROTTLE_THRESHOLD
        lastRequestTime = now
        return throttled
    }

    companion object {

        private const val ARGUMENT_URL = "url"
        private const val ARGUMENT_PARENT_FRAGMENT = "parent_frag_tag"
        private const val ARGUMENT_ALLOW_SUGGESTION = "allow_suggestion"
        private const val REQUEST_THROTTLE_THRESHOLD = 300

        /**
         * Create a new UrlInputFragment and animate the url input view from the position/size of the
         * fake url bar view.
         */
        @JvmStatic
        fun create(url: String?, parentFragmentTag: String?, allowSuggestion: Boolean): UrlInputFragment {
            val arguments = Bundle()
            arguments.putString(ARGUMENT_URL, url)
            arguments.putString(ARGUMENT_PARENT_FRAGMENT, parentFragmentTag)
            arguments.putBoolean(ARGUMENT_ALLOW_SUGGESTION, allowSuggestion)

            val fragment = UrlInputFragment()
            fragment.arguments = arguments

            return fragment
        }
    }

    override fun getFragment() = this
}
