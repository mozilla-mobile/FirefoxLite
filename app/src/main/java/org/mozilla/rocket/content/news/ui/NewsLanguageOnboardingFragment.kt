package org.mozilla.rocket.content.news.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import org.mozilla.focus.R

class NewsLanguageOnboardingFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news_language_onboarding, container, false)
    }

    companion object {
        fun newInstance(): NewsLanguageOnboardingFragment {
            return NewsLanguageOnboardingFragment()
        }
    }
}
