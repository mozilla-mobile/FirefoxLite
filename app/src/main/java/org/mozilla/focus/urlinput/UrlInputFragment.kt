/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.urlinput

import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.webkit.URLUtil
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_urlinput.input_container
import kotlinx.android.synthetic.main.fragment_urlinput.awesomeBar
import kotlinx.android.synthetic.main.fragment_urlinput.search_suggestion_block
import mozilla.components.browser.domains.autocomplete.ShippedDomainsProvider
import mozilla.components.ui.autocomplete.InlineAutocompleteEditText
import org.mozilla.focus.R
import org.mozilla.focus.navigation.ScreenNavigator
import org.mozilla.focus.search.SearchEngineManager
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value.AWESOMEBAR_TYPE_AUTOCOMPLETE
import org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value.AWESOMEBAR_TYPE_BOOKMARK
import org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value.AWESOMEBAR_TYPE_CLIPBOARD
import org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value.AWESOMEBAR_TYPE_HISTORY
import org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value.AWESOMEBAR_TYPE_MANUALCOMPLETE
import org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value.AWESOMEBAR_TYPE_SUGGESTION
import org.mozilla.focus.telemetry.TelemetryWrapper.Extra_Value.AWESOMEBAR_TYPE_TABTRAY
import org.mozilla.focus.utils.SearchUtils
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.focus.web.WebViewProvider
import org.mozilla.focus.widget.FlowLayout
import org.mozilla.rocket.awesomebar.BookmarkSuggestionProvider
import org.mozilla.rocket.awesomebar.ClipboardSuggestionProvider
import org.mozilla.rocket.awesomebar.HistorySuggestionProvider
import org.mozilla.rocket.awesomebar.SessionSuggestionProvider
import org.mozilla.rocket.chrome.ChromeViewModel
import org.mozilla.rocket.chrome.ChromeViewModel.OpenUrlAction
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.tabs.SessionManager
import org.mozilla.rocket.tabs.TabsSessionProvider
import org.mozilla.rocket.urlinput.QuickSearch
import org.mozilla.rocket.urlinput.QuickSearchAdapter
import org.mozilla.rocket.urlinput.QuickSearchViewModel
import java.util.Locale
import javax.inject.Inject

/**
 * Fragment for displaying he URL input controls.
 */
class UrlInputFragment : Fragment(), UrlInputContract.View, View.OnClickListener,
        View.OnLongClickListener, ScreenNavigator.UrlInputScreen {

    @Inject
    lateinit var quickSearchViewModelCreator: Lazy<QuickSearchViewModel>
    @Inject
    lateinit var chromeViewModelCreator: Lazy<ChromeViewModel>

    private val autoCompleteProvider: ShippedDomainsProvider = ShippedDomainsProvider()
    private lateinit var presenter: UrlInputContract.Presenter
    private lateinit var chromeViewModel: ChromeViewModel

    private lateinit var urlView: InlineAutocompleteEditText
    private lateinit var suggestionView: FlowLayout
    private lateinit var clearView: View
    private lateinit var dismissView: View
    private lateinit var quickSearchRecyclerView: RecyclerView
    private lateinit var quickSearchView: ViewGroup
    private var lastRequestTime: Long = 0
    private var autoCompleteInProgress: Boolean = false
    private var allowSuggestion: Boolean = false
    private var isUserInput = true

    override fun onCreate(bundle: Bundle?) {
        appComponent().inject(this)
        super.onCreate(bundle)
        val userAgent = WebViewProvider.getUserAgentString(activity)
        this.presenter = UrlInputPresenter(SearchEngineManager.getInstance()
                .getDefaultSearchEngine(activity), userAgent)
        chromeViewModel = getActivityViewModel(chromeViewModelCreator)

        context?.let {
            autoCompleteProvider.initialize(it.applicationContext)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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

        initByArguments()

        initQuickSearch(view)

        return view
    }

    private fun toAwesomeBarIcon(res: Int): Bitmap? {
        return AppCompatResources.getDrawable(requireContext(), res)?.also {
            DrawableCompat.setTint(it, Color.WHITE)
        }?.toBitmap()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = context ?: return
        val bookmarkRepo = chromeViewModel.bookmarkRepo
        val historyRepo = chromeViewModel.historyRepo
        val iconBookmark = toAwesomeBarIcon(R.drawable.ic_illustration_no_bookmarks)
        val iconHistory = toAwesomeBarIcon(R.drawable.history_empty)
        val iconTab = toAwesomeBarIcon(R.drawable.ic_current_tab)

        awesomeBar.setOnApplyWindowInsetsListener { v: View, insets: WindowInsets ->
            (v.layoutParams as ViewGroup.MarginLayoutParams).topMargin = insets.systemWindowInsetTop
            insets
        }
        awesomeBar.addProviders(
                HistorySuggestionProvider(iconHistory, historyRepo) {
                    onSuggestionClicked(it, AWESOMEBAR_TYPE_HISTORY)
                },
                BookmarkSuggestionProvider(iconBookmark, bookmarkRepo) {
                    onSuggestionClicked(it, AWESOMEBAR_TYPE_BOOKMARK)
                },
                SessionSuggestionProvider(
                    iconTab,
                    TabsSessionProvider.getOrThrow(activity)
                ) { sm: SessionManager, id: String ->
                    sm.switchToTab(id)
                    ScreenNavigator[context].raiseBrowserScreen(false)
                    val isInLandscape = isInLandscape()
                    TelemetryWrapper.urlBarEvent(
                        isUrl = true,
                        isSuggestion = true,
                        isInLandscape = isInLandscape,
                        type = AWESOMEBAR_TYPE_TABTRAY
                    )
                },
                ClipboardSuggestionProvider(ctx) {
                    onSuggestionClicked(it, AWESOMEBAR_TYPE_CLIPBOARD)
                }
        )

        awesomeBar.onInputStarted() // if something is in the clipboard, it'll be the first and show directly
        awesomeBar.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                recyclerView.requestFocus()
                ViewUtils.hideKeyboard(recyclerView)
            }
        })
    }

    private fun initQuickSearch(view: View) {
        quickSearchView = view.findViewById(R.id.quick_search_container)
        quickSearchRecyclerView = view.findViewById(R.id.quick_search_recycler_view)
        quickSearchRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        val quickSearchAdapter = QuickSearchAdapter(fun(quickSearch: QuickSearch) {
            if (TextUtils.isEmpty(urlView.text)) {
                openUrl(quickSearch.homeUrl)
            } else {
                openUrl(quickSearch.generateLink(urlView.originalText))
            }
            TelemetryWrapper.clickQuickSearchEngine(quickSearch.name)
        })
        quickSearchRecyclerView.adapter = quickSearchAdapter
        getActivityViewModel(quickSearchViewModelCreator).run {
            quickSearchObservable.observe(
                    viewLifecycleOwner,
                    Observer { quickSearchList ->
                        quickSearchAdapter.submitList(quickSearchList)
                    }
            )
        }
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

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateUrlInputHeight()
    }

    private fun updateUrlInputHeight() {
        val urlInputHeight = input_container.resources.getDimensionPixelOffset(R.dimen.search_url_input_height)
        input_container.layoutParams = input_container.layoutParams.apply {
            height = urlInputHeight
        }
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
                awesomeBar.onInputCancelled()
                TelemetryWrapper.searchClear(isInLandscape())
            }
            R.id.dismiss -> {
                dismiss()
                TelemetryWrapper.searchDismiss(isInLandscape())
            }
            R.id.suggestion_item -> onSuggestionClicked((view as TextView).text, AWESOMEBAR_TYPE_SUGGESTION)
            else -> throw IllegalStateException("Unhandled view in onClick()")
        }
    }

    private fun isInLandscape(): Boolean {
        return resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    }

    private fun initByArguments() {
        val args = arguments
        if (args?.containsKey(ARGUMENT_URL) == true) {
            val url = args.getString(ARGUMENT_URL)
            urlView.setText(url)
            clearView.visibility = if (TextUtils.isEmpty(url)) View.GONE else View.VISIBLE
        }
        allowSuggestion = args?.getBoolean(ARGUMENT_ALLOW_SUGGESTION, true) ?: false
        if (args?.getBoolean(ARGUMENT_BOOLEAN_PRIVATE_MODE, false) == true) {
            urlView.imeOptions = urlView.imeOptions or ViewUtils.IME_FLAG_NO_PERSONALIZED_LEARNING
        }
    }

    private fun onSuggestionClicked(tag: CharSequence, type: String) {
        val isInLandscape = isInLandscape()
        search(tag.toString())
        TelemetryWrapper.urlBarEvent(SupportUtils.isUrl(tag.toString()), true, isInLandscape, type)
    }

    private fun dismiss() {
        // This method is called from animation callbacks. In the short time frame between the animation
        // starting and ending the activity can be paused. In this case this code can throw an
        // IllegalStateException because we already saved the state (of the activity / fragment) before
        // this transaction is committed. To avoid this we commit while allowing a state loss here.
        // We do not save any state in this fragment (It's getting destroyed) so this should not be a problem.
        chromeViewModel.dismissUrlInput.call()
    }

    private fun onCommit() {
        val isInLandscape = isInLandscape()
        val input = urlView.autocompleteResult?.let { result ->
            if (result.text.isEmpty() || !URLUtil.isValidUrl(urlView.text.toString())) {
                urlView.text.toString()
            } else {
                result.text
            }
        } ?: urlView.text.toString()
        search(input)
        val type = if (isUserInput) {
            AWESOMEBAR_TYPE_MANUALCOMPLETE
        } else {
            AWESOMEBAR_TYPE_AUTOCOMPLETE
        }
        TelemetryWrapper.urlBarEvent(SupportUtils.isUrl(input), false, isInLandscape, type)
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
        val args = arguments
        val openNewTab = if (args != null && args.containsKey(ARGUMENT_PARENT_FRAGMENT)) {
            ScreenNavigator.HOME_FRAGMENT_TAG == args.getString(ARGUMENT_PARENT_FRAGMENT)
        } else {
            false
        }
        chromeViewModel.openUrl.value = OpenUrlAction(url, withNewTab = openNewTab, isFromExternal = false)

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
        search_suggestion_block.visibility = View.GONE
        if (texts == null) {
            return
        }

        val searchKey = urlView.originalText.trim { it <= ' ' }.toLowerCase(Locale.getDefault())
        if (texts.isNotEmpty()) {
            search_suggestion_block.visibility = View.VISIBLE
        }
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

    override fun setQuickSearchVisible(visible: Boolean) {
    }

    private fun onFilter(searchText: String) {
        // If the UrlInputFragment has already been hidden, don't bother with filtering. Because of the text
        // input architecture on Android it's possible for onFilter() to be called after we've already
        // hidden the Fragment, see the relevant bug for more background:
        // https://github.com/mozilla-mobile/focus-android/issues/441#issuecomment-293691141
        if (!isVisible) {
            return
        }
        autoCompleteInProgress = true
        autoCompleteProvider.getAutocompleteSuggestion(searchText)?.let { result ->
            isUserInput = result.text == urlView.text.toString() || result.totalItems == 0
            urlView.applyAutocompleteResult(InlineAutocompleteEditText.AutocompleteResult(result.text, result.source, result.totalItems) { result.url })
        } ?: run {
            isUserInput = true
            urlView.applyAutocompleteResult(InlineAutocompleteEditText.AutocompleteResult(searchText, "", 0))
        }
        autoCompleteInProgress = false
    }

    private fun onTextChange(
        originalText: String,
        @Suppress("UNUSED_PARAMETER") autocompleteText: String
    ) {
        if (autoCompleteInProgress) {
            return
        }
        if (allowSuggestion) {
            awesomeBar.onInputChanged(originalText)
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
        private const val ARGUMENT_BOOLEAN_PRIVATE_MODE = "boolean_private_mode"
        private const val REQUEST_THROTTLE_THRESHOLD = 300

        /**
         * Create a new UrlInputFragment and animate the url input view from the position/size of the
         * fake url bar view.
         */
        @JvmStatic
        fun create(url: String?, parentFragmentTag: String?, allowSuggestion: Boolean, privateMode: Boolean = false): UrlInputFragment {
            val arguments = Bundle()
            arguments.putString(ARGUMENT_URL, url)
            arguments.putString(ARGUMENT_PARENT_FRAGMENT, parentFragmentTag)
            arguments.putBoolean(ARGUMENT_ALLOW_SUGGESTION, allowSuggestion)
            arguments.putBoolean(ARGUMENT_BOOLEAN_PRIVATE_MODE, privateMode)

            val fragment = UrlInputFragment()
            fragment.arguments = arguments

            return fragment
        }
    }

    override fun getFragment() = this
}
