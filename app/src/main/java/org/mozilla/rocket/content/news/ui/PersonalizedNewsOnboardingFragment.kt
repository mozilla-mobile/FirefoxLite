package org.mozilla.rocket.content.news.ui

import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_personalized_news_onboarding.*
import org.mozilla.focus.R
import org.mozilla.focus.activity.InfoActivity
import org.mozilla.focus.utils.SupportUtils
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.getActivityViewModel
import javax.inject.Inject

class PersonalizedNewsOnboardingFragment : Fragment() {

    @Inject
    lateinit var personalizedNewsOnboardingViewModelCreator: Lazy<PersonalizedNewsOnboardingViewModel>

    @Inject
    lateinit var newsPageStateViewModelCreator: Lazy<NewsPageStateViewModel>

    private lateinit var newsPageStateViewModel: NewsPageStateViewModel
    private lateinit var personalizedNewsOnboardingViewModel: PersonalizedNewsOnboardingViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        newsPageStateViewModel = getActivityViewModel(newsPageStateViewModelCreator)
        personalizedNewsOnboardingViewModel = getActivityViewModel(personalizedNewsOnboardingViewModelCreator)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_personalized_news_onboarding, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        observeAction()
    }

    private fun initViews() {
        setupDescriptionText()
        personalized_news_onboarding_btn_no.setOnClickListener {
            selectPreference(false)
        }
        personalized_news_onboarding_btn_yes.setOnClickListener {
            selectPreference(true)
        }
    }

    private fun setupDescriptionText() {
        val learnMoreStr = getString(R.string.about_link_learn_more)
        val descriptionStr = getString(R.string.recommended_news_content, learnMoreStr)
        val learnMoreIndex = descriptionStr.indexOf(learnMoreStr)
        val descriptionSpannableString = SpannableString(descriptionStr).apply {
            setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    personalizedNewsOnboardingViewModel.onLearnMoreLinkClick()
                }
            }, learnMoreIndex, learnMoreIndex + learnMoreStr.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
        personalized_news_onboarding_description.apply {
            movementMethod = LinkMovementMethod.getInstance()
            text = descriptionSpannableString
        }
    }

    private fun selectPreference(bool: Boolean) {
        personalizedNewsOnboardingViewModel.onPersonalizationSelected(bool)
    }

    private fun observeAction() {
        personalizedNewsOnboardingViewModel.openLearnMorePage.observe(viewLifecycleOwner, Observer {
            openLearnMorePage()
        })
        personalizedNewsOnboardingViewModel.setEnablePersonalizedNews.observe(viewLifecycleOwner, Observer {
            enablePersonalizedNews(it)
        })
    }

    private fun openLearnMorePage() {
        val url = SupportUtils.getSumoURLForTopic(context, "enable-news-lite")
        val title: String = getString(R.string.recommended_news_preference_toggle)
        val intent = InfoActivity.getIntentFor(context, url, title)
        context?.startActivity(intent)
    }

    private fun enablePersonalizedNews(enable: Boolean) {
        newsPageStateViewModel.onPersonalizedOptionSelected(enable)
    }

    companion object {
        fun newInstance(): PersonalizedNewsOnboardingFragment {
            return PersonalizedNewsOnboardingFragment()
        }
    }
}
