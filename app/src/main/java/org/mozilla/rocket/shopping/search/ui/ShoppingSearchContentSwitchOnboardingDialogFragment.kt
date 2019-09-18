package org.mozilla.rocket.shopping.search.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_shopping_search_onbarding.*
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getViewModel
import javax.inject.Inject

class ShoppingSearchContentSwitchOnboardingDialogFragment : DialogFragment() {

    @Inject
    lateinit var viewModelCreator: Lazy<ShoppingSearchContentSwitchOnboardingViewModel>

    private lateinit var viewModel: ShoppingSearchContentSwitchOnboardingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        viewModel = getViewModel(viewModelCreator)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_shopping_search_onbarding, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        view?.setOnClickListener {
            viewModel.dismissEvent.call()
        }
        button.setOnClickListener {
            viewModel.dismissEvent.call()
        }
        observeAction()
    }

    private fun observeAction() {
        viewModel.dismissEvent.observe(this, Observer {
            dismiss()
        })
    }
}