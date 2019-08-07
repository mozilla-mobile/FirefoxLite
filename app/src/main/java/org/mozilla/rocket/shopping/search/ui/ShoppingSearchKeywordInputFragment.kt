package org.mozilla.rocket.shopping.search.ui

import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.android.synthetic.main.fragment_shopping_search_keyword_input.search_keyword_edit
import org.mozilla.focus.R

class ShoppingSearchKeywordInputFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shopping_search_keyword_input, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
}