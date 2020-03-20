package org.mozilla.rocket.shopping.search.ui

import android.os.Bundle
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_shopping_search_keyword_input.*
import org.mozilla.focus.R
import org.mozilla.focus.glide.GlideApp
import org.mozilla.focus.telemetry.TelemetryWrapper
import org.mozilla.focus.utils.ViewUtils
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import org.mozilla.rocket.shopping.search.data.ShoppingSearchMode
import javax.inject.Inject

class ShoppingSearchKeywordInputFragment : Fragment(), View.OnClickListener, ViewTreeObserver.OnGlobalLayoutListener {

    @Inject
    lateinit var viewModelCreator: Lazy<ShoppingSearchKeywordInputViewModel>

    private lateinit var viewModel: ShoppingSearchKeywordInputViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        viewModel = getViewModel(viewModelCreator)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shopping_search_keyword_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        ShoppingSearchMode.getInstance(view.context).deleteKeyword()

        viewModel.uiModel.observe(viewLifecycleOwner, Observer { uiModel ->
            setupView(uiModel)
        })

        viewModel.navigateToResultTab.observe(viewLifecycleOwner, Observer { showResultTab(it) })

        search_keyword_edit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // TODO: Deal with non-sequence responses when a user types quickly
                s?.let { viewModel.onTypingKeyword(it.toString()) }
            }
        })
        search_keyword_edit.setOnEditorActionListener { editTextView, actionId, _ ->
            when (actionId) {
                EditorInfo.IME_ACTION_SEARCH -> {
                    viewModel.onTypedKeywordSent(editTextView.text.toString())
                    true
                }
                else -> false
            }
        }
        search_keyword_edit.onFocusChangeListener = View.OnFocusChangeListener { v, hasFocus ->
            // Avoid showing keyboard again when returning to the previous page by back key.
            if (hasFocus) {
                ViewUtils.showKeyboard(v)
            } else {
                ViewUtils.hideKeyboard(v)
            }
        }

        clear.setOnClickListener(this)

        root_view.setOnKeyboardVisibilityChangedListener { visible ->
            if (visible) {
                viewModel.onKeyboardShown()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        root_view.viewTreeObserver.addOnGlobalLayoutListener(this)
        search_keyword_edit.requestFocus()
        viewModel.onStart(search_keyword_edit.text.toString())
    }

    override fun onStop() {
        super.onStop()
        root_view.viewTreeObserver.removeOnGlobalLayoutListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.clear -> search_keyword_edit.text.clear()
            R.id.suggestion_item -> {
                val searchTerm = (view as TextView).text
                val isTrendingKeyword = search_keyword_edit.text.isEmpty()
                search_keyword_edit.text = SpannableStringBuilder(searchTerm)
                viewModel.onSuggestionKeywordSent(searchTerm.toString(), isTrendingKeyword)
            }
            else -> throw IllegalStateException("Unhandled view in onClick()")
        }
    }

    override fun onGlobalLayout() {
        val contentLayoutHeight = content_layout.measuredHeight
        val descriptionHeight = description.measuredHeight
        val logoManHeight = logo_man.measuredHeight
        val searchSuggestionLayoutHeight = search_suggestion_layout.measuredHeight
        val inputContainerHeight = input_container.measuredHeight

        val extraMargin = (contentLayoutHeight / 10)
        val expectedContentHeight = descriptionHeight + logoManHeight + searchSuggestionLayoutHeight + inputContainerHeight + extraMargin

        logo_man.isVisible = (contentLayoutHeight > expectedContentHeight)
    }

    private fun setupView(uiModel: ShoppingSearchKeywordInputUiModel) {
        description.text = uiModel.description
        if (uiModel.logoManUrl.isNotEmpty()) {
            GlideApp.with(logo_man.context)
                    .asBitmap()
                    .placeholder(uiModel.defaultLogoManResId)
                    .load(uiModel.logoManUrl)
                    .into(logo_man)
        }
        clear.visibility = if (uiModel.hideClear) View.GONE else View.VISIBLE
        setSuggestions(uiModel.keywordSuggestions)
    }

    private fun setSuggestions(suggestions: List<CharSequence>?) {
        search_suggestion_view.removeAllViews()
        if (suggestions == null || suggestions.isEmpty()) {
            search_suggestion_layout.visibility = View.GONE
            return
        }

        search_suggestion_layout.visibility = View.VISIBLE
        for (suggestion in suggestions) {
            val item = View.inflate(context, R.layout.tag_text, null) as TextView
            item.text = suggestion
            item.setOnClickListener(this)
            this.search_suggestion_view.addView(item)
        }
    }

    private fun showResultTab(keyword: String) {
        findNavController().navigate(
            ShoppingSearchKeywordInputFragmentDirections.actionSearchKeywordToResult(keyword)
        )

        TelemetryWrapper.addTabSwipeTab(TelemetryWrapper.Extra_Value.SHOPPING)
    }
}
