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
import kotlinx.android.synthetic.main.fragment_news_language_setting.*
import org.mozilla.focus.R
import org.mozilla.focus.widget.BackKeyHandleable
import org.mozilla.rocket.content.appComponent
import org.mozilla.rocket.content.common.ui.GridLayoutItemDecoration
import org.mozilla.rocket.content.getActivityViewModel
import org.mozilla.rocket.content.news.data.NewsLanguage
import java.util.Locale
import javax.inject.Inject

class NewsLanguageSettingFragment : Fragment(), BackKeyHandleable {

    @Inject
    lateinit var newsPageStateViewModelCreator: Lazy<NewsPageStateViewModel>

    @Inject
    lateinit var newsLanguageSettingViewModelCreator: Lazy<NewsLanguageSettingViewModel>

    private lateinit var newsPageStateViewModel: NewsPageStateViewModel
    private lateinit var newsLanguageSettingViewModel: NewsLanguageSettingViewModel

    val languages = mutableListOf<NewsLanguage>()

    override fun onCreate(savedInstanceState: Bundle?) {
        appComponent().inject(this)
        super.onCreate(savedInstanceState)
        newsPageStateViewModel = getActivityViewModel(newsPageStateViewModelCreator)
        newsLanguageSettingViewModel = getActivityViewModel(newsLanguageSettingViewModelCreator)
        newsLanguageSettingViewModel.requestLanguageList()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_news_language_setting, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initRecyclerView()
        bindListData()
        bindPageState()
        initNoResultView()
    }

    override fun onBackPressed(): Boolean {
        newsPageStateViewModel.onLanguageSelected("")
        return true
    }

    private fun initRecyclerView() {
        news_language_list.adapter = NewsLanguageAdapter()
        news_language_list.layoutManager = GridLayoutManager(context, SPAN_COUNT)
        news_language_list.addItemDecoration(
            GridLayoutItemDecoration(
                news_language_list.resources.getDimensionPixelSize(R.dimen.news_language_setting_card_horizontal_space),
                news_language_list.resources.getDimensionPixelSize(R.dimen.news_language_setting_card_vertical_space),
                SPAN_COUNT
            )
        )
    }

    private fun bindListData() {
        newsLanguageSettingViewModel.uiModel.observe(viewLifecycleOwner, Observer { newsSettingsUiModel ->
            languages.addAll(newsSettingsUiModel.allLanguages)
            news_language_list.adapter?.notifyDataSetChanged()
        })
    }

    private fun bindPageState() {
        newsLanguageSettingViewModel.isDataLoading.observe(viewLifecycleOwner, Observer { state ->
            when (state) {
                is NewsLanguageSettingViewModel.State.Idle -> showContentView()
                is NewsLanguageSettingViewModel.State.Loading -> showLoadingView()
                is NewsLanguageSettingViewModel.State.Error -> showErrorView()
            }
        })
    }

    private fun initNoResultView() {
        no_result_view.setButtonOnClickListener(View.OnClickListener {
            newsLanguageSettingViewModel.onRetryButtonClicked()
        })
    }

    private fun showLoadingView() {
        spinner.visibility = View.VISIBLE
        news_language_list.visibility = View.GONE
        no_result_view.visibility = View.GONE
    }

    private fun showContentView() {
        spinner.visibility = View.GONE
        news_language_list.visibility = View.VISIBLE
        no_result_view.visibility = View.GONE
    }

    private fun showErrorView() {
        spinner.visibility = View.GONE
        news_language_list.visibility = View.GONE
        no_result_view.visibility = View.VISIBLE
    }

    inner class NewsLanguageAdapter : RecyclerView.Adapter<NewsLanguageViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, p1: Int): NewsLanguageViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_news_language, parent, false)
            return NewsLanguageViewHolder(v)
        }

        override fun onBindViewHolder(vh: NewsLanguageViewHolder, pos: Int) {
            vh.button.text = languages[pos].name
            vh.button.setOnClickListener {
                newsLanguageSettingViewModel.onLanguageSelected(languages[pos])
                newsPageStateViewModel.onLanguageSelected(languages[pos].name.toLowerCase(Locale.getDefault()))
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

        fun newInstance(): NewsLanguageSettingFragment {
            return NewsLanguageSettingFragment()
        }
    }
}
