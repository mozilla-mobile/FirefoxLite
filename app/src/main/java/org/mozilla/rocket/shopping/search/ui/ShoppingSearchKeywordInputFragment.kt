package org.mozilla.rocket.shopping.search.ui

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_shopping_search_keyword_input.*
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import javax.inject.Inject

class ShoppingSearchKeywordInputFragment : Fragment() {

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

        viewModel.uiModel.observe(this, Observer { uiModel ->
            setSuggestions(uiModel.keywordSuggestions)
        })

        search_keyword_edit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // TODO: Deal with non-sequence responses when a user types quickly
                s?.let { viewModel.fetchSuggestions(it.toString()) }
            }
        })
        search_keyword_edit.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (event.action == KeyEvent.ACTION_DOWN) {
                    findNavController().navigate(
                        ShoppingSearchKeywordInputFragmentDirections.actionSearchKeywordToResult(
                            search_keyword_edit.text.toString()
                        )
                    )
                    return@setOnKeyListener true
                }
            }
            return@setOnKeyListener false
        }
    }

    private fun setSuggestions(suggestions: List<CharSequence>?) {
        search_suggestion_view.removeAllViews()
        if (suggestions == null) {
            return
        }

        for (suggestion in suggestions) {
            val item = View.inflate(context, R.layout.tag_text, null) as TextView
            item.text = suggestion
            this.search_suggestion_view.addView(item)
        }
    }
}