package org.mozilla.rocket.content.news.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import dagger.Lazy
import kotlinx.android.synthetic.main.fragment_news_language_onboarding.*
import org.mozilla.focus.R
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.GridLayoutItemDecoration
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.news.data.NewsLanguage
import javax.inject.Inject

class NewsLanguageOnboardingFragment : Fragment() {

    @Inject
    lateinit var newsOnboardingViewModelCreator: Lazy<NewsOnboardingViewModel>

    @Inject
    lateinit var newsLanguageOnboardingViewModelCreator: Lazy<NewsLanguageOnboardingViewModel>

    private lateinit var newsOnboardingViewModel: NewsOnboardingViewModel
    private lateinit var newsLanguageOnboardingViewModel: NewsLanguageOnboardingViewModel

    val languages = mutableListOf<NewsLanguage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        newsOnboardingViewModel = getActivityViewModel(newsOnboardingViewModelCreator)
        newsLanguageOnboardingViewModel = getActivityViewModel(newsLanguageOnboardingViewModelCreator)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news_language_onboarding, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        newsLanguageOnboardingViewModel.uiModel.observe(this, Observer { newsSettingsUiModel ->
            languages.addAll(newsSettingsUiModel.allLanguages)
            news_language_list.adapter?.notifyDataSetChanged()
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        news_language_list.adapter = NewsLanguageAdapter()
        news_language_list.layoutManager = GridLayoutManager(context, SPAN_COUNT)
        news_language_list.addItemDecoration(GridLayoutItemDecoration(view.resources.getDimensionPixelSize(R.dimen.news_language_onbarding_card_horizontal_space), view.resources.getDimensionPixelSize(R.dimen.news_language_onbarding_card_vertical_space), SPAN_COUNT))
    }

    inner class NewsLanguageAdapter : RecyclerView.Adapter<NewsLanguageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): NewsLanguageViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_news_language, parent, false)
            return NewsLanguageViewHolder(v)
        }

        override fun onBindViewHolder(vh: NewsLanguageViewHolder, pos: Int) {
            vh.button.text = languages[pos].name
            vh.button.setOnClickListener {
                newsLanguageOnboardingViewModel.onLanguageSelected(languages[pos])
                newsOnboardingViewModel.onLanguageSelected()
            }
        }

        override fun getItemCount(): Int {
            return languages.size
        }
    }

    inner class NewsLanguageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var button: TextView = view.findViewById(R.id.news_language_item)
    }

    companion object {
        private const val SPAN_COUNT = 2

        fun newInstance(): NewsLanguageOnboardingFragment {
            return NewsLanguageOnboardingFragment()
        }
    }
}
