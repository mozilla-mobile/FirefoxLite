package org.mozilla.rocket.content.news.ui

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_news_personalization_onboarding.*
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class NewsPersonalizationOnboardingFragment : Fragment() {

    @Inject
    lateinit var newsPersonalizationOnboardingViewModelCreator: Lazy<NewsPersonalizationOnboardingViewModel>

    @Inject
    lateinit var newsOnboardingViewModelCreator: Lazy<NewsOnboardingViewModel>

    private lateinit var newsPersonalizationOnboardingViewModel: NewsPersonalizationOnboardingViewModel
    private lateinit var newsOnboardingViewModel: NewsOnboardingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news_personalization_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (savedInstanceState == null) {
            newsPersonalizationOnboardingViewModel = getActivityViewModel(newsPersonalizationOnboardingViewModelCreator)
            newsOnboardingViewModel = getActivityViewModel(newsOnboardingViewModelCreator)
        }

        news_personalization_onboarding_desc.movementMethod = LinkMovementMethod.getInstance()
        news_personalization_onboarding_btn_no.setOnClickListener {
            onPersonalizationSelected(false)
        }
        news_personalization_onboarding_btn_yes.setOnClickListener {
            onPersonalizationSelected(true)
        }
    }

    private fun onPersonalizationSelected(bool: Boolean) {
        newsPersonalizationOnboardingViewModel.onPersonalizationSelected(bool)
        newsOnboardingViewModel.onPersonalizationSelected()
    }

    companion object {
        fun newInstance(): NewsPersonalizationOnboardingFragment {
            return NewsPersonalizationOnboardingFragment()
        }
    }
}
